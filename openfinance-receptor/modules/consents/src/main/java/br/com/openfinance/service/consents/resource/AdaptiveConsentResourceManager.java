package br.com.openfinance.service.consents.resource;

import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive resource manager specifically designed for consent processing operations.
 * Dynamically adjusts processing parameters based on system resources, consent-specific
 * performance metrics, and load patterns.
 */
@Slf4j
@Service
public class AdaptiveConsentResourceManager {

    private final ConsentPerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;

    // System monitoring
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    // Adaptive parameters for consent processing
    private volatile int dynamicBatchSize;
    private volatile int dynamicConcurrencyLevel;
    private volatile int dynamicValidationConcurrency;
    private volatile long dynamicProcessingInterval;
    private volatile int dynamicApiCallConcurrency;

    // Resource control
    private final AtomicInteger activeConsentTasks = new AtomicInteger(0);
    private final AtomicInteger activeValidationTasks = new AtomicInteger(0);
    private final AtomicInteger activeApiCalls = new AtomicInteger(0);
    private final AtomicLong memoryUsageThreshold;
    private final AtomicLong cpuUsageThreshold;

    // Semaphores for resource control
    private final Semaphore consentProcessingSemaphore;
    private final Semaphore validationSemaphore;
    private final Semaphore apiCallSemaphore;
    private final Semaphore memoryPressureSemaphore;

    // Configuration
    @Value("${openfinance.consents.adaptive.enabled:true}")
    private boolean adaptiveProcessingEnabled;

    @Value("${openfinance.consents.adaptive.batch-size.min:50}")
    private int minBatchSize;

    @Value("${openfinance.consents.adaptive.batch-size.max:500}")
    private int maxBatchSize;

    @Value("${openfinance.consents.adaptive.concurrency.min:20}")
    private int minConcurrency;

    @Value("${openfinance.consents.adaptive.concurrency.max:500}")
    private int maxConcurrency;

    @Value("${openfinance.consents.adaptive.validation.concurrency.min:10}")
    private int minValidationConcurrency;

    @Value("${openfinance.consents.adaptive.validation.concurrency.max:200}")
    private int maxValidationConcurrency;

    @Value("${openfinance.consents.adaptive.api.concurrency.min:50}")
    private int minApiConcurrency;

    @Value("${openfinance.consents.adaptive.api.concurrency.max:1000}")
    private int maxApiConcurrency;

    @Value("${openfinance.consents.adaptive.memory-threshold:0.85}")
    private double memoryThreshold;

    @Value("${openfinance.consents.adaptive.cpu-threshold:0.80}")
    private double cpuThreshold;

    @Value("${openfinance.consents.adaptive.interval.min:10000}")
    private long minProcessingInterval;

    @Value("${openfinance.consents.adaptive.interval.max:120000}")
    private long maxProcessingInterval;

    public AdaptiveConsentResourceManager(
            ConsentPerformanceMonitor performanceMonitor,
            MeterRegistry meterRegistry) {
        
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // Initialize adaptive parameters
        this.dynamicBatchSize = 200;
        this.dynamicConcurrencyLevel = 100;
        this.dynamicValidationConcurrency = 50;
        this.dynamicApiCallConcurrency = 200;
        this.dynamicProcessingInterval = 30000; // 30 seconds

        // Initialize thresholds
        this.memoryUsageThreshold = new AtomicLong((long) (memoryThreshold * 100));
        this.cpuUsageThreshold = new AtomicLong((long) (cpuThreshold * 100));

        // Initialize resource control semaphores
        this.consentProcessingSemaphore = new Semaphore(maxConcurrency);
        this.validationSemaphore = new Semaphore(maxValidationConcurrency);
        this.apiCallSemaphore = new Semaphore(maxApiConcurrency);
        this.memoryPressureSemaphore = new Semaphore(1);

        log.info("AdaptiveConsentResourceManager initialized with adaptive processing: {}", adaptiveProcessingEnabled);
    }

    /**
     * Acquire resources for consent processing operations.
     */
    public boolean acquireConsentProcessingResources() {
        if (!adaptiveProcessingEnabled) {
            return true;
        }

        try {
            // Check system resources before acquiring
            if (isSystemUnderPressure()) {
                log.debug("System under pressure, throttling consent processing resource acquisition");
                return false;
            }

            // Try to acquire consent processing permit
            boolean acquired = consentProcessingSemaphore.tryAcquire();
            if (acquired) {
                activeConsentTasks.incrementAndGet();
                recordResourceAcquisition("consent_processing", true);
                
                // Update Virtual Thread metrics
                performanceMonitor.recordVirtualThreadUsage(activeConsentTasks.get());
            } else {
                recordResourceAcquisition("consent_processing", false);
            }

            return acquired;

        } catch (Exception e) {
            log.error("Error acquiring consent processing resources", e);
            return false;
        }
    }

