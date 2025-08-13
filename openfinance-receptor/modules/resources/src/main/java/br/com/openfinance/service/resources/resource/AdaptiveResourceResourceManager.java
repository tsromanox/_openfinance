package br.com.openfinance.service.resources.resource;

import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
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
 * Adaptive resource manager for resource processing operations.
 * Dynamically adjusts resource allocation based on system performance and load
 * specifically optimized for Open Finance Brasil resource management.
 */
@Slf4j
@Component
public class AdaptiveResourceResourceManager {
    
    private final ResourcePerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;
    private final OperatingSystemMXBean osBean;
    
    // Resource semaphores
    private final Semaphore resourceDiscoverySemaphore;
    private final Semaphore resourceSyncSemaphore;
    private final Semaphore resourceValidationSemaphore;
    private final Semaphore resourceMonitoringSemaphore;
    private final Semaphore apiCallSemaphore;
    private final Semaphore batchProcessingSemaphore;
    
    // Dynamic resource parameters
    private final AtomicInteger dynamicBatchSize = new AtomicInteger(200);
    private final AtomicInteger dynamicConcurrencyLevel = new AtomicInteger(100);
    private final AtomicInteger dynamicDiscoveryConcurrency = new AtomicInteger(50);
    private final AtomicInteger dynamicSyncConcurrency = new AtomicInteger(75);
    private final AtomicInteger dynamicValidationConcurrency = new AtomicInteger(30);
    private final AtomicInteger dynamicMonitoringConcurrency = new AtomicInteger(40);
    private final AtomicInteger dynamicApiCallConcurrency = new AtomicInteger(200);
    
    // Resource tracking
    private final AtomicInteger activeResourceDiscoveryTasks = new AtomicInteger(0);
    private final AtomicInteger activeResourceSyncTasks = new AtomicInteger(0);
    private final AtomicInteger activeResourceValidationTasks = new AtomicInteger(0);
    private final AtomicInteger activeResourceMonitoringTasks = new AtomicInteger(0);
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
    private static final int MIN_DISCOVERY_CONCURRENCY = 5;
    private static final int MAX_DISCOVERY_CONCURRENCY = 200;
    private static final int MIN_SYNC_CONCURRENCY = 10;
    private static final int MAX_SYNC_CONCURRENCY = 300;
    private static final int MIN_VALIDATION_CONCURRENCY = 5;
    private static final int MAX_VALIDATION_CONCURRENCY = 100;
    private static final int MIN_MONITORING_CONCURRENCY = 5;
    private static final int MAX_MONITORING_CONCURRENCY = 150;
    private static final int MIN_API_CONCURRENCY = 20;
    private static final int MAX_API_CONCURRENCY = 1000;
    
    private static final double CPU_THRESHOLD_HIGH = 0.80;
    private static final double CPU_THRESHOLD_LOW = 0.40;
    private static final double MEMORY_THRESHOLD_HIGH = 0.85;
    private static final double MEMORY_THRESHOLD_LOW = 0.50;
    
    public AdaptiveResourceResourceManager(
            ResourcePerformanceMonitor performanceMonitor,
            MeterRegistry meterRegistry) {
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // Initialize semaphores with initial capacity
        this.resourceDiscoverySemaphore = new Semaphore(dynamicDiscoveryConcurrency.get());
        this.resourceSyncSemaphore = new Semaphore(dynamicSyncConcurrency.get());
        this.resourceValidationSemaphore = new Semaphore(dynamicValidationConcurrency.get());
        this.resourceMonitoringSemaphore = new Semaphore(dynamicMonitoringConcurrency.get());
        this.apiCallSemaphore = new Semaphore(dynamicApiCallConcurrency.get());
        this.batchProcessingSemaphore = new Semaphore(10); // Fixed batch processing limit
        
        log.info("Initialized AdaptiveResourceResourceManager with {} processors", availableProcessors);
    }
    
    // Resource acquisition methods
    
