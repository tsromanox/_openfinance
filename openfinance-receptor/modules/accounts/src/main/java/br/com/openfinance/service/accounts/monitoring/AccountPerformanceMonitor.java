package br.com.openfinance.service.accounts.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Comprehensive performance monitoring for account processing operations.
 * Provides detailed metrics, performance analysis, and optimization recommendations.
 */
@Slf4j
@Component
public class AccountPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    // Core metrics
    private final Counter accountsProcessedCounter;
    private final Counter accountsSyncedCounter;
    private final Counter balancesUpdatedCounter;
    private final Counter accountValidationsCounter;
    private final Counter errorCounter;
    private final Counter apiCallCounter;
    
    private final Timer accountProcessingTimer;
    private final Timer accountSyncTimer;
    private final Timer balanceUpdateTimer;
    private final Timer accountValidationTimer;
    private final Timer apiCallTimer;
    private final Timer batchProcessingTimer;
    
    // Performance tracking
    private final AtomicLong totalAccountsProcessed = new AtomicLong(0);
    private final AtomicLong totalAccountsSynced = new AtomicLong(0);
    private final AtomicLong totalBalancesUpdated = new AtomicLong(0);
    private final AtomicLong totalAccountValidations = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    private final AtomicInteger activeVirtualThreads = new AtomicInteger(0);
    private final AtomicInteger concurrentAccountOperations = new AtomicInteger(0);
    
    // Timing metrics
    private final DoubleAdder totalProcessingTime = new DoubleAdder();
    private final DoubleAdder totalSyncTime = new DoubleAdder();
    private final DoubleAdder totalBalanceUpdateTime = new DoubleAdder();
    private final DoubleAdder totalValidationTime = new DoubleAdder();
    
    // Operation tracking
    private final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> operationTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // Performance windows
    private final AtomicLong lastMetricReset = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong operationsInCurrentWindow = new AtomicLong(0);
    private final DoubleAdder timeInCurrentWindow = new DoubleAdder();
    
    public AccountPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.accountsProcessedCounter = Counter.builder("openfinance.accounts.processed.total")
                .description("Total number of accounts processed")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.accountsSyncedCounter = Counter.builder("openfinance.accounts.synced.total")
                .description("Total number of accounts synchronized")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.balancesUpdatedCounter = Counter.builder("openfinance.accounts.balances.updated.total")
                .description("Total number of account balances updated")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.accountValidationsCounter = Counter.builder("openfinance.accounts.validations.total")
                .description("Total number of account validations performed")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("openfinance.accounts.errors.total")
                .description("Total number of account processing errors")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.apiCallCounter = Counter.builder("openfinance.accounts.api.calls.total")
                .description("Total number of OpenFinance API calls")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        // Initialize timers
        this.accountProcessingTimer = Timer.builder("openfinance.accounts.processing.duration")
                .description("Account processing operation duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.accountSyncTimer = Timer.builder("openfinance.accounts.sync.duration")
                .description("Account synchronization duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.balanceUpdateTimer = Timer.builder("openfinance.accounts.balance.update.duration")
                .description("Balance update operation duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.accountValidationTimer = Timer.builder("openfinance.accounts.validation.duration")
                .description("Account validation duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.apiCallTimer = Timer.builder("openfinance.accounts.api.call.duration")
                .description("OpenFinance API call duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        this.batchProcessingTimer = Timer.builder("openfinance.accounts.batch.processing.duration")
                .description("Batch processing duration")
                .tag("component", "account-service")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("openfinance.accounts.virtual.threads.active")
                .description("Number of active Virtual Threads for account processing")
                .tag("component", "account-service")
                .register(meterRegistry, this, monitor -> monitor.activeVirtualThreads.get());
        
        Gauge.builder("openfinance.accounts.operations.concurrent")
                .description("Number of concurrent account operations")
                .tag("component", "account-service")
                .register(meterRegistry, this, monitor -> monitor.concurrentAccountOperations.get());
        
        Gauge.builder("openfinance.accounts.throughput.current")
                .description("Current account processing throughput (operations/second)")
                .tag("component", "account-service")
                .register(meterRegistry, this, this::getCurrentThroughput);
        
        Gauge.builder("openfinance.accounts.efficiency.processing")
                .description("Account processing efficiency ratio")
                .tag("component", "account-service")
                .register(meterRegistry, this, this::getProcessingEfficiency);
    }
    
    // Recording methods
    
    public void recordAccountOperation(String operationType, boolean success, long durationMs) {
        operationCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        operationTimes.computeIfAbsent(operationType, k -> new DoubleAdder()).add(durationMs);
        
        operationsInCurrentWindow.incrementAndGet();
        timeInCurrentWindow.add(durationMs);
        
        switch (operationType) {
            case "SYNC", "SYNC_START", "SYNC_COMPLETE" -> {
                accountsSyncedCounter.increment();
                if (durationMs > 0) {
                    accountSyncTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalSyncTime.add(durationMs);
                }
                totalAccountsSynced.incrementAndGet();
            }
            case "BALANCE_UPDATE", "BALANCE_UPDATE_START", "BALANCE_UPDATE_COMPLETE" -> {
                balancesUpdatedCounter.increment();
                if (durationMs > 0) {
                    balanceUpdateTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalBalanceUpdateTime.add(durationMs);
                }
                totalBalancesUpdated.incrementAndGet();
            }
            case "VALIDATION", "VALIDATE" -> {
                accountValidationsCounter.increment();
                if (durationMs > 0) {
                    accountValidationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalValidationTime.add(durationMs);
                }
                totalAccountValidations.incrementAndGet();
            }
            case "PROCESS", "PROCESSING" -> {
                accountsProcessedCounter.increment();
                if (durationMs > 0) {
                    accountProcessingTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalProcessingTime.add(durationMs);
                }
                totalAccountsProcessed.incrementAndGet();
            }
        }
        
        if (!success) {
            recordError("operation_failure", operationType, true);
        }
        
        log.debug("Recorded account operation: type={}, success={}, duration={}ms", 
                operationType, success, durationMs);
    }
    
    public void recordBatchProcessing(int accountCount, double durationMs) {
        totalBatchesProcessed.incrementAndGet();
        batchProcessingTimer.record((long) durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.info("Recorded batch processing: {} accounts in {:.2f}ms ({:.2f} accounts/second)", 
                accountCount, durationMs, accountCount * 1000.0 / durationMs);
    }
    
    public void recordVirtualThreadUsage(int threadCount) {
        activeVirtualThreads.set(threadCount);
        
        log.debug("Recorded Virtual Thread usage: {} active threads", threadCount);
    }
    
    public void recordError(String errorType, String operation, boolean retryable) {
        errorCounter.increment();
        totalErrors.incrementAndGet();
        
        String errorKey = errorType + "_" + operation;
        errorCounts.computeIfAbsent(errorKey, k -> new AtomicLong(0)).incrementAndGet();
        
        log.warn("Recorded error: type={}, operation={}, retryable={}", errorType, operation, retryable);
    }
    
    public void recordApiCallStart() {
        concurrentAccountOperations.incrementAndGet();
        apiCallCounter.increment();
        totalApiCalls.incrementAndGet();
    }
    
    public void recordApiCallEnd() {
        concurrentAccountOperations.decrementAndGet();
    }
    
    public void recordApiCall(long durationMs) {
        apiCallTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    // Metrics retrieval
    
    public AccountPerformanceReport getPerformanceReport() {
        return new AccountPerformanceReport(
                totalAccountsProcessed.get(),
                totalAccountsSynced.get(),
                totalBalancesUpdated.get(),
                totalAccountValidations.get(),
                totalBatchesProcessed.get(),
                totalErrors.get(),
                totalApiCalls.get(),
                getCurrentThroughput(),
                getProcessingEfficiency(),
                getAverageProcessingTime(),
                getAverageSyncTime(),
                getAverageBalanceUpdateTime(),
                getAverageValidationTime(),
                activeVirtualThreads.get(),
                concurrentAccountOperations.get(),
                getErrorRate()
        );
    }
    
    public AccountPerformanceRecommendations getRecommendations() {
        double efficiency = getProcessingEfficiency();
        double throughput = getCurrentThroughput();
        double errorRate = getErrorRate();
        
        int recommendedBatchSize = calculateRecommendedBatchSize(efficiency, throughput);
        int recommendedConcurrency = calculateRecommendedConcurrency(efficiency, throughput);
        double processingEfficiency = efficiency;
        double balanceUpdateSuccessRate = calculateBalanceUpdateSuccessRate();
        
        return new AccountPerformanceRecommendations(
                recommendedBatchSize,
                recommendedConcurrency,
                processingEfficiency,
                balanceUpdateSuccessRate,
                throughput,
                generateOptimizationSuggestions(efficiency, errorRate, throughput)
        );
    }
    
    // Helper methods
    
    private double getCurrentThroughput() {
        long windowDuration = System.currentTimeMillis() - lastMetricReset.get();
        if (windowDuration == 0) return 0.0;
        
        return operationsInCurrentWindow.get() * 1000.0 / windowDuration;
    }
    
    private double getProcessingEfficiency() {
        long totalOperations = totalAccountsProcessed.get() + totalAccountsSynced.get() + 
                              totalBalancesUpdated.get() + totalAccountValidations.get();
        if (totalOperations == 0) return 1.0;
        
        long successfulOperations = totalOperations - totalErrors.get();
        return (double) successfulOperations / totalOperations;
    }
    
    private double getAverageProcessingTime() {
        long operations = totalAccountsProcessed.get();
        return operations > 0 ? totalProcessingTime.sum() / operations : 0.0;
    }
    
    private double getAverageSyncTime() {
        long operations = totalAccountsSynced.get();
        return operations > 0 ? totalSyncTime.sum() / operations : 0.0;
    }
    
    private double getAverageBalanceUpdateTime() {
        long operations = totalBalancesUpdated.get();
        return operations > 0 ? totalBalanceUpdateTime.sum() / operations : 0.0;
    }
    
    private double getAverageValidationTime() {
        long operations = totalAccountValidations.get();
        return operations > 0 ? totalValidationTime.sum() / operations : 0.0;
    }
    
    private double getErrorRate() {
        long totalOperations = totalAccountsProcessed.get() + totalAccountsSynced.get() + 
                              totalBalancesUpdated.get() + totalAccountValidations.get();
        if (totalOperations == 0) return 0.0;
        
        return (double) totalErrors.get() / totalOperations;
    }
    
    private int calculateRecommendedBatchSize(double efficiency, double throughput) {
        if (efficiency > 0.9 && throughput > 100) return 500;
        if (efficiency > 0.8 && throughput > 50) return 300;
        if (efficiency > 0.7) return 200;
        return 100;
    }
    
    private int calculateRecommendedConcurrency(double efficiency, double throughput) {
        if (efficiency > 0.9 && throughput > 100) return 200;
        if (efficiency > 0.8 && throughput > 50) return 100;
        if (efficiency > 0.7) return 50;
        return 20;
    }
    
    private double calculateBalanceUpdateSuccessRate() {
        long totalBalanceUpdates = totalBalancesUpdated.get();
        if (totalBalanceUpdates == 0) return 1.0;
        
        long balanceUpdateErrors = errorCounts.entrySet().stream()
                .filter(entry -> entry.getKey().contains("balance"))
                .mapToLong(entry -> entry.getValue().get())
                .sum();
        
        return (double) (totalBalanceUpdates - balanceUpdateErrors) / totalBalanceUpdates;
    }
    
    private String generateOptimizationSuggestions(double efficiency, double errorRate, double throughput) {
        StringBuilder suggestions = new StringBuilder();
        
        if (efficiency < 0.8) {
            suggestions.append("Consider increasing batch size and reducing concurrent operations. ");
        }
        if (errorRate > 0.1) {
            suggestions.append("High error rate detected - review error handling and retry policies. ");
        }
        if (throughput < 10) {
            suggestions.append("Low throughput - consider optimizing API calls and reducing synchronization overhead. ");
        }
        if (activeVirtualThreads.get() > 2000) {
            suggestions.append("Very high Virtual Thread usage - consider implementing backpressure. ");
        }
        
        return suggestions.length() > 0 ? suggestions.toString() : "Performance is optimal.";
    }
    
    public void resetMetrics() {
        lastMetricReset.set(System.currentTimeMillis());
        operationsInCurrentWindow.set(0);
        timeInCurrentWindow.reset();
    }
    
    // Record classes
    
    public record AccountPerformanceReport(
            long totalAccountsProcessed,
            long totalAccountsSynced,
            long totalBalancesUpdated,
            long totalAccountValidations,
            long totalBatchesProcessed,
            long totalErrors,
            long totalApiCalls,
            double currentThroughput,
            double processingEfficiency,
            double averageProcessingTime,
            double averageSyncTime,
            double averageBalanceUpdateTime,
            double averageValidationTime,
            int activeVirtualThreads,
            int concurrentAccountOperations,
            double errorRate
    ) {}
    
    public record AccountPerformanceRecommendations(
            int recommendedBatchSize,
            int recommendedConcurrency,
            double processingEfficiency,
            double balanceUpdateSuccessRate,
            double throughput,
            String optimizationSuggestions
    ) {}
}