    /**
     * Release consent processing resources.
     */
    public void releaseConsentProcessingResources() {
        if (!adaptiveProcessingEnabled) {
            return;
        }

        try {
            consentProcessingSemaphore.release();
            activeConsentTasks.decrementAndGet();
            recordResourceRelease("consent_processing");
            
            // Update Virtual Thread metrics
            performanceMonitor.recordVirtualThreadUsage(activeConsentTasks.get());

        } catch (Exception e) {
            log.error("Error releasing consent processing resources", e);
        }
    }

    /**
     * Acquire resources for consent validation operations.
     */
    public boolean acquireValidationResources() {
        if (!adaptiveProcessingEnabled) {
            return true;
        }

        try {
            if (isSystemUnderPressure()) {
                return false;
            }

            boolean acquired = validationSemaphore.tryAcquire();
            if (acquired) {
                activeValidationTasks.incrementAndGet();
                recordResourceAcquisition("validation", true);
            } else {
                recordResourceAcquisition("validation", false);
            }

            return acquired;

        } catch (Exception e) {
            log.error("Error acquiring validation resources", e);
            return false;
        }
    }

    /**
     * Release validation resources.
     */
    public void releaseValidationResources() {
        if (!adaptiveProcessingEnabled) {
            return;
        }

        try {
            validationSemaphore.release();
            activeValidationTasks.decrementAndGet();
            recordResourceRelease("validation");

        } catch (Exception e) {
            log.error("Error releasing validation resources", e);
        }
    }

    /**
     * Acquire resources for API calls.
     */
    public boolean acquireApiCallResources() {
        if (!adaptiveProcessingEnabled) {
            return true;
        }

        try {
            if (isSystemUnderPressure()) {
                return false;
            }

            boolean acquired = apiCallSemaphore.tryAcquire();
            if (acquired) {
                activeApiCalls.incrementAndGet();
                recordResourceAcquisition("api_call", true);
                performanceMonitor.recordApiCallStart();
            } else {
                recordResourceAcquisition("api_call", false);
            }

            return acquired;

        } catch (Exception e) {
            log.error("Error acquiring API call resources", e);
            return false;
        }
    }

    /**
     * Release API call resources.
     */
    public void releaseApiCallResources() {
        if (!adaptiveProcessingEnabled) {
            return;
        }

        try {
            apiCallSemaphore.release();
            activeApiCalls.decrementAndGet();
            recordResourceRelease("api_call");
            performanceMonitor.recordApiCallEnd();

        } catch (Exception e) {
            log.error("Error releasing API call resources", e);
        }
    }

    /**
     * Check if system is under resource pressure.
     */
    private boolean isSystemUnderPressure() {
        double currentMemoryUsage = getMemoryUsagePercentage();
        double currentCpuUsage = getCpuUsagePercentage();

        boolean memoryPressure = currentMemoryUsage > memoryThreshold;
        boolean cpuPressure = currentCpuUsage > cpuThreshold;

        if (memoryPressure || cpuPressure) {
            log.debug("System pressure detected - Memory: {:.2f}%, CPU: {:.2f}%", 
                    currentMemoryUsage * 100, currentCpuUsage * 100);
        }

        return memoryPressure || cpuPressure;
    }

    /**
     * Get current memory usage percentage.
     */
    private double getMemoryUsagePercentage() {
        try {
            var heapMemory = memoryBean.getHeapMemoryUsage();
            long used = heapMemory.getUsed();
            long max = heapMemory.getMax();
            
            if (max <= 0) {
                return 0.0;
            }
            
            return (double) used / max;
        } catch (Exception e) {
            log.debug("Error getting memory usage", e);
            return 0.0;
        }
    }