    public boolean acquireResourceDiscoveryResources() {
        boolean acquired = resourceDiscoverySemaphore.tryAcquire();
        if (acquired) {
            activeResourceDiscoveryTasks.incrementAndGet();
            log.debug("Acquired resource discovery resource. Active: {}", activeResourceDiscoveryTasks.get());
        } else {
            log.debug("Failed to acquire resource discovery resource. Available: {}", 
                    resourceDiscoverySemaphore.availablePermits());
        }
        return acquired;
    }
    
    public void releaseResourceDiscoveryResources() {
        resourceDiscoverySemaphore.release();
        activeResourceDiscoveryTasks.decrementAndGet();
        log.debug("Released resource discovery resource. Active: {}", activeResourceDiscoveryTasks.get());
    }
    
    public boolean acquireResourceSyncResources() {
        boolean acquired = resourceSyncSemaphore.tryAcquire();
        if (acquired) {
            activeResourceSyncTasks.incrementAndGet();
            log.debug("Acquired resource sync resource. Active: {}", activeResourceSyncTasks.get());
        }
        return acquired;
    }
    
    public void releaseResourceSyncResources() {
        resourceSyncSemaphore.release();
        activeResourceSyncTasks.decrementAndGet();
        log.debug("Released resource sync resource. Active: {}", activeResourceSyncTasks.get());
    }
    
    public boolean acquireResourceValidationResources() {
        boolean acquired = resourceValidationSemaphore.tryAcquire();
        if (acquired) {
            activeResourceValidationTasks.incrementAndGet();
            log.debug("Acquired resource validation resource. Active: {}", activeResourceValidationTasks.get());
        }
        return acquired;
    }
    
    public void releaseResourceValidationResources() {
        resourceValidationSemaphore.release();
        activeResourceValidationTasks.decrementAndGet();
        log.debug("Released resource validation resource. Active: {}", activeResourceValidationTasks.get());
    }
    
    public boolean acquireResourceMonitoringResources() {
        boolean acquired = resourceMonitoringSemaphore.tryAcquire();
        if (acquired) {
            activeResourceMonitoringTasks.incrementAndGet();
            log.debug("Acquired resource monitoring resource. Active: {}", activeResourceMonitoringTasks.get());
        }
        return acquired;
    }
    
    public void releaseResourceMonitoringResources() {
        resourceMonitoringSemaphore.release();
        activeResourceMonitoringTasks.decrementAndGet();
        log.debug("Released resource monitoring resource. Active: {}", activeResourceMonitoringTasks.get());
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
    
    public int getDynamicDiscoveryConcurrency() {
        return dynamicDiscoveryConcurrency.get();
    }
    
    public int getDynamicSyncConcurrency() {
        return dynamicSyncConcurrency.get();
    }
    
    public int getDynamicValidationConcurrency() {
        return dynamicValidationConcurrency.get();
    }
    
    public int getDynamicMonitoringConcurrency() {
        return dynamicMonitoringConcurrency.get();
    }
    
    public int getDynamicApiCallConcurrency() {
        return dynamicApiCallConcurrency.get();
    }
    
    // Resource utilization
    
    public ResourceUtilization getResourceUtilization() {
        return new ResourceUtilization(
                activeResourceDiscoveryTasks.get(),
                activeResourceSyncTasks.get(),
                activeResourceValidationTasks.get(),
                activeResourceMonitoringTasks.get(),
                activeApiCalls.get(),
                activeBatchProcessingTasks.get(),
                resourceDiscoverySemaphore.availablePermits(),
                resourceSyncSemaphore.availablePermits(),
                resourceValidationSemaphore.availablePermits(),
                resourceMonitoringSemaphore.availablePermits(),
                apiCallSemaphore.availablePermits(),
                batchProcessingSemaphore.availablePermits(),
                currentCpuUsage,
                currentMemoryUsage,
                availableProcessors
        );
    }
    
    // Adaptive resource management
    
    @Scheduled(fixedDelayString = "#{@adaptiveResourceResourceManager.getAdaptationInterval()}")
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
        
        log.info("Resource adaptation completed - Batch: {}, Concurrency: {}, Discovery: {}, " +
                "Sync: {}, Validation: {}, Monitoring: {}, API: {}, Interval: {}ms",
                dynamicBatchSize.get(), dynamicConcurrencyLevel.get(), dynamicDiscoveryConcurrency.get(),
                dynamicSyncConcurrency.get(), dynamicValidationConcurrency.get(),
                dynamicMonitoringConcurrency.get(), dynamicApiCallConcurrency.get(), adaptationInterval.get());
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
    
    private void adaptBatchSize(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport,
                               ResourcePerformanceMonitor.ResourcePerformanceRecommendations recommendations) {
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
    
    private void adaptConcurrencyLevels(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport,
                                       ResourcePerformanceMonitor.ResourcePerformanceRecommendations recommendations) {
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
            log.info("Adapted concurrency level: {} -> {}", currentConcurrency, newConcurrency);
        }
    }
    
    private void adaptSpecializedConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        // Adapt discovery concurrency
        adaptDiscoveryConcurrency(performanceReport);
        
        // Adapt sync concurrency
        adaptSyncConcurrency(performanceReport);
        
        // Adapt validation concurrency
        adaptValidationConcurrency(performanceReport);
        
        // Adapt monitoring concurrency
        adaptMonitoringConcurrency(performanceReport);
        
        // Adapt API call concurrency
        adaptApiCallConcurrency(performanceReport);
    }
    
    private void adaptDiscoveryConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        int currentDiscoveryConcurrency = dynamicDiscoveryConcurrency.get();
        int newDiscoveryConcurrency = currentDiscoveryConcurrency;
        
        if (performanceReport.totalResourcesDiscovered() > 0) {
            double discoveryRatio = (double) performanceReport.totalResourcesDiscovered() / 
                    (performanceReport.totalResourcesDiscovered() + performanceReport.totalResourcesSynced() +
                     performanceReport.totalResourcesValidated() + performanceReport.totalResourcesMonitored());
            
            if (discoveryRatio > 0.3 && currentMemoryUsage < MEMORY_THRESHOLD_LOW) {
                newDiscoveryConcurrency = Math.min(currentDiscoveryConcurrency + 10, MAX_DISCOVERY_CONCURRENCY);
            } else if (discoveryRatio < 0.1 || currentMemoryUsage > MEMORY_THRESHOLD_HIGH) {
                newDiscoveryConcurrency = Math.max(currentDiscoveryConcurrency - 5, MIN_DISCOVERY_CONCURRENCY);
            }
        }
        
        if (newDiscoveryConcurrency != currentDiscoveryConcurrency) {
            dynamicDiscoveryConcurrency.set(newDiscoveryConcurrency);
            updateSemaphoreCapacity(resourceDiscoverySemaphore, newDiscoveryConcurrency);
            log.info("Adapted discovery concurrency: {} -> {}", 
                    currentDiscoveryConcurrency, newDiscoveryConcurrency);
        }
    }
    
    private void adaptSyncConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        int currentSyncConcurrency = dynamicSyncConcurrency.get();
        int newSyncConcurrency = currentSyncConcurrency;
        
        if (performanceReport.totalResourcesSynced() > 0) {
            double syncRatio = (double) performanceReport.totalResourcesSynced() / 
                    (performanceReport.totalResourcesDiscovered() + performanceReport.totalResourcesSynced() +
                     performanceReport.totalResourcesValidated() + performanceReport.totalResourcesMonitored());
            
            if (syncRatio > 0.4 && currentCpuUsage < CPU_THRESHOLD_LOW) {
                newSyncConcurrency = Math.min(currentSyncConcurrency + 15, MAX_SYNC_CONCURRENCY);
            } else if (syncRatio < 0.1 || currentCpuUsage > CPU_THRESHOLD_HIGH) {
                newSyncConcurrency = Math.max(currentSyncConcurrency - 10, MIN_SYNC_CONCURRENCY);
            }
        }
        
        if (newSyncConcurrency != currentSyncConcurrency) {
            dynamicSyncConcurrency.set(newSyncConcurrency);
            updateSemaphoreCapacity(resourceSyncSemaphore, newSyncConcurrency);
            log.info("Adapted sync concurrency: {} -> {}", currentSyncConcurrency, newSyncConcurrency);
        }
    }
    
    private void adaptValidationConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        int currentValidationConcurrency = dynamicValidationConcurrency.get();
        int newValidationConcurrency = currentValidationConcurrency;
        
        if (performanceReport.totalResourcesValidated() > 0) {
            double validationRatio = (double) performanceReport.totalResourcesValidated() / 
                    (performanceReport.totalResourcesDiscovered() + performanceReport.totalResourcesSynced() +
                     performanceReport.totalResourcesValidated() + performanceReport.totalResourcesMonitored());
            
            if (validationRatio > 0.3 && currentCpuUsage < CPU_THRESHOLD_LOW) {
                newValidationConcurrency = Math.min(currentValidationConcurrency + 5, MAX_VALIDATION_CONCURRENCY);
            } else if (validationRatio < 0.05 || currentCpuUsage > CPU_THRESHOLD_HIGH) {
                newValidationConcurrency = Math.max(currentValidationConcurrency - 3, MIN_VALIDATION_CONCURRENCY);
            }
        }
        
        if (newValidationConcurrency != currentValidationConcurrency) {
            dynamicValidationConcurrency.set(newValidationConcurrency);
            updateSemaphoreCapacity(resourceValidationSemaphore, newValidationConcurrency);
            log.info("Adapted validation concurrency: {} -> {}", 
                    currentValidationConcurrency, newValidationConcurrency);
        }
    }
    
    private void adaptMonitoringConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        int currentMonitoringConcurrency = dynamicMonitoringConcurrency.get();
        int newMonitoringConcurrency = currentMonitoringConcurrency;
        
        if (performanceReport.totalResourcesMonitored() > 0) {
            double monitoringRatio = (double) performanceReport.totalResourcesMonitored() / 
                    (performanceReport.totalResourcesDiscovered() + performanceReport.totalResourcesSynced() +
                     performanceReport.totalResourcesValidated() + performanceReport.totalResourcesMonitored());
            
            if (monitoringRatio > 0.3 && currentMemoryUsage < MEMORY_THRESHOLD_LOW) {
                newMonitoringConcurrency = Math.min(currentMonitoringConcurrency + 8, MAX_MONITORING_CONCURRENCY);
            } else if (monitoringRatio < 0.1 || currentMemoryUsage > MEMORY_THRESHOLD_HIGH) {
                newMonitoringConcurrency = Math.max(currentMonitoringConcurrency - 5, MIN_MONITORING_CONCURRENCY);
            }
        }
        
        if (newMonitoringConcurrency != currentMonitoringConcurrency) {
            dynamicMonitoringConcurrency.set(newMonitoringConcurrency);
            updateSemaphoreCapacity(resourceMonitoringSemaphore, newMonitoringConcurrency);
            log.info("Adapted monitoring concurrency: {} -> {}", 
                    currentMonitoringConcurrency, newMonitoringConcurrency);
        }
    }
    
    private void adaptApiCallConcurrency(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
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
    
    private void adaptAdaptationInterval(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
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
            int activeResourceDiscoveryTasks,
            int activeResourceSyncTasks,
            int activeResourceValidationTasks,
            int activeResourceMonitoringTasks,
            int activeApiCalls,
            int activeBatchProcessingTasks,
            int availableResourceDiscoveryPermits,
            int availableResourceSyncPermits,
            int availableResourceValidationPermits,
            int availableResourceMonitoringPermits,
            int availableApiCallPermits,
            int availableBatchProcessingPermits,
            double currentCpuUsage,
            double currentMemoryUsage,
            int availableProcessors
    ) {}
}