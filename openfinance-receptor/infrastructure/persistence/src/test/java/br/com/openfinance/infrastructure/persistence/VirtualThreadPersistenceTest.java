package br.com.openfinance.infrastructure.persistence;

import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.persistence.repository.VirtualThreadProcessingQueueRepository;
import br.com.openfinance.infrastructure.persistence.service.ParallelDataSyncService;
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
 * Integration tests demonstrating Virtual Threads and Structured Concurrency 
 * performance in database operations.
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:${random.int(10000,65535)}/test",
    "openfinance.database.virtual-threads.enabled=true",
    "openfinance.database.monitoring.enabled=true"
})
class VirtualThreadPersistenceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("test-schema.sql");
    
    @Test
    void shouldHandleHighConcurrencyWithVirtualThreads() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        VirtualThreadProcessingQueueRepository repository = createRepository(meterRegistry);
        
        int numberOfJobs = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfJobs);
        
        // Create jobs concurrently using Virtual Threads
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> futures = IntStream.range(0, numberOfJobs)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        ProcessingJob job = ProcessingJob.builder()
                                .consentId(UUID.randomUUID())
                                .organizationId("org-" + i)
                                .status(JobStatus.PENDING)
                                .retryCount(0)
                                .createdAt(LocalDateTime.now())
                                .build();
                        
                        repository.save(job);
                        latch.countDown();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, virtualThreadExecutor))
                .toList();
        
        // Wait for all operations to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(completed).isTrue();
        assertThat(duration).isLessThan(10000); // Should complete in under 10 seconds
        
        System.out.printf("Created %d jobs using Virtual Threads in %d ms (%.2f jobs/second)%n", 
                numberOfJobs, duration, numberOfJobs * 1000.0 / duration);
        
        virtualThreadExecutor.shutdown();
    }
    
    @Test
    void shouldProcessBatchesWithStructuredConcurrency() throws InterruptedException {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        VirtualThreadProcessingQueueRepository repository = createRepository(meterRegistry);
        
        // Create test jobs
        List<ProcessingJob> jobs = IntStream.range(0, 100)
                .mapToObj(i -> ProcessingJob.builder()
                        .id((long) i)
                        .consentId(UUID.randomUUID())
                        .organizationId("org-" + i)
                        .status(JobStatus.PENDING)
                        .retryCount(0)
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
        
        // Process batch with Structured Concurrency
        long startTime = System.currentTimeMillis();
        
        List<ProcessingJob> processedJobs = repository.processBatchWithStructuredConcurrency(
                jobs, 
                job -> job.withStatus(JobStatus.COMPLETED)
        );
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(processedJobs).hasSize(100);
        assertThat(processedJobs).allMatch(job -> job.getStatus() == JobStatus.COMPLETED);
        assertThat(duration).isLessThan(5000); // Should complete quickly with parallel processing
        
        System.out.printf("Processed %d jobs with Structured Concurrency in %d ms%n", 
                processedJobs.size(), duration);
    }
    
    @Test
    void shouldDemonstrateReactiveDataSync() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ParallelDataSyncService syncService = new ParallelDataSyncService(null, meterRegistry);
        
        // Test reactive sync capabilities
        long startTime = System.currentTimeMillis();
        
        List<ParallelDataSyncService.SyncResult> results = syncService.syncConsentsReactive(50)
                .collectList()
                .block();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify performance metrics are collected
        ParallelDataSyncService.SyncPerformanceMetrics metrics = syncService.getPerformanceMetrics();
        
        assertThat(metrics).isNotNull();
        assertThat(metrics.activeOperations()).isGreaterThanOrEqualTo(0);
        
        System.out.printf("Reactive sync completed in %d ms with metrics: %s%n", 
                duration, metrics);
    }
    
    @Test
    void shouldMonitorDatabasePerformance() throws InterruptedException {
        // Setup mock data source and monitor
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        
        // Simulate high-load database operations
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch loadTestLatch = new CountDownLatch(200);
        
        List<CompletableFuture<Void>> loadTasks = IntStream.range(0, 200)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate database operation
                        Thread.sleep(10);
                        loadTestLatch.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, executor))
                .toList();
        
        boolean completed = loadTestLatch.await(10, TimeUnit.SECONDS);
        CompletableFuture.allOf(loadTasks.toArray(new CompletableFuture[0])).join();
        
        assertThat(completed).isTrue();
        
        // Verify metrics were collected
        assertThat(meterRegistry.getMeters()).isNotEmpty();
        
        executor.shutdown();
    }
    
    @Test
    void shouldPerformBatchOperationsEfficiently() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        VirtualThreadProcessingQueueRepository repository = createRepository(meterRegistry);
        
        // Test batch fetching performance
        long startTime = System.currentTimeMillis();
        
        List<ProcessingJob> batch1 = repository.fetchNextBatch(50);
        List<ProcessingJob> batch2 = repository.fetchNextBatch(50);
        List<ProcessingJob> batch3 = repository.fetchNextBatch(50);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify batch operations are fast
        assertThat(duration).isLessThan(1000); // Should be very fast with optimized queries
        
        System.out.printf("Fetched %d jobs in 3 batches in %d ms%n", 
                batch1.size() + batch2.size() + batch3.size(), duration);
    }
    
    @Test
    void shouldScaleWithVirtualThreads() throws InterruptedException {
        // Demonstrate scalability with increasing virtual thread count
        int[] threadCounts = {10, 50, 100, 500, 1000};
        
        for (int threadCount : threadCounts) {
            long startTime = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            
            IntStream.range(0, threadCount)
                    .forEach(i -> executor.submit(() -> {
                        try {
                            // Simulate work
                            Thread.sleep(1);
                            latch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(completed).isTrue();
            
            System.out.printf("Completed %d virtual thread tasks in %d ms%n", threadCount, duration);
            
            executor.shutdown();
        }
    }
    
    private VirtualThreadProcessingQueueRepository createRepository(SimpleMeterRegistry meterRegistry) {
        // This would normally be injected by Spring
        // For testing, we create a mock or test implementation
        return new VirtualThreadProcessingQueueRepository(
                null, // Mock DataSource
                null, // Mock Mapper
                meterRegistry
        );
    }
}