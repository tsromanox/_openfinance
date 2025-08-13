package br.com.openfinance.service.accounts.resource;

import br.com.openfinance.service.accounts.monitoring.AccountPerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive resource manager for account processing operations.
 * Dynamically adjusts resource allocation based on system performance and load.
 */
@Slf4j
@Component
public class AdaptiveAccountResourceManager {
    
    private final AccountPerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;
    private final OperatingSystemMXBean osBean;
    
    // Resource semaphores
    private final Semaphore accountProcessingSemaphore;
    private final Semaphore balanceUpdateSemaphore;
    private final Semaphore accountValidationSemaphore;
    private final Semaphore apiCallSemaphore;
    private final Semaphore batchProcessingSemaphore;
    
    // Dynamic resource parameters
    private final AtomicInteger dynamicBatchSize = new AtomicInteger(200);
    private final AtomicInteger dynamicConcurrencyLevel = new AtomicInteger(100);
    private final AtomicInteger dynamicBalanceUpdateConcurrency = new AtomicInteger(50);
    private final AtomicInteger dynamicValidationConcurrency = new AtomicInteger(30);
    private final AtomicInteger dynamicApiCallConcurrency = new AtomicInteger(200);
    
    // Resource tracking
    private final AtomicInteger activeAccountProcessingTasks = new AtomicInteger(0);
    private final AtomicInteger activeBalanceUpdateTasks = new AtomicInteger(0);
    private final AtomicInteger activeValidationTasks = new AtomicInteger(0);
    private final AtomicInteger activeApiCalls = new AtomicInteger(0);
    private final AtomicInteger activeBatchProcessingTasks = new AtomicInteger(0);
    
    // Adaptive thresholds
    private final AtomicLong lastAdaptationTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger adaptationInterval = new AtomicInteger(30000); // 30 seconds
    
    // System resource monitoring
    private volatile double currentCpuUsage = 0.0;
    private volatile double currentMemoryUsage = 0.0;
    private volatile int availableProcessors;
    
    // Configuration parameters
    private static final int MIN_BATCH_SIZE = 50;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final int MIN_CONCURRENCY = 10;
    private static final int MAX_CONCURRENCY = 500;
    private static final int MIN_BALANCE_CONCURRENCY = 5;
    private static final int MAX_BALANCE_CONCURRENCY = 200;
    private static final int MIN_VALIDATION_CONCURRENCY = 5;
    private static final int MAX_VALIDATION_CONCURRENCY = 100;
    private static final int MIN_API_CONCURRENCY = 20;
    private static final int MAX_API_CONCURRENCY = 1000;
    
    private static final double CPU_THRESHOLD_HIGH = 0.80;
    private static final double CPU_THRESHOLD_LOW = 0.40;
    private static final double MEMORY_THRESHOLD_HIGH = 0.85;
    private static final double MEMORY_THRESHOLD_LOW = 0.50;
    
    public AdaptiveAccountResourceManager(
            AccountPerformanceMonitor performanceMonitor,
            MeterRegistry meterRegistry) {
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // Initialize semaphores with initial capacity
        this.accountProcessingSemaphore = new Semaphore(dynamicConcurrencyLevel.get());
        this.balanceUpdateSemaphore = new Semaphore(dynamicBalanceUpdateConcurrency.get());
        this.accountValidationSemaphore = new Semaphore(dynamicValidationConcurrency.get());
        this.apiCallSemaphore = new Semaphore(dynamicApiCallConcurrency.get());
        this.batchProcessingSemaphore = new Semaphore(10); // Fixed batch processing limit
        
        log.info("Initialized AdaptiveAccountResourceManager with {} processors", availableProcessors);
    }
    
    // Resource acquisition methods
    
    public boolean acquireAccountProcessingResources() {
        boolean acquired = accountProcessingSemaphore.tryAcquire();
        if (acquired) {
            activeAccountProcessingTasks.incrementAndGet();
            log.debug("Acquired account processing resource. Active: {}", activeAccountProcessingTasks.get());
        } else {
            log.debug("Failed to acquire account processing resource. Available: {}", 
                    accountProcessingSemaphore.availablePermits());
        }
        return acquired;
    }
    
    public void releaseAccountProcessingResources() {
        accountProcessingSemaphore.release();
        activeAccountProcessingTasks.decrementAndGet();
        log.debug("Released account processing resource. Active: {}", activeAccountProcessingTasks.get());
    }
    
    public boolean acquireBalanceUpdateResources() {
        boolean acquired = balanceUpdateSemaphore.tryAcquire();
        if (acquired) {
            activeBalanceUpdateTasks.incrementAndGet();
            log.debug("Acquired balance update resource. Active: {}", activeBalanceUpdateTasks.get());
        }
        return acquired;
    }
    
