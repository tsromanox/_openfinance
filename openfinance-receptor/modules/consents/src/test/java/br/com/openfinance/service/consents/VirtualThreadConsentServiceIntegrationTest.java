package br.com.openfinance.service.consents;

import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import br.com.openfinance.service.consents.resource.AdaptiveConsentResourceManager;
import br.com.openfinance.service.consents.validation.ParallelConsentValidator;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for Virtual Thread consent service demonstrating
 * high-performance concurrent consent processing with Java 21 features.
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "openfinance.consents.enabled=true",
    "openfinance.consents.virtual-threads.enabled=true",
    "openfinance.consents.adaptive.enabled=true",
    "openfinance.consents.monitoring.enabled=true",
    "openfinance.consents.validation.parallel.enabled=true",
    "spring.profiles.active=test"
})
class VirtualThreadConsentServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance_consents_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldCreateLargeNumberOfConsentsWithVirtualThreads() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        AdaptiveConsentResourceManager resourceManager = createResourceManager(performanceMonitor, meterRegistry);
        VirtualThreadConsentService consentService = createConsentService(
                resourceManager, performanceMonitor, meterRegistry);
        
        int numberOfConsents = 1000;
        CountDownLatch completionLatch = new CountDownLatch(numberOfConsents);
        
        // Create consent commands
        List<ConsentUseCase.CreateConsentCommand> commands = IntStream.range(0, numberOfConsents)
                .mapToObj(i -> new ConsentUseCase.CreateConsentCommand(
                        "org-" + i,
                        "customer-" + i,
                        Set.of("ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ"),
                        LocalDateTime.now().plusMonths(6)
                ))
                .toList();
        
        // Create consents concurrently using Virtual Threads
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<List<br.com.openfinance.domain.consent.Consent>> consentsFuture = 
                consentService.createConsentsAsync(commands);
        
        List<br.com.openfinance.domain.consent.Consent> createdConsents = consentsFuture.get(60, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(createdConsents).hasSize(numberOfConsents);
        assertThat(duration).isLessThan(30000); // Should complete in under 30 seconds
        
        // Verify all consents have correct status
        assertThat(createdConsents).allMatch(consent -> 
                consent.getStatus() == ConsentStatus.AWAITING_AUTHORISATION);
        
        // Verify performance metrics
        var performanceReport = performanceMonitor.getPerformanceReport();
        assertThat(performanceReport.totalConsentsCreated()).isGreaterThan(0);
        assertThat(performanceReport.processingEfficiency()).isGreaterThan(0.8);
        
        System.out.printf("Created %d consents using Virtual Threads in %d ms (%.2f consents/second)%n", 
                numberOfConsents, duration, numberOfConsents * 1000.0 / duration);
        
        virtualThreadExecutor.shutdown();
    }
    
    @Test
    void shouldProcessConsentsWithStructuredConcurrency() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        VirtualThreadConsentService consentService = createConsentService(
                null, performanceMonitor, meterRegistry);
        
        // Create test consent IDs
        List<UUID> consentIds = IntStream.range(0, 100)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Process consents using Structured Concurrency
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<VirtualThreadConsentService.BatchProcessingResult> processingFuture = 
                consentService.processConsentsWithStructuredConcurrency(consentIds);
        
        VirtualThreadConsentService.BatchProcessingResult result = 
                processingFuture.get(30, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(result.processed()).isEqualTo(consentIds.size());
        assertThat(result.strategy()).isEqualTo("STRUCTURED_CONCURRENCY");
        assertThat(duration).isLessThan(15000); // Should complete in under 15 seconds
        
        System.out.printf("Processed %d consents with Structured Concurrency in %d ms%n", 
                result.processed(), duration);
    }
    
    @Test
    void shouldValidateConsentsInParallel() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        ParallelConsentValidator validator = createValidator(performanceMonitor, meterRegistry);
        
        // Create test consent IDs
        List<UUID> consentIds = IntStream.range(0, 50)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Validate consents in parallel
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<List<VirtualThreadConsentService.ConsentBusinessValidationResult>> validationFuture = 
                validator.validateMultipleConsentsAsync(consentIds);
        
        List<VirtualThreadConsentService.ConsentBusinessValidationResult> validationResults = 
                validationFuture.get(20, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(validationResults).hasSize(consentIds.size());
        assertThat(duration).isLessThan(10000); // Should complete in under 10 seconds
        
        // Verify validation results structure
        assertThat(validationResults).allMatch(result -> result.consentId() != null);
        
        System.out.printf("Validated %d consents in parallel in %d ms%n", 
                validationResults.size(), duration);
    }
    
    @Test
    void shouldDemonstrateAdaptiveResourceManagement() throws InterruptedException {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        AdaptiveConsentResourceManager resourceManager = createResourceManager(performanceMonitor, meterRegistry);
        
        // Simulate varying load
        ExecutorService loadSimulator = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch loadTestLatch = new CountDownLatch(200);
        
        // Initial resource state
        int initialBatchSize = resourceManager.getDynamicBatchSize();
        int initialProcessingConcurrency = resourceManager.getDynamicConcurrencyLevel();
        int initialValidationConcurrency = resourceManager.getDynamicValidationConcurrency();
        
        // Create high load scenario with different resource types
        List<CompletableFuture<Void>> loadTasks = IntStream.range(0, 200)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate different types of resource usage
                        boolean processingAcquired = false;
                        boolean validationAcquired = false;
                        boolean apiAcquired = false;
                        
                        if (i % 3 == 0) {
                            processingAcquired = resourceManager.acquireConsentProcessingResources();
                        } else if (i % 3 == 1) {
                            validationAcquired = resourceManager.acquireValidationResources();
                        } else {
                            apiAcquired = resourceManager.acquireApiCallResources();
                        }
                        
                        Thread.sleep(10); // Simulate work
                        
                        // Release resources
                        if (processingAcquired) {
                            resourceManager.releaseConsentProcessingResources();
                        }
                        if (validationAcquired) {
                            resourceManager.releaseValidationResources();
                        }
                        if (apiAcquired) {
                            resourceManager.releaseApiCallResources();
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
        assertThat(resourceUtilization.activeProcessingTasks()).isGreaterThanOrEqualTo(0);
        assertThat(resourceUtilization.activeValidationTasks()).isGreaterThanOrEqualTo(0);
        assertThat(resourceUtilization.activeApiCalls()).isGreaterThanOrEqualTo(0);
        
        System.out.printf("Adaptive Resource Management Test - Initial: batch=%d, processing=%d, validation=%d; " +
                         "Final: batch=%d, processing=%d, validation=%d\n",
                initialBatchSize, initialProcessingConcurrency, initialValidationConcurrency,
                resourceManager.getDynamicBatchSize(), 
                resourceManager.getDynamicConcurrencyLevel(),
                resourceManager.getDynamicValidationConcurrency());
        
        loadSimulator.shutdown();
    }
    
    @Test
    void shouldProvideComprehensivePerformanceMetrics() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        
        // Simulate various consent operations
        performanceMonitor.recordConsentOperation("CREATE", true, 150);
        performanceMonitor.recordConsentOperation("PROCESS", true, 250);
        performanceMonitor.recordConsentOperation("VALIDATE", true, 100);
        performanceMonitor.recordConsentOperation("REVOKE", true, 80);
        performanceMonitor.recordConsentOperation("VALIDATE", false, 2000); // Slow/failed operation
        
        performanceMonitor.recordBatchProcessing(200, 8000.0);
        performanceMonitor.recordBatchProcessing(150, 6000.0);
        
        performanceMonitor.recordVirtualThreadUsage(500);
        performanceMonitor.recordApiCallStart();
        performanceMonitor.recordApiCallStart();
        performanceMonitor.recordApiCallEnd();
        
        performanceMonitor.recordConsentStatusChange("AWAITING_AUTHORISATION", "AUTHORISED");
        performanceMonitor.recordConsentStatusChange("AUTHORISED", "CONSUMED");
        
        performanceMonitor.recordError("validation_error", "VALIDATE", true);
        performanceMonitor.recordError("timeout", "API_CALL", true);
        
        // Get performance report
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify metrics collection
        assertThat(performanceReport).isNotNull();
        assertThat(performanceReport.totalConsentsCreated()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalConsentsProcessed()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalConsentsValidated()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalConsentsRevoked()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalBatchesProcessed()).isGreaterThan(0);
        assertThat(performanceReport.currentThroughput()).isGreaterThanOrEqualTo(0.0);
        assertThat(performanceReport.activeVirtualThreads()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.concurrentApiCalls()).isGreaterThanOrEqualTo(0);
        
        // Get recommendations
        var recommendations = performanceMonitor.getRecommendations();
        assertThat(recommendations.recommendedBatchSize()).isGreaterThan(0);
        assertThat(recommendations.recommendedConcurrency()).isGreaterThan(0);
        assertThat(recommendations.processingEfficiency()).isGreaterThanOrEqualTo(0.0);
        assertThat(recommendations.validationSuccessRate()).isGreaterThanOrEqualTo(0.0);
        
        System.out.printf("Performance Report: Created=%d, Processed=%d, Validated=%d, Revoked=%d, " +
                         "Batches=%d, Throughput=%.2f, Efficiency=%.2f%%, Validation Success=%.2f%%\n",
                performanceReport.totalConsentsCreated(),
                performanceReport.totalConsentsProcessed(),
                performanceReport.totalConsentsValidated(),
                performanceReport.totalConsentsRevoked(),
                performanceReport.totalBatchesProcessed(),
                performanceReport.currentThroughput(),
                recommendations.processingEfficiency() * 100,
                recommendations.validationSuccessRate() * 100);
    }
    
    @Test
    void shouldDemonstrateVirtualThreadScalabilityForConsents() throws InterruptedException {
        // Test Virtual Thread scalability with consent-specific operations
        int[] threadCounts = {50, 200, 500, 1000, 2000};
        
        for (int threadCount : threadCounts) {
            long startTime = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            
            IntStream.range(0, threadCount)
                    .forEach(i -> executor.submit(() -> {
                        try {
                            // Simulate consent processing work (I/O-bound operations)
                            simulateConsentOperation();
                            latch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(completed).isTrue();
            
            System.out.printf("Virtual Thread Scalability (Consents): %d threads completed in %d ms " +
                             "(%.2f operations/second)\n",
                    threadCount, duration, threadCount * 1000.0 / duration);
            
            executor.shutdown();
        }
    }
    
    @Test
    void shouldHandleConsentErrorsGracefullyWithCircuitBreaker() throws Exception {
        // Setup with error simulation
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        
        // Simulate consent operations with some failures
        int successfulCreations = 150;
        int failedCreations = 30;
        int successfulValidations = 120;
        int failedValidations = 20;
        int successfulProcessing = 100;
        int failedProcessing = 15;
        
        // Record successful operations
        for (int i = 0; i < successfulCreations; i++) {
            performanceMonitor.recordConsentOperation("CREATE", true, 100 + (i % 50));
        }
        for (int i = 0; i < successfulValidations; i++) {
            performanceMonitor.recordConsentOperation("VALIDATE", true, 80 + (i % 40));
        }
        for (int i = 0; i < successfulProcessing; i++) {
            performanceMonitor.recordConsentOperation("PROCESS", true, 200 + (i % 100));
        }
        
        // Record failed operations
        for (int i = 0; i < failedCreations; i++) {
            performanceMonitor.recordConsentOperation("CREATE", false, 3000);
            performanceMonitor.recordError("creation_error", "CREATE", true);
        }
        for (int i = 0; i < failedValidations; i++) {
            performanceMonitor.recordConsentOperation("VALIDATE", false, 5000);
            performanceMonitor.recordError("validation_error", "VALIDATE", true);
        }
        for (int i = 0; i < failedProcessing; i++) {
            performanceMonitor.recordConsentOperation("PROCESS", false, 10000);
            performanceMonitor.recordError("processing_error", "PROCESS", true);
        }
        
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify error handling metrics
        int totalOperations = successfulCreations + failedCreations + successfulValidations + 
                             failedValidations + successfulProcessing + failedProcessing;
        int successfulOperations = successfulCreations + successfulValidations + successfulProcessing;
        double expectedEfficiency = (double) successfulOperations / totalOperations;
        
        assertThat(performanceReport.processingEfficiency()).isCloseTo(expectedEfficiency, 
                org.assertj.core.data.Percentage.withPercentage(5));
        
        System.out.printf("Error Handling Test: Efficiency=%.2f%% (expected: %.2f%%), " +
                         "Error Rate=%.2f%%, Total Operations=%d\n",
                performanceReport.processingEfficiency() * 100,
                expectedEfficiency * 100,
                performanceReport.errorRate() * 100,
                totalOperations);
    }
    
    @Test
    void shouldDemonstrateReactiveConsentProcessing() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ConsentPerformanceMonitor performanceMonitor = new ConsentPerformanceMonitor(meterRegistry);
        VirtualThreadConsentService consentService = createConsentService(
                null, performanceMonitor, meterRegistry);
        
        // Create test consent IDs
        List<UUID> consentIds = IntStream.range(0, 20)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Process consents reactively
        long startTime = System.currentTimeMillis();
        
        List<VirtualThreadConsentService.ConsentProcessingResult> results = 
                consentService.processConsentsReactively(consentIds)
                        .collectList()
                        .block();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify reactive processing
        assertThat(results).hasSize(consentIds.size());
        assertThat(duration).isLessThan(5000); // Should be fast with reactive processing
        
        System.out.printf("Reactive consent processing completed: %d consents in %d ms\n", 
                results.size(), duration);
    }
    
    // Helper methods
    private void simulateConsentOperation() throws InterruptedException {
        // Simulate I/O-bound consent processing (database access, API calls, etc.)
        Thread.sleep(5);
    }
    
    private VirtualThreadConsentService createConsentService(
            AdaptiveConsentResourceManager resourceManager,
            ConsentPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        // Create mock or test implementations
        return new VirtualThreadConsentService(
                null, // Mock ConsentRepository
                null, // Mock ProcessingQueueRepository
                null, // Mock OpenFinanceClient
                null, // Mock VirtualThreadConsentProcessor
                null, // Mock ParallelConsentValidator
                resourceManager,
                performanceMonitor,
                null  // Mock TaskExecutor
        );
    }
    
    private AdaptiveConsentResourceManager createResourceManager(
            ConsentPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        return new AdaptiveConsentResourceManager(performanceMonitor, meterRegistry);
    }
    
    private ParallelConsentValidator createValidator(
            ConsentPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        return new ParallelConsentValidator(
                null, // Mock ConsentRepository
                null, // Mock TaskExecutor
                null, // Mock AdaptiveConsentResourceManager
                performanceMonitor,
                meterRegistry
        );
    }
}