    /**
     * Get current CPU usage percentage.
     */
    private double getCpuUsagePercentage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                return sunOsBean.getProcessCpuLoad();
            }
            return 0.0;
        } catch (Exception e) {
            log.debug("Error getting CPU usage", e);
            return 0.0;
        }
    }

    /**
     * Adapt consent processing parameters based on current performance and system state.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void adaptConsentProcessingParameters() {
        if (!adaptiveProcessingEnabled) {
            return;
        }

        try {
            var recommendations = performanceMonitor.getRecommendations();
            var systemMetrics = getSystemMetrics();

            // Adapt consent-specific parameters
            adaptBatchSize(recommendations, systemMetrics);
            adaptConcurrencyLevels(recommendations, systemMetrics);
            adaptProcessingInterval(recommendations, systemMetrics);

            // Log adaptive changes
            logAdaptiveChanges();

        } catch (Exception e) {
            log.error("Error adapting consent processing parameters", e);
        }
    }

    /**
     * Adapt batch size based on consent processing performance.
     */
    private void adaptBatchSize(
            ConsentPerformanceMonitor.ConsentPerformanceRecommendations recommendations, 
            SystemMetrics systemMetrics) {
        
        int recommendedSize = recommendations.recommendedBatchSize();
        double efficiency = recommendations.processingEfficiency();
        double errorRate = recommendations.errorRate();
        double validationSuccessRate = recommendations.validationSuccessRate();

        int newBatchSize = dynamicBatchSize;

        // Increase batch size if performance is excellent and system has capacity
        if (efficiency > 0.95 && errorRate < 0.02 && validationSuccessRate > 0.95 && !systemMetrics.isUnderPressure()) {
            newBatchSize = Math.min(recommendedSize + 25, maxBatchSize);
        }
        // Decrease batch size if performance is poor or system is under pressure
        else if (efficiency < 0.8 || errorRate > 0.1 || validationSuccessRate < 0.8 || systemMetrics.isUnderPressure()) {
            newBatchSize = Math.max(recommendedSize - 25, minBatchSize);
        }
        // Use recommendation as-is
        else {
            newBatchSize = Math.max(minBatchSize, Math.min(recommendedSize, maxBatchSize));
        }

        if (newBatchSize != dynamicBatchSize) {
            log.info("Adapting consent batch size from {} to {} (efficiency: {:.2f}, error rate: {:.2f}, validation success: {:.2f})",
                    dynamicBatchSize, newBatchSize, efficiency, errorRate, validationSuccessRate);
            dynamicBatchSize = newBatchSize;
        }
    }

    /**
     * Adapt concurrency levels for different consent operations.
     */
    private void adaptConcurrencyLevels(
            ConsentPerformanceMonitor.ConsentPerformanceRecommendations recommendations,
            SystemMetrics systemMetrics) {
        
        int recommendedConcurrency = recommendations.recommendedConcurrency();
        double currentThroughput = recommendations.currentThroughput();
        double validationSuccessRate = recommendations.validationSuccessRate();

        // Adapt main processing concurrency
        int newConcurrency = dynamicConcurrencyLevel;
        if (systemMetrics.isUnderPressure()) {
            newConcurrency = Math.max(minConcurrency, dynamicConcurrencyLevel - 20);
        } else if (currentThroughput > 0 && !systemMetrics.isUnderPressure()) {
            newConcurrency = Math.max(minConcurrency, 
                    Math.min(recommendedConcurrency, maxConcurrency));
        }

        // Adapt validation concurrency based on validation success rate
        int newValidationConcurrency = dynamicValidationConcurrency;
        if (validationSuccessRate > 0.95 && !systemMetrics.isUnderPressure()) {
            newValidationConcurrency = Math.min(dynamicValidationConcurrency + 5, maxValidationConcurrency);
        } else if (validationSuccessRate < 0.8) {
            newValidationConcurrency = Math.max(dynamicValidationConcurrency - 5, minValidationConcurrency);
        }

        // Adapt API call concurrency based on timeouts and throughput
        int newApiConcurrency = dynamicApiCallConcurrency;
        if (currentThroughput > recommendations.currentThroughput() * 1.5) {
            // High throughput, might need more API concurrency
            newApiConcurrency = Math.min(dynamicApiCallConcurrency + 50, maxApiConcurrency);
        } else if (currentThroughput < recommendations.currentThroughput() * 0.5) {
            // Low throughput, reduce API concurrency to avoid timeouts
            newApiConcurrency = Math.max(dynamicApiCallConcurrency - 50, minApiConcurrency);
        }

        // Update semaphore permits if concurrency levels changed
        updateSemaphorePermits(newConcurrency, newValidationConcurrency, newApiConcurrency);

        log.info("Adapting concurrency levels - Processing: {} -> {}, Validation: {} -> {}, API: {} -> {}",
                dynamicConcurrencyLevel, newConcurrency,
                dynamicValidationConcurrency, newValidationConcurrency,
                dynamicApiCallConcurrency, newApiConcurrency);

        dynamicConcurrencyLevel = newConcurrency;
        dynamicValidationConcurrency = newValidationConcurrency;
        dynamicApiCallConcurrency = newApiConcurrency;
    }

    /**
     * Update semaphore permits based on new concurrency levels.
     */
    private void updateSemaphorePermits(int newConcurrency, int newValidationConcurrency, int newApiConcurrency) {
        // Update processing semaphore
        int concurrencyDiff = newConcurrency - dynamicConcurrencyLevel;
        if (concurrencyDiff > 0) {
            consentProcessingSemaphore.release(concurrencyDiff);
        } else if (concurrencyDiff < 0) {
            try {
                consentProcessingSemaphore.acquire(Math.abs(concurrencyDiff));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while adjusting processing semaphore permits");
            }
        }

        // Update validation semaphore
        int validationDiff = newValidationConcurrency - dynamicValidationConcurrency;
        if (validationDiff > 0) {
            validationSemaphore.release(validationDiff);
        } else if (validationDiff < 0) {
            try {
                validationSemaphore.acquire(Math.abs(validationDiff));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while adjusting validation semaphore permits");
            }
        }

        // Update API semaphore
        int apiDiff = newApiConcurrency - dynamicApiCallConcurrency;
        if (apiDiff > 0) {
            apiCallSemaphore.release(apiDiff);
        } else if (apiDiff < 0) {
            try {
                apiCallSemaphore.acquire(Math.abs(apiDiff));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while adjusting API semaphore permits");
            }
        }
    }

    /**
     * Adapt processing interval based on system load and consent performance.
     */
    private void adaptProcessingInterval(
            ConsentPerformanceMonitor.ConsentPerformanceRecommendations recommendations,
            SystemMetrics systemMetrics) {
        
        long newInterval = dynamicProcessingInterval;

        // Increase interval if system is under pressure
        if (systemMetrics.isUnderPressure()) {
            newInterval = Math.min(maxProcessingInterval, dynamicProcessingInterval * 2);
        }
        // Decrease interval if system has capacity and performance is good
        else if (recommendations.processingEfficiency() > 0.95 && 
                 recommendations.errorRate() < 0.02 &&
                 recommendations.validationSuccessRate() > 0.95) {
            newInterval = Math.max(minProcessingInterval, dynamicProcessingInterval / 2);
        }

        if (newInterval != dynamicProcessingInterval) {
            log.info("Adapting consent processing interval from {}ms to {}ms", 
                    dynamicProcessingInterval, newInterval);
            dynamicProcessingInterval = newInterval;
        }
    }

    /**
     * Get current system metrics.
     */
    private SystemMetrics getSystemMetrics() {
        double memoryUsage = getMemoryUsagePercentage();
        double cpuUsage = getCpuUsagePercentage();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        
        boolean underPressure = memoryUsage > memoryThreshold || cpuUsage > cpuThreshold;

        return new SystemMetrics(
                memoryUsage,
                cpuUsage,
                availableProcessors,
                freeMemory,
                totalMemory,
                underPressure
        );
    }

    /**
     * Record resource acquisition metrics.
     */
    private void recordResourceAcquisition(String resourceType, boolean successful) {
        meterRegistry.counter("consent.resource.acquisition",
                "resource_type", resourceType,
                "successful", String.valueOf(successful))
                .increment();

        if (successful) {
            switch (resourceType) {
                case "consent_processing" -> 
                    meterRegistry.gauge("consent.resource.active.processing", activeConsentTasks.get());
                case "validation" -> 
                    meterRegistry.gauge("consent.resource.active.validation", activeValidationTasks.get());
                case "api_call" -> 
                    meterRegistry.gauge("consent.resource.active.api", activeApiCalls.get());
            }
        }
    }

    /**
     * Record resource release metrics.
     */
    private void recordResourceRelease(String resourceType) {
        switch (resourceType) {
            case "consent_processing" -> 
                meterRegistry.gauge("consent.resource.active.processing", activeConsentTasks.get());
            case "validation" -> 
                meterRegistry.gauge("consent.resource.active.validation", activeValidationTasks.get());
            case "api_call" -> 
                meterRegistry.gauge("consent.resource.active.api", activeApiCalls.get());
        }
    }

    /**
     * Log adaptive parameter changes.
     */
    private void logAdaptiveChanges() {
        SystemMetrics systemMetrics = getSystemMetrics();
        
        log.debug("=== Adaptive Consent Resource Management ===");
        log.debug("Dynamic Batch Size: {}", dynamicBatchSize);
        log.debug("Dynamic Processing Concurrency: {}", dynamicConcurrencyLevel);
        log.debug("Dynamic Validation Concurrency: {}", dynamicValidationConcurrency);
        log.debug("Dynamic API Concurrency: {}", dynamicApiCallConcurrency);
        log.debug("Dynamic Processing Interval: {}ms", dynamicProcessingInterval);
        log.debug("Active Processing Tasks: {}", activeConsentTasks.get());
        log.debug("Active Validation Tasks: {}", activeValidationTasks.get());
        log.debug("Active API Calls: {}", activeApiCalls.get());
        log.debug("Available Processing Permits: {}", consentProcessingSemaphore.availablePermits());
        log.debug("Memory Usage: {:.2f}%", systemMetrics.memoryUsage() * 100);
        log.debug("CPU Usage: {:.2f}%", systemMetrics.cpuUsage() * 100);
        log.debug("System Under Pressure: {}", systemMetrics.isUnderPressure());
        log.debug("============================================");

        // Record adaptive metrics
        meterRegistry.gauge("consent.adaptive.batch.size", dynamicBatchSize);
        meterRegistry.gauge("consent.adaptive.concurrency.processing", dynamicConcurrencyLevel);
        meterRegistry.gauge("consent.adaptive.concurrency.validation", dynamicValidationConcurrency);
        meterRegistry.gauge("consent.adaptive.concurrency.api", dynamicApiCallConcurrency);
        meterRegistry.gauge("consent.adaptive.processing.interval", dynamicProcessingInterval);
        meterRegistry.gauge("consent.system.memory.usage", systemMetrics.memoryUsage());
        meterRegistry.gauge("consent.system.cpu.usage", systemMetrics.cpuUsage());
    }

    // Getters for current adaptive parameters
    public int getDynamicBatchSize() { return dynamicBatchSize; }
    public int getDynamicConcurrencyLevel() { return dynamicConcurrencyLevel; }
    public int getDynamicValidationConcurrency() { return dynamicValidationConcurrency; }
    public int getDynamicApiCallConcurrency() { return dynamicApiCallConcurrency; }
    public long getDynamicProcessingInterval() { return dynamicProcessingInterval; }
    
    public int getActiveConsentTasks() { return activeConsentTasks.get(); }
    public int getActiveValidationTasks() { return activeValidationTasks.get(); }
    public int getActiveApiCalls() { return activeApiCalls.get(); }
    
    public int getAvailableProcessingPermits() { return consentProcessingSemaphore.availablePermits(); }
    public int getAvailableValidationPermits() { return validationSemaphore.availablePermits(); }
    public int getAvailableApiCallPermits() { return apiCallSemaphore.availablePermits(); }

    /**
     * Get current resource utilization specific to consent processing.
     */
    public ConsentResourceUtilization getResourceUtilization() {
        SystemMetrics systemMetrics = getSystemMetrics();
        
        return new ConsentResourceUtilization(
                activeConsentTasks.get(),
                activeValidationTasks.get(),
                activeApiCalls.get(),
                dynamicConcurrencyLevel,
                dynamicValidationConcurrency,
                dynamicApiCallConcurrency,
                consentProcessingSemaphore.availablePermits(),
                validationSemaphore.availablePermits(),
                apiCallSemaphore.availablePermits(),
                systemMetrics.memoryUsage(),
                systemMetrics.cpuUsage(),
                systemMetrics.isUnderPressure()
        );
    }

    // Support records
    private record SystemMetrics(
            double memoryUsage,
            double cpuUsage,
            int availableProcessors,
            long freeMemory,
            long totalMemory,
            boolean isUnderPressure
    ) {}

    public record ConsentResourceUtilization(
            int activeProcessingTasks,
            int activeValidationTasks,
            int activeApiCalls,
            int maxProcessingConcurrency,
            int maxValidationConcurrency,
            int maxApiConcurrency,
            int availableProcessingPermits,
            int availableValidationPermits,
            int availableApiCallPermits,
            double memoryUsage,
            double cpuUsage,
            boolean isUnderPressure
    ) {
        public double getProcessingUtilizationPercentage() {
            return maxProcessingConcurrency > 0 ? (double) activeProcessingTasks / maxProcessingConcurrency * 100 : 0.0;
        }
        
        public double getValidationUtilizationPercentage() {
            return maxValidationConcurrency > 0 ? (double) activeValidationTasks / maxValidationConcurrency * 100 : 0.0;
        }
        
        public double getApiCallUtilizationPercentage() {
            return maxApiConcurrency > 0 ? (double) activeApiCalls / maxApiConcurrency * 100 : 0.0;
        }
    }
}