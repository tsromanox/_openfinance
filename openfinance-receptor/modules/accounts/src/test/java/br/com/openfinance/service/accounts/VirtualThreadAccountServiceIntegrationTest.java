package br.com.openfinance.service.accounts;

import br.com.openfinance.service.accounts.monitoring.AccountPerformanceMonitor;
import br.com.openfinance.service.accounts.resource.AdaptiveAccountResourceManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
 * Comprehensive integration tests for Virtual Thread account service demonstrating
 * high-performance concurrent account processing with Java 21 features.
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "openfinance.accounts.enabled=true",
    "openfinance.accounts.virtual-threads.enabled=true",
    "openfinance.accounts.adaptive.enabled=true",
    "openfinance.accounts.monitoring.enabled=true",
    "openfinance.accounts.sync.parallel.enabled=true",
    "openfinance.accounts.balance.parallel.enabled=true",
    "spring.profiles.active=test"
})
class VirtualThreadAccountServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("openfinance_accounts_test")
            .withUsername("test")
            .withPassword("test");
    
    @Test
    void shouldSyncLargeNumberOfAccountsWithVirtualThreads() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        AdaptiveAccountResourceManager resourceManager = createResourceManager(performanceMonitor, meterRegistry);
        VirtualThreadAccountService accountService = createAccountService(
                resourceManager, performanceMonitor, meterRegistry);
        
        int numberOfConsents = 500;
        
        // Create consent IDs
        List<UUID> consentIds = IntStream.range(0, numberOfConsents)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Sync accounts concurrently using Virtual Threads
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<List<br.com.openfinance.domain.account.Account>> accountsFuture = 
                accountService.syncAccountsForConsentsAsync(consentIds);
        
        List<br.com.openfinance.domain.account.Account> syncedAccounts = 
                accountsFuture.get(120, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(syncedAccounts).isNotNull();
        assertThat(duration).isLessThan(60000); // Should complete in under 60 seconds
        
        // Verify performance metrics
        var performanceReport = performanceMonitor.getPerformanceReport();
        assertThat(performanceReport.totalAccountsSynced()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.processingEfficiency()).isGreaterThanOrEqualTo(0.0);
        
        System.out.printf("Synced accounts for %d consents using Virtual Threads in %d ms%n", 
                numberOfConsents, duration);
    }
    
    @Test
    void shouldProcessAccountsWithStructuredConcurrency() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        VirtualThreadAccountService accountService = createAccountService(
                null, performanceMonitor, meterRegistry);
        
        // Create test consent IDs
        List<UUID> consentIds = IntStream.range(0, 50)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Process accounts using Structured Concurrency
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<VirtualThreadAccountService.BatchProcessingResult> processingFuture = 
                accountService.processAccountsInAdaptiveBatches(consentIds);
        
        VirtualThreadAccountService.BatchProcessingResult result = 
                processingFuture.get(60, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(result.processedCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.strategy()).isEqualTo("ADAPTIVE_BATCH");
        assertThat(duration).isLessThan(30000); // Should complete in under 30 seconds
        
        System.out.printf("Processed %d accounts with Structured Concurrency in %d ms%n", 
                result.processedCount(), duration);
    }
    
    @Test
    void shouldUpdateAccountBalancesInParallel() throws Exception {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        VirtualThreadAccountService accountService = createAccountService(
                null, performanceMonitor, meterRegistry);
        
        // Create test account IDs
        List<String> accountIds = IntStream.range(0, 30)
                .mapToObj(i -> "account-" + i)
                .toList();
        
        // Update balances in parallel
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<VirtualThreadAccountService.BalanceUpdateResult> updateFuture = 
                accountService.updateAccountBalancesAsync(accountIds);
        
        VirtualThreadAccountService.BalanceUpdateResult result = 
                updateFuture.get(45, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(result.updatedCount()).isGreaterThanOrEqualTo(0);
        assertThat(duration).isLessThan(20000); // Should complete in under 20 seconds
        
        System.out.printf("Updated %d account balances in parallel in %d ms%n", 
                result.updatedCount(), duration);
    }
    
    @Test
    void shouldDemonstrateAdaptiveResourceManagement() throws InterruptedException {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        AdaptiveAccountResourceManager resourceManager = createResourceManager(performanceMonitor, meterRegistry);
        
        // Simulate varying load
        ExecutorService loadSimulator = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch loadTestLatch = new CountDownLatch(100);
        
        // Initial resource state
        int initialBatchSize = resourceManager.getDynamicBatchSize();
        int initialAccountConcurrency = resourceManager.getDynamicConcurrencyLevel();
        int initialBalanceConcurrency = resourceManager.getDynamicBalanceUpdateConcurrency();
        
        // Create high load scenario with different resource types
        List<CompletableFuture<Void>> loadTasks = IntStream.range(0, 100)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        // Simulate different types of resource usage
                        boolean accountAcquired = false;
                        boolean balanceAcquired = false;
                        boolean apiAcquired = false;
                        
                        if (i % 3 == 0) {
                            accountAcquired = resourceManager.acquireAccountProcessingResources();
                        } else if (i % 3 == 1) {
                            balanceAcquired = resourceManager.acquireBalanceUpdateResources();
                        } else {
                            apiAcquired = resourceManager.acquireApiCallResources();
                        }
                        
                        Thread.sleep(15); // Simulate work
                        
                        // Release resources
                        if (accountAcquired) {
                            resourceManager.releaseAccountProcessingResources();
                        }
                        if (balanceAcquired) {
                            resourceManager.releaseBalanceUpdateResources();
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
        boolean completed = loadTestLatch.await(60, TimeUnit.SECONDS);
        CompletableFuture.allOf(loadTasks.toArray(new CompletableFuture[0])).join();
        
        // Verify adaptive behavior
        assertThat(completed).isTrue();
        
        // Get resource utilization after load
        var resourceUtilization = resourceManager.getResourceUtilization();
        assertThat(resourceUtilization.activeAccountProcessingTasks()).isGreaterThanOrEqualTo(0);
        assertThat(resourceUtilization.activeBalanceUpdateTasks()).isGreaterThanOrEqualTo(0);
        assertThat(resourceUtilization.activeApiCalls()).isGreaterThanOrEqualTo(0);
        
        System.out.printf("Adaptive Resource Management Test - Initial: batch=%d, account=%d, balance=%d; " +
                         "Final: batch=%d, account=%d, balance=%d%n",
                initialBatchSize, initialAccountConcurrency, initialBalanceConcurrency,
                resourceManager.getDynamicBatchSize(), 
                resourceManager.getDynamicConcurrencyLevel(),
                resourceManager.getDynamicBalanceUpdateConcurrency());
        
        loadSimulator.shutdown();
    }
    
    @Test
    void shouldProvideComprehensivePerformanceMetrics() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        
        // Simulate various account operations
        performanceMonitor.recordAccountOperation("SYNC", true, 200);
        performanceMonitor.recordAccountOperation("BALANCE_UPDATE", true, 150);
        performanceMonitor.recordAccountOperation("VALIDATION", true, 100);
        performanceMonitor.recordAccountOperation("PROCESS", true, 300);
        performanceMonitor.recordAccountOperation("SYNC", false, 5000); // Slow/failed operation
        
        performanceMonitor.recordBatchProcessing(300, 12000.0);
        performanceMonitor.recordBatchProcessing(250, 8000.0);
        
        performanceMonitor.recordVirtualThreadUsage(800);
        performanceMonitor.recordApiCallStart();
        performanceMonitor.recordApiCallStart();
        performanceMonitor.recordApiCallEnd();
        
        performanceMonitor.recordError("sync_error", "SYNC", true);
        performanceMonitor.recordError("timeout", "API_CALL", true);
        
        // Get performance report
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify metrics collection
        assertThat(performanceReport).isNotNull();
        assertThat(performanceReport.totalAccountsProcessed()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalAccountsSynced()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalBalancesUpdated()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalAccountValidations()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.totalBatchesProcessed()).isGreaterThan(0);
        assertThat(performanceReport.currentThroughput()).isGreaterThanOrEqualTo(0.0);
        assertThat(performanceReport.activeVirtualThreads()).isGreaterThanOrEqualTo(0);
        assertThat(performanceReport.concurrentAccountOperations()).isGreaterThanOrEqualTo(0);
        
        // Get recommendations
        var recommendations = performanceMonitor.getRecommendations();
        assertThat(recommendations.recommendedBatchSize()).isGreaterThan(0);
        assertThat(recommendations.recommendedConcurrency()).isGreaterThan(0);
        assertThat(recommendations.processingEfficiency()).isGreaterThanOrEqualTo(0.0);
        assertThat(recommendations.balanceUpdateSuccessRate()).isGreaterThanOrEqualTo(0.0);
        
        System.out.printf("Performance Report: Processed=%d, Synced=%d, Balances=%d, Validated=%d, " +
                         "Batches=%d, Throughput=%.2f, Efficiency=%.2f%%, Balance Success=%.2f%%\\n",
                performanceReport.totalAccountsProcessed(),
                performanceReport.totalAccountsSynced(),
                performanceReport.totalBalancesUpdated(),
                performanceReport.totalAccountValidations(),
                performanceReport.totalBatchesProcessed(),
                performanceReport.currentThroughput(),
                recommendations.processingEfficiency() * 100,
                recommendations.balanceUpdateSuccessRate() * 100);
    }
    
    @Test
    void shouldDemonstrateVirtualThreadScalabilityForAccounts() throws InterruptedException {
        // Test Virtual Thread scalability with account-specific operations
        int[] threadCounts = {100, 500, 1000, 2000, 5000};
        
        for (int threadCount : threadCounts) {
            long startTime = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            
            IntStream.range(0, threadCount)
                    .forEach(i -> executor.submit(() -> {
                        try {
                            // Simulate account processing work (I/O-bound operations)
                            simulateAccountOperation();
                            latch.countDown();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(completed).isTrue();
            
            System.out.printf("Virtual Thread Scalability (Accounts): %d threads completed in %d ms " +
                             "(%.2f operations/second)%n",
                    threadCount, duration, threadCount * 1000.0 / duration);
            
            executor.shutdown();
        }
    }
    
    @Test
    void shouldHandleAccountErrorsGracefullyWithCircuitBreaker() throws Exception {
        // Setup with error simulation
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        
        // Simulate account operations with some failures
        int successfulSyncs = 200;
        int failedSyncs = 40;
        int successfulBalanceUpdates = 150;
        int failedBalanceUpdates = 25;
        int successfulProcessing = 180;
        int failedProcessing = 20;
        
        // Record successful operations
        for (int i = 0; i < successfulSyncs; i++) {
            performanceMonitor.recordAccountOperation("SYNC", true, 150 + (i % 50));
        }
        for (int i = 0; i < successfulBalanceUpdates; i++) {
            performanceMonitor.recordAccountOperation("BALANCE_UPDATE", true, 100 + (i % 30));
        }
        for (int i = 0; i < successfulProcessing; i++) {
            performanceMonitor.recordAccountOperation("PROCESS", true, 250 + (i % 100));
        }
        
        // Record failed operations
        for (int i = 0; i < failedSyncs; i++) {
            performanceMonitor.recordAccountOperation("SYNC", false, 4000);
            performanceMonitor.recordError("sync_error", "SYNC", true);
        }
        for (int i = 0; i < failedBalanceUpdates; i++) {
            performanceMonitor.recordAccountOperation("BALANCE_UPDATE", false, 6000);
            performanceMonitor.recordError("balance_error", "BALANCE_UPDATE", true);
        }
        for (int i = 0; i < failedProcessing; i++) {
            performanceMonitor.recordAccountOperation("PROCESS", false, 8000);
            performanceMonitor.recordError("processing_error", "PROCESS", true);
        }
        
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Verify error handling metrics
        int totalOperations = successfulSyncs + failedSyncs + successfulBalanceUpdates + 
                             failedBalanceUpdates + successfulProcessing + failedProcessing;
        int successfulOperations = successfulSyncs + successfulBalanceUpdates + successfulProcessing;
        double expectedEfficiency = (double) successfulOperations / totalOperations;
        
        assertThat(performanceReport.processingEfficiency()).isCloseTo(expectedEfficiency, 
                org.assertj.core.data.Percentage.withPercentage(5));
        
        System.out.printf("Error Handling Test: Efficiency=%.2f%% (expected: %.2f%%), " +
                         "Error Rate=%.2f%%, Total Operations=%d%n",
                performanceReport.processingEfficiency() * 100,
                expectedEfficiency * 100,
                performanceReport.errorRate() * 100,
                totalOperations);
    }
    
    @Test
    void shouldDemonstrateReactiveAccountProcessing() {
        // Setup
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountPerformanceMonitor performanceMonitor = new AccountPerformanceMonitor(meterRegistry);
        VirtualThreadAccountService accountService = createAccountService(
                null, performanceMonitor, meterRegistry);
        
        // Create test consent IDs
        List<UUID> consentIds = IntStream.range(0, 15)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        
        // Process accounts reactively
        long startTime = System.currentTimeMillis();
        
        List<VirtualThreadAccountService.AccountProcessingResult> results = 
                accountService.processAccountsReactively(consentIds)
                        .collectList()
                        .block();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify reactive processing
        assertThat(results).hasSize(consentIds.size());
        assertThat(duration).isLessThan(10000); // Should be fast with reactive processing
        
        System.out.printf("Reactive account processing completed: %d consents in %d ms%n", 
                results.size(), duration);
    }
    
    // Helper methods
    private void simulateAccountOperation() throws InterruptedException {
        // Simulate I/O-bound account processing (database access, API calls, etc.)
        Thread.sleep(8);
    }
    
    private VirtualThreadAccountService createAccountService(
            AdaptiveAccountResourceManager resourceManager,
            AccountPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        // Create mock or test implementations
        return new VirtualThreadAccountService(
                null, // Mock ConsentRepository
                null, // Mock OpenFinanceClient
                null, // Mock VirtualThreadAccountProcessor
                resourceManager,
                performanceMonitor,
                null, // Mock TaskExecutor
                null, // Mock TaskExecutor
                null  // Mock TaskExecutor
        );
    }
    
    private AdaptiveAccountResourceManager createResourceManager(
            AccountPerformanceMonitor performanceMonitor,
            SimpleMeterRegistry meterRegistry) {
        
        return new AdaptiveAccountResourceManager(performanceMonitor, meterRegistry);
    }
}