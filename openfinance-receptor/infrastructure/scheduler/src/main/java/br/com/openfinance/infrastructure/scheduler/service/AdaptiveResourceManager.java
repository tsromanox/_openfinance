package br.com.openfinance.infrastructure.scheduler.service;

import br.com.openfinance.infrastructure.scheduler.monitoring.SchedulerPerformanceMonitor;
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
 * Adaptive resource manager that dynamically adjusts processing parameters
 * based on system resources, performance metrics, and load patterns.
 */
@Slf4j
@Service
public class AdaptiveResourceManager {

    private final SchedulerPerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;

    // System monitoring
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    // Adaptive parameters
    private volatile int dynamicBatchSize;
    private volatile int dynamicConcurrencyLevel;
    private volatile long dynamicProcessingInterval;

    // Resource limits
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicLong memoryUsageThreshold;
    private final AtomicLong cpuUsageThreshold;

    // Resource control
    private final Semaphore resourceSemaphore;
    private final Semaphore memoryPressureSemaphore;

    // Configuration
    @Value("${openfinance.scheduler.adaptive.enabled:true}")
    private boolean adaptiveSchedulingEnabled;

    @Value("${openfinance.scheduler.adaptive.batch-size.min:10}")
    private int minBatchSize;

    @Value("${openfinance.scheduler.adaptive.batch-size.max:500}")
    private int maxBatchSize;

    @Value("${openfinance.scheduler.adaptive.concurrency.min:5}")
    private int minConcurrency;

    @Value("${openfinance.scheduler.adaptive.concurrency.max:200}")
    private int maxConcurrency;

    @Value("${openfinance.scheduler.adaptive.memory-threshold:0.85}")
    private double memoryThreshold;

    @Value("${openfinance.scheduler.adaptive.cpu-threshold:0.80}")
    private double cpuThreshold;

    @Value("${openfinance.scheduler.adaptive.interval.min:5000}")
    private long minProcessingInterval;

    @Value("${openfinance.scheduler.adaptive.interval.max:60000}")
    private long maxProcessingInterval;

    public AdaptiveResourceManager(
            SchedulerPerformanceMonitor performanceMonitor,
            MeterRegistry meterRegistry) {
        
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();

        // Initialize adaptive parameters
        this.dynamicBatchSize = 100;
        this.dynamicConcurrencyLevel = 50;
        this.dynamicProcessingInterval = 30000; // 30 seconds

        // Initialize thresholds
        this.memoryUsageThreshold = new AtomicLong((long) (memoryThreshold * 100));
        this.cpuUsageThreshold = new AtomicLong((long) (cpuThreshold * 100));

        // Initialize resource control
        this.resourceSemaphore = new Semaphore(maxConcurrency);
        this.memoryPressureSemaphore = new Semaphore(1);

        log.info("AdaptiveResourceManager initialized with adaptive scheduling: {}", adaptiveSchedulingEnabled);
    }

    /**
     * Acquire resources for task execution.
     */
    public boolean acquireResources() {
        try {
            if (!adaptiveSchedulingEnabled) {
                return true;
            }

            // Check system resources before acquiring
            if (isSystemUnderPressure()) {
                log.debug("System under pressure, throttling resource acquisition");
                return false;
            }

            // Try to acquire resource permit
            boolean acquired = resourceSemaphore.tryAcquire();
            if (acquired) {
                activeTasks.incrementAndGet();
                recordResourceAcquisition(true);
            } else {
                recordResourceAcquisition(false);
            }

            return acquired;

        } catch (Exception e) {
            log.error("Error acquiring resources", e);
            return false;
        }
    }

