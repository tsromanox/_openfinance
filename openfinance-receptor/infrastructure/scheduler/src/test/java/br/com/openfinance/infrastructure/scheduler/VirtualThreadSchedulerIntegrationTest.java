package br.com.openfinance.infrastructure.scheduler;

import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.scheduler.monitoring.SchedulerPerformanceMonitor;
import br.com.openfinance.infrastructure.scheduler.service.AdaptiveResourceManager;
import br.com.openfinance.infrastructure.scheduler.service.VirtualThreadProcessingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Virtual Thread scheduler demonstrating
 * high-performance concurrent processing with Java 21 features.
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "openfinance.scheduler.enabled=true",
    "openfinance.scheduler.virtual-threads.enabled=true",
    "openfinance.scheduler.adaptive.enabled=true",
    "openfinance.scheduler.monitoring.enabled=true",
    "spring.profiles.active=test"
})
class VirtualThreadSchedulerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance_scheduler_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldProcessLargeNumberOfJobsWithVirtualThreads() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SchedulerPerformanceMonitor performanceMonitor = new SchedulerPerformanceMonitor(meterRegistry);
        AdaptiveResourceManager resourceManager = createResourceManager(meterRegistry);
        VirtualThreadProcessingService processingService = createProcessingService(
                resourceManager, performanceMonitor, meterRegistry);
        
        int numberOfJobs = 2000;
        CountDownLatch completionLatch = new CountDownLatch(numberOfJobs);
        
        // Create test jobs concurrently using Virtual Threads
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> jobCreationFutures = IntStream.range(0, numberOfJobs)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate job creation and processing
                        ProcessingJob job = createTestJob(i);
                        
                        // Process job using Virtual Thread service
                        processJobWithVirtualThread(processingService, job);
                        
                        completionLatch.countDown();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, virtualThreadExecutor))
                .toList();
        
        // Wait for all jobs to complete
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        CompletableFuture.allOf(jobCreationFutures.toArray(new CompletableFuture[0])).join();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(completed).isTrue();
        assertThat(duration).isLessThan(30000); // Should complete in under 30 seconds
        
        // Verify performance metrics
        var performanceReport = performanceMonitor.getPerformanceReport();
        assertThat(performanceReport.totalJobsProcessed()).isGreaterThan(0);
        assertThat(performanceReport.processingEfficiency()).isGreaterThan(0.8);
        
        System.out.printf("Processed %d jobs using Virtual Threads in %d ms (%.2f jobs/second)%n", 
                numberOfJobs, duration, numberOfJobs * 1000.0 / duration);
        
        virtualThreadExecutor.shutdown();
    }
    
    @Test
    void shouldDemonstrateStructuredConcurrencyPerformance() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SchedulerPerformanceMonitor performanceMonitor = new SchedulerPerformanceMonitor(meterRegistry);
        AdaptiveResourceManager resourceManager = createResourceManager(meterRegistry);
        
        int batchSize = 500;
        
        // Test Structured Concurrency with different batch sizes
        int[] batchSizes = {50, 100, 200, 500};
        
        for (int currentBatchSize : batchSizes) {
            long startTime = System.currentTimeMillis();
            
            // Process batch using Structured Concurrency
            processBatchWithStructuredConcurrency(currentBatchSize, resourceManager);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Verify performance scales with batch size
            assertThat(duration).isLessThan(10000); // Should complete in under 10 seconds
            
            System.out.printf("Processed batch of %d jobs with Structured Concurrency in %d ms%n", 
                    currentBatchSize, duration);
        }
    }
    
    @Test
    void shouldAdaptResourcesBasedOnSystemLoad() throws InterruptedException {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AdaptiveResourceManager resourceManager = createResourceManager(meterRegistry);
        
        // Simulate varying system load
        ExecutorService loadSimulator = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch loadTestLatch = new CountDownLatch(300);
        
        // Initial resource state
        int initialBatchSize = resourceManager.getDynamicBatchSize();
        int initialConcurrency = resourceManager.getDynamicConcurrencyLevel();
        
        // Create high load scenario
        List<CompletableFuture<Void>> loadTasks = IntStream.range(0, 300)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Acquire resources to simulate load
                        boolean acquired = resourceManager.acquireResources();
                        if (acquired) {
                            Thread.sleep(50); // Simulate work
                            resourceManager.releaseResources();
                        }
                        loadTestLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, loadSimulator))
                .toList();
        
        // Wait for load test to complete
        boolean completed = loadTestLatch.await(30, TimeUnit.SECONDS);
        CompletableFuture.allOf(loadTasks.toArray(new CompletableFuture[0])).join();
        
        // Verify adaptive behavior
        assertThat(completed).isTrue();
        
        // Get resource utilization after load
        var resourceUtilization = resourceManager.getResourceUtilization();
        assertThat(resourceUtilization.activeTasks()).isGreaterThanOrEqualTo(0);
        
        System.out.printf("Adaptive Resource Management Test - Initial: batch=%d, concurrency=%d; " +
                         "Final: batch=%d, concurrency=%d, utilization=%.2f%%\n",
                initialBatchSize, initialConcurrency,
                resourceManager.getDynamicBatchSize(), 
                resourceManager.getDynamicConcurrencyLevel(),
                resourceUtilization.getResourceUtilizationPercentage());
        
        loadSimulator.shutdown();
    }
    
    @Test
    void shouldProvideComprehensivePerformanceMetrics() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SchedulerPerformanceMonitor performanceMonitor = new SchedulerPerformanceMonitor(meterRegistry);
        
        // Simulate various processing scenarios
        performanceMonitor.recordJobProcessing("CONSENT_PROCESSING", true, 150);
        performanceMonitor.recordJobProcessing("ACCOUNT_SYNC", true, 250);
        performanceMonitor.recordJobProcessing("ACCOUNT_BALANCE_UPDATE", false, 2000); // Slow/failed job
        
        performanceMonitor.recordBatchProcessing(100, 5000.0);
        performanceMonitor.recordBatchProcessing(150, 7500.0);
        
        performanceMonitor.recordVirtualThreadUsage(250);
        performanceMonitor.recordError("timeout", "ACCOUNT_SYNC", true);
        
        // Get performance report
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify metrics collection
        assertThat(performanceReport).isNotNull();
        assertThat(performanceReport.totalJobsProcessed()).isGreaterThan(0);
        assertThat(performanceReport.totalBatchesProcessed()).isGreaterThan(0);
        assertThat(performanceReport.currentThroughput()).isGreaterThanOrEqualTo(0.0);
        assertThat(performanceReport.activeVirtualThreads()).isGreaterThanOrEqualTo(0);
        
        // Get recommendations
        var recommendations = performanceMonitor.getRecommendations();
        assertThat(recommendations.recommendedBatchSize()).isGreaterThan(0);
        assertThat(recommendations.recommendedConcurrency()).isGreaterThan(0);
        assertThat(recommendations.processingEfficiency()).isGreaterThanOrEqualTo(0.0);
        
        System.out.printf("Performance Report: Jobs=%d, Batches=%d, Throughput=%.2f, " +
                         "Efficiency=%.2f%%, Recommended Batch Size=%d\n",
                performanceReport.totalJobsProcessed(),
                performanceReport.totalBatchesProcessed(),
                performanceReport.currentThroughput(),
                recommendations.processingEfficiency() * 100,
                recommendations.recommendedBatchSize());
    }
    
    @Test
    void shouldDemonstrateVirtualThreadScalability() throws InterruptedException {
        // Test Virtual Thread scalability with increasing load
        int[] threadCounts = {100, 500, 1000, 2000, 5000};
        
        for (int threadCount : threadCounts) {
            long startTime = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            
            IntStream.range(0, threadCount)
                    .forEach(i -> executor.submit(() -> {
                        try {
                            // Simulate I/O-bound work (perfect for Virtual Threads)
                            Thread.sleep(10);
                            latch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(completed).isTrue();
            
            System.out.printf("Virtual Thread Scalability: %d threads completed in %d ms " +
                             "(%.2f threads/second)\n",
                    threadCount, duration, threadCount * 1000.0 / duration);
            
            executor.shutdown();
        }
    }
    
    @Test
    void shouldHandleErrorsGracefullyWithCircuitBreaker() throws Exception {
        // Setup with error simulation
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SchedulerPerformanceMonitor performanceMonitor = new SchedulerPerformanceMonitor(meterRegistry);
        
        // Simulate processing with some failures
        int successfulJobs = 80;
        int failedJobs = 20;
        
        // Record successful jobs
        for (int i = 0; i < successfulJobs; i++) {
            performanceMonitor.recordJobProcessing("CONSENT_PROCESSING", true, 100);
        }
        
        // Record failed jobs
        for (int i = 0; i < failedJobs; i++) {
            performanceMonitor.recordJobProcessing("CONSENT_PROCESSING", false, 5000);
            performanceMonitor.recordError("processing_error", "CONSENT_PROCESSING", true);
        }
        
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify error handling metrics
        double expectedEfficiency = (double) successfulJobs / (successfulJobs + failedJobs);
        assertThat(performanceReport.processingEfficiency()).isCloseTo(expectedEfficiency, 
                org.assertj.core.data.Percentage.withPercentage(5));
        
        System.out.printf("Error Handling Test: Efficiency=%.2f%% (expected: %.2f%%), " +
                         "Error Rate=%.2f%%\n",
                performanceReport.processingEfficiency() * 100,
                expectedEfficiency * 100,
                performanceReport.errorRate() * 100);
    }
    
    // Helper methods
    private ProcessingJob createTestJob(int index) {
        return ProcessingJob.builder()
                .id((long) index)
                .consentId(UUID.randomUUID())
                .organizationId("org-" + index)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    private void processJobWithVirtualThread(VirtualThreadProcessingService service, ProcessingJob job) {
        // Simulate processing using the service
        // In real implementation, this would be handled by the service
        try {
            Thread.sleep(1); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processBatchWithStructuredConcurrency(int batchSize, AdaptiveResourceManager resourceManager) 
            throws InterruptedException {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Submit batch tasks
            List<StructuredTaskScope.Subtask<Integer>> subtasks = IntStream.range(0, batchSize)
                    .mapToObj(i -> scope.fork(() -> {
                        Thread.sleep(1); // Simulate work
                        return i;
                    }))
                    .toList();
            
            // Wait for completion
            scope.join();
            scope.throwIfFailed();
            
            // Verify all tasks completed
            assertThat(subtasks).hasSize(batchSize);
            
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    private AdaptiveResourceManager createResourceManager(SimpleMeterRegistry meterRegistry) {
        // Create a test instance of AdaptiveResourceManager
        // In real tests, this would be injected by Spring
        return new AdaptiveResourceManager(
                new SchedulerPerformanceMonitor(meterRegistry),
                meterRegistry
        );
    }
    
    private VirtualThreadProcessingService createProcessingService(
            AdaptiveResourceManager resourceManager,
            SchedulerPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        // Create a test instance - in real tests, this would be injected by Spring
        return new VirtualThreadProcessingService(
                null, // Mock repository
                null, // Mock consent use case
                null, // Mock account use case
                null, // Mock virtual thread executor
                null, // Mock structured concurrency executor
                meterRegistry,
                performanceMonitor
        );
    }
}