    public void releaseBalanceUpdateResources() {
        balanceUpdateSemaphore.release();
        activeBalanceUpdateTasks.decrementAndGet();
        log.debug("Released balance update resource. Active: {}", activeBalanceUpdateTasks.get());
    }
    
    public boolean acquireValidationResources() {
        boolean acquired = accountValidationSemaphore.tryAcquire();
        if (acquired) {
            activeValidationTasks.incrementAndGet();
            log.debug("Acquired validation resource. Active: {}", activeValidationTasks.get());
        }
        return acquired;
    }
    
    public void releaseValidationResources() {
        accountValidationSemaphore.release();
        activeValidationTasks.decrementAndGet();
        log.debug("Released validation resource. Active: {}", activeValidationTasks.get());
    }
    
    public boolean acquireApiCallResources() {
        boolean acquired = apiCallSemaphore.tryAcquire();
        if (acquired) {
            activeApiCalls.incrementAndGet();
            performanceMonitor.recordApiCallStart();
            log.debug("Acquired API call resource. Active: {}", activeApiCalls.get());
        }
        return acquired;
    }
    
    public void releaseApiCallResources() {
        apiCallSemaphore.release();
        activeApiCalls.decrementAndGet();
        performanceMonitor.recordApiCallEnd();
        log.debug("Released API call resource. Active: {}", activeApiCalls.get());
    }
    
    public boolean acquireBatchProcessingResources() {
        boolean acquired = batchProcessingSemaphore.tryAcquire();
        if (acquired) {
            activeBatchProcessingTasks.incrementAndGet();
            log.debug("Acquired batch processing resource. Active: {}", activeBatchProcessingTasks.get());
        }
        return acquired;
    }
    
    public void releaseBatchProcessingResources() {
        batchProcessingSemaphore.release();
        activeBatchProcessingTasks.decrementAndGet();
        log.debug("Released batch processing resource. Active: {}", activeBatchProcessingTasks.get());
    }
    
    // Dynamic parameter getters
    
    public int getDynamicBatchSize() {
        return dynamicBatchSize.get();
    }
    
    public int getDynamicConcurrencyLevel() {
        return dynamicConcurrencyLevel.get();
    }
    
    public int getDynamicBalanceUpdateConcurrency() {
        return dynamicBalanceUpdateConcurrency.get();
    }
    
    public int getDynamicValidationConcurrency() {
        return dynamicValidationConcurrency.get();
    }
    
    public int getDynamicApiCallConcurrency() {
        return dynamicApiCallConcurrency.get();
    }
    
    // Resource utilization
    
    public ResourceUtilization getResourceUtilization() {
        return new ResourceUtilization(
                activeAccountProcessingTasks.get(),
                activeBalanceUpdateTasks.get(),
                activeValidationTasks.get(),
                activeApiCalls.get(),
                activeBatchProcessingTasks.get(),
                accountProcessingSemaphore.availablePermits(),
                balanceUpdateSemaphore.availablePermits(),
                accountValidationSemaphore.availablePermits(),
                apiCallSemaphore.availablePermits(),
                batchProcessingSemaphore.availablePermits(),
                currentCpuUsage,
                currentMemoryUsage,
                availableProcessors
        );
    }
    
    // Adaptive resource management
    
    @Scheduled(fixedDelayString = "#{@adaptiveAccountResourceManager.getAdaptationInterval()}")
    public void adaptResourceLimits() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastAdaptation = currentTime - lastAdaptationTime.get();
        
        if (timeSinceLastAdaptation < adaptationInterval.get()) {
            return;
        }
        
        updateSystemMetrics();
        
        var performanceReport = performanceMonitor.getPerformanceReport();
        var recommendations = performanceMonitor.getRecommendations();
        
        log.info("Adapting resource limits - CPU: {:.2f}%, Memory: {:.2f}%, Efficiency: {:.2f}%, " +
                "Throughput: {:.2f} ops/sec, Error Rate: {:.2f}%",
                currentCpuUsage * 100, currentMemoryUsage * 100, 
                performanceReport.processingEfficiency() * 100,
                performanceReport.currentThroughput(),
                performanceReport.errorRate() * 100);
        
        // Adapt based on system resources and performance
        adaptBatchSize(performanceReport, recommendations);
        adaptConcurrencyLevels(performanceReport, recommendations);
        adaptSpecializedConcurrency(performanceReport);
        adaptAdaptationInterval(performanceReport);
        
        lastAdaptationTime.set(currentTime);
        
        log.info("Resource adaptation completed - Batch: {}, Concurrency: {}, Balance: {}, " +
                "Validation: {}, API: {}, Interval: {}ms",
                dynamicBatchSize.get(), dynamicConcurrencyLevel.get(),
                dynamicBalanceUpdateConcurrency.get(), dynamicValidationConcurrency.get(),
                dynamicApiCallConcurrency.get(), adaptationInterval.get());
    }
    
    private void updateSystemMetrics() {
        // Update CPU usage
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            currentCpuUsage = sunOsBean.getCpuLoad();
        } else {
            currentCpuUsage = osBean.getSystemLoadAverage() / availableProcessors;
        }
        
        // Update memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        currentMemoryUsage = (double) (totalMemory - freeMemory) / maxMemory;
        
        log.debug("System metrics updated - CPU: {:.2f}%, Memory: {:.2f}%", 
                currentCpuUsage * 100, currentMemoryUsage * 100);
    }
    
    private void adaptBatchSize(AccountPerformanceMonitor.AccountPerformanceReport performanceReport,
                               AccountPerformanceMonitor.AccountPerformanceRecommendations recommendations) {
        int currentBatchSize = dynamicBatchSize.get();
        int newBatchSize = currentBatchSize;
        
        // Increase batch size if system has capacity and efficiency is good
        if (currentCpuUsage < CPU_THRESHOLD_LOW && currentMemoryUsage < MEMORY_THRESHOLD_LOW 
                && performanceReport.processingEfficiency() > 0.85) {
            newBatchSize = Math.min(currentBatchSize + 50, MAX_BATCH_SIZE);
        }
        // Decrease batch size if system is under pressure or efficiency is poor
        else if (currentCpuUsage > CPU_THRESHOLD_HIGH || currentMemoryUsage > MEMORY_THRESHOLD_HIGH 
                || performanceReport.processingEfficiency() < 0.70) {
            newBatchSize = Math.max(currentBatchSize - 50, MIN_BATCH_SIZE);
        }
        // Use recommendation if available
        else if (recommendations.recommendedBatchSize() != currentBatchSize) {
            newBatchSize = Math.max(MIN_BATCH_SIZE, 
                    Math.min(MAX_BATCH_SIZE, recommendations.recommendedBatchSize()));
        }
        
        if (newBatchSize != currentBatchSize) {
            dynamicBatchSize.set(newBatchSize);
            log.info("Adapted batch size: {} -> {}", currentBatchSize, newBatchSize);
        }
    }
    
    private void adaptConcurrencyLevels(AccountPerformanceMonitor.AccountPerformanceReport performanceReport,
                                       AccountPerformanceMonitor.AccountPerformanceRecommendations recommendations) {
        int currentConcurrency = dynamicConcurrencyLevel.get();
        int newConcurrency = currentConcurrency;
        
        // Increase concurrency if system has capacity and throughput is low
        if (currentCpuUsage < CPU_THRESHOLD_LOW && performanceReport.currentThroughput() < 50
                && performanceReport.errorRate() < 0.05) {
            newConcurrency = Math.min(currentConcurrency + 20, MAX_CONCURRENCY);
        }
        // Decrease concurrency if system is under pressure or error rate is high
        else if (currentCpuUsage > CPU_THRESHOLD_HIGH || performanceReport.errorRate() > 0.15) {
            newConcurrency = Math.max(currentConcurrency - 20, MIN_CONCURRENCY);
        }
        // Use recommendation if available
        else if (recommendations.recommendedConcurrency() != currentConcurrency) {
            newConcurrency = Math.max(MIN_CONCURRENCY, 
                    Math.min(MAX_CONCURRENCY, recommendations.recommendedConcurrency()));
        }
        
        if (newConcurrency != currentConcurrency) {
            dynamicConcurrencyLevel.set(newConcurrency);
            updateSemaphoreCapacity(accountProcessingSemaphore, newConcurrency);
            log.info("Adapted concurrency level: {} -> {}", currentConcurrency, newConcurrency);
        }
    }
    
    private void adaptSpecializedConcurrency(AccountPerformanceMonitor.AccountPerformanceReport performanceReport) {
        // Adapt balance update concurrency
        int currentBalanceConcurrency = dynamicBalanceUpdateConcurrency.get();
        int newBalanceConcurrency = currentBalanceConcurrency;
        
        if (performanceReport.totalBalancesUpdated() > 0) {
            double balanceUpdateRatio = (double) performanceReport.totalBalancesUpdated() / 
                    performanceReport.totalAccountsProcessed();
            
            if (balanceUpdateRatio > 0.8 && currentMemoryUsage < MEMORY_THRESHOLD_LOW) {
                newBalanceConcurrency = Math.min(currentBalanceConcurrency + 10, MAX_BALANCE_CONCURRENCY);
            } else if (balanceUpdateRatio < 0.2 || currentMemoryUsage > MEMORY_THRESHOLD_HIGH) {
                newBalanceConcurrency = Math.max(currentBalanceConcurrency - 5, MIN_BALANCE_CONCURRENCY);
            }
        }
        
        if (newBalanceConcurrency != currentBalanceConcurrency) {
            dynamicBalanceUpdateConcurrency.set(newBalanceConcurrency);
            updateSemaphoreCapacity(balanceUpdateSemaphore, newBalanceConcurrency);
            log.info("Adapted balance update concurrency: {} -> {}", 
                    currentBalanceConcurrency, newBalanceConcurrency);
        }
        
        // Adapt validation concurrency
        int currentValidationConcurrency = dynamicValidationConcurrency.get();
        int newValidationConcurrency = currentValidationConcurrency;
        
        if (performanceReport.totalAccountValidations() > 0) {
            double validationRatio = (double) performanceReport.totalAccountValidations() / 
                    performanceReport.totalAccountsProcessed();
            
            if (validationRatio > 0.5 && currentCpuUsage < CPU_THRESHOLD_LOW) {
                newValidationConcurrency = Math.min(currentValidationConcurrency + 5, MAX_VALIDATION_CONCURRENCY);
            } else if (validationRatio < 0.1 || currentCpuUsage > CPU_THRESHOLD_HIGH) {
                newValidationConcurrency = Math.max(currentValidationConcurrency - 3, MIN_VALIDATION_CONCURRENCY);
            }
        }
        
        if (newValidationConcurrency != currentValidationConcurrency) {
            dynamicValidationConcurrency.set(newValidationConcurrency);
            updateSemaphoreCapacity(accountValidationSemaphore, newValidationConcurrency);
            log.info("Adapted validation concurrency: {} -> {}", 
                    currentValidationConcurrency, newValidationConcurrency);
        }
        
        // Adapt API call concurrency
        int currentApiConcurrency = dynamicApiCallConcurrency.get();
        int newApiConcurrency = currentApiConcurrency;
        
        if (performanceReport.totalApiCalls() > 0 && performanceReport.errorRate() < 0.05) {
            if (performanceReport.currentThroughput() < 20 && currentMemoryUsage < MEMORY_THRESHOLD_LOW) {
                newApiConcurrency = Math.min(currentApiConcurrency + 50, MAX_API_CONCURRENCY);
            } else if (performanceReport.errorRate() > 0.10) {
                newApiConcurrency = Math.max(currentApiConcurrency - 30, MIN_API_CONCURRENCY);
            }
        }
        
        if (newApiConcurrency != currentApiConcurrency) {
            dynamicApiCallConcurrency.set(newApiConcurrency);
            updateSemaphoreCapacity(apiCallSemaphore, newApiConcurrency);
            log.info("Adapted API call concurrency: {} -> {}", currentApiConcurrency, newApiConcurrency);
        }
    }
    
    private void adaptAdaptationInterval(AccountPerformanceMonitor.AccountPerformanceReport performanceReport) {
        int currentInterval = adaptationInterval.get();
        int newInterval = currentInterval;
        
        // Faster adaptation under high load or poor performance
        if (currentCpuUsage > CPU_THRESHOLD_HIGH || currentMemoryUsage > MEMORY_THRESHOLD_HIGH 
                || performanceReport.processingEfficiency() < 0.70) {
            newInterval = Math.max(10000, currentInterval - 5000); // Min 10 seconds
        }
        // Slower adaptation under stable conditions
        else if (currentCpuUsage < CPU_THRESHOLD_LOW && currentMemoryUsage < MEMORY_THRESHOLD_LOW 
                && performanceReport.processingEfficiency() > 0.90) {
            newInterval = Math.min(120000, currentInterval + 10000); // Max 2 minutes
        }
        
        if (newInterval != currentInterval) {
            adaptationInterval.set(newInterval);
            log.info("Adapted adaptation interval: {}ms -> {}ms", currentInterval, newInterval);
        }
    }
    
    private void updateSemaphoreCapacity(Semaphore semaphore, int newCapacity) {
        int currentCapacity = semaphore.availablePermits() + semaphore.getQueueLength();
        int difference = newCapacity - currentCapacity;
        
        if (difference > 0) {
            semaphore.release(difference);
        } else if (difference < 0) {
            try {
                semaphore.acquire(-difference);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while reducing semaphore capacity", e);
            }
        }
    }
    
    public int getAdaptationInterval() {
        return adaptationInterval.get();
    }
    
    // Resource utilization record
    
    public record ResourceUtilization(
            int activeAccountProcessingTasks,
            int activeBalanceUpdateTasks,
            int activeValidationTasks,
            int activeApiCalls,
            int activeBatchProcessingTasks,
            int availableAccountProcessingPermits,
            int availableBalanceUpdatePermits,
            int availableValidationPermits,
            int availableApiCallPermits,
            int availableBatchProcessingPermits,
            double currentCpuUsage,
            double currentMemoryUsage,
            int availableProcessors
    ) {}
}