    /**
     * Release resources after task completion.
     */
    public void releaseResources() {
        try {
            if (!adaptiveSchedulingEnabled) {
                return;
            }

            resourceSemaphore.release();
            activeTasks.decrementAndGet();
            recordResourceRelease();

        } catch (Exception e) {
            log.error("Error releasing resources", e);
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
     * Adapt processing parameters based on current performance and system state.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void adaptProcessingParameters() {
        if (!adaptiveSchedulingEnabled) {
            return;
        }

        try {
            var recommendations = performanceMonitor.getRecommendations();
            var systemMetrics = getSystemMetrics();

            // Adapt batch size
            adaptBatchSize(recommendations, systemMetrics);

            // Adapt concurrency level
            adaptConcurrencyLevel(recommendations, systemMetrics);

            // Adapt processing interval
            adaptProcessingInterval(recommendations, systemMetrics);

            // Log adaptive changes
            logAdaptiveChanges();

        } catch (Exception e) {
            log.error("Error adapting processing parameters", e);
        }
    }

    /**
     * Adapt batch size based on performance and system metrics.
     */
    private void adaptBatchSize(
            SchedulerPerformanceMonitor.PerformanceRecommendations recommendations, 
            SystemMetrics systemMetrics) {
        
        int recommendedSize = recommendations.recommendedBatchSize();
        double efficiency = recommendations.processingEfficiency();
        double errorRate = recommendations.errorRate();

        int newBatchSize = dynamicBatchSize;

        // Increase batch size if performance is good and system has capacity
        if (efficiency > 0.9 && errorRate < 0.05 && !systemMetrics.isUnderPressure()) {
            newBatchSize = Math.min(recommendedSize + 10, maxBatchSize);
        }
        // Decrease batch size if performance is poor or system is under pressure
        else if (efficiency < 0.7 || errorRate > 0.1 || systemMetrics.isUnderPressure()) {
            newBatchSize = Math.max(recommendedSize - 10, minBatchSize);
        }
        // Use recommendation as-is
        else {
            newBatchSize = Math.max(minBatchSize, Math.min(recommendedSize, maxBatchSize));
        }

        if (newBatchSize != dynamicBatchSize) {
            log.info("Adapting batch size from {} to {} (efficiency: {:.2f}, error rate: {:.2f})",
                    dynamicBatchSize, newBatchSize, efficiency, errorRate);
            dynamicBatchSize = newBatchSize;
        }
    }

    /**
     * Adapt concurrency level based on performance and system capacity.
     */
    private void adaptConcurrencyLevel(
            SchedulerPerformanceMonitor.PerformanceRecommendations recommendations,
            SystemMetrics systemMetrics) {
        
        int recommendedConcurrency = recommendations.recommendedConcurrency();
        double currentThroughput = recommendations.currentThroughput();

        int newConcurrency = dynamicConcurrencyLevel;

        // Adjust based on system pressure
        if (systemMetrics.isUnderPressure()) {
            newConcurrency = Math.max(minConcurrency, dynamicConcurrencyLevel - 10);
        }
        // Adjust based on throughput and capacity
        else if (currentThroughput > 0 && !systemMetrics.isUnderPressure()) {
            newConcurrency = Math.max(minConcurrency, 
                    Math.min(recommendedConcurrency, maxConcurrency));
        }

        // Update semaphore permits if concurrency level changed
        if (newConcurrency != dynamicConcurrencyLevel) {
            int difference = newConcurrency - dynamicConcurrencyLevel;
            
            if (difference > 0) {
                resourceSemaphore.release(difference);
            } else {
                try {
                    resourceSemaphore.acquire(Math.abs(difference));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while adjusting semaphore permits");
                }
            }

            log.info("Adapting concurrency level from {} to {} (throughput: {:.2f}, system pressure: {})",
                    dynamicConcurrencyLevel, newConcurrency, currentThroughput, systemMetrics.isUnderPressure());
            dynamicConcurrencyLevel = newConcurrency;
        }
    }

    /**
     * Adapt processing interval based on system load and performance.
     */
    private void adaptProcessingInterval(
            SchedulerPerformanceMonitor.PerformanceRecommendations recommendations,
            SystemMetrics systemMetrics) {
        
        long newInterval = dynamicProcessingInterval;

        // Increase interval if system is under pressure
        if (systemMetrics.isUnderPressure()) {
            newInterval = Math.min(maxProcessingInterval, dynamicProcessingInterval * 2);
        }
        // Decrease interval if system has capacity and performance is good
        else if (recommendations.processingEfficiency() > 0.9 && recommendations.errorRate() < 0.05) {
            newInterval = Math.max(minProcessingInterval, dynamicProcessingInterval / 2);
        }

        if (newInterval != dynamicProcessingInterval) {
            log.info("Adapting processing interval from {}ms to {}ms", 
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
    private void recordResourceAcquisition(boolean successful) {
        meterRegistry.counter("scheduler.resource.acquisition",
                "successful", String.valueOf(successful))
                .increment();

        if (successful) {
            meterRegistry.gauge("scheduler.resource.active.tasks", activeTasks.get());
        }
    }

    /**
     * Record resource release metrics.
     */
    private void recordResourceRelease() {
        meterRegistry.gauge("scheduler.resource.active.tasks", activeTasks.get());
    }

    /**
     * Log adaptive parameter changes.
     */
    private void logAdaptiveChanges() {
        SystemMetrics systemMetrics = getSystemMetrics();
        
        log.debug("=== Adaptive Resource Management ===");
        log.debug("Dynamic Batch Size: {}", dynamicBatchSize);
        log.debug("Dynamic Concurrency Level: {}", dynamicConcurrencyLevel);
        log.debug("Dynamic Processing Interval: {}ms", dynamicProcessingInterval);
        log.debug("Active Tasks: {}", activeTasks.get());
        log.debug("Available Resource Permits: {}", resourceSemaphore.availablePermits());
        log.debug("Memory Usage: {:.2f}%", systemMetrics.memoryUsage() * 100);
        log.debug("CPU Usage: {:.2f}%", systemMetrics.cpuUsage() * 100);
        log.debug("System Under Pressure: {}", systemMetrics.isUnderPressure());
        log.debug("====================================");

        // Record metrics
        meterRegistry.gauge("scheduler.adaptive.batch.size", dynamicBatchSize);
        meterRegistry.gauge("scheduler.adaptive.concurrency.level", dynamicConcurrencyLevel);
        meterRegistry.gauge("scheduler.adaptive.processing.interval", dynamicProcessingInterval);
        meterRegistry.gauge("scheduler.system.memory.usage", systemMetrics.memoryUsage());
        meterRegistry.gauge("scheduler.system.cpu.usage", systemMetrics.cpuUsage());
    }

    // Getters for current adaptive parameters
    public int getDynamicBatchSize() { return dynamicBatchSize; }
    public int getDynamicConcurrencyLevel() { return dynamicConcurrencyLevel; }
    public long getDynamicProcessingInterval() { return dynamicProcessingInterval; }
    public int getActiveTasks() { return activeTasks.get(); }
    public int getAvailableResourcePermits() { return resourceSemaphore.availablePermits(); }

    /**
     * Get current resource utilization.
     */
    public ResourceUtilization getResourceUtilization() {
        SystemMetrics systemMetrics = getSystemMetrics();
        
        return new ResourceUtilization(
                activeTasks.get(),
                dynamicConcurrencyLevel,
                resourceSemaphore.availablePermits(),
                systemMetrics.memoryUsage(),
                systemMetrics.cpuUsage(),
                systemMetrics.isUnderPressure()
        );
    }

    /**
     * System metrics record.
     */
    private record SystemMetrics(
            double memoryUsage,
            double cpuUsage,
            int availableProcessors,
            long freeMemory,
            long totalMemory,
            boolean isUnderPressure
    ) {}

    /**
     * Resource utilization record.
     */
    public record ResourceUtilization(
            int activeTasks,
            int maxConcurrency,
            int availablePermits,
            double memoryUsage,
            double cpuUsage,
            boolean isUnderPressure
    ) {
        public double getResourceUtilizationPercentage() {
            return maxConcurrency > 0 ? (double) activeTasks / maxConcurrency : 0.0;
        }
    }
}