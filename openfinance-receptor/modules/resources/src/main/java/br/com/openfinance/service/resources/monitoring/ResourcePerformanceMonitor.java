package br.com.openfinance.service.resources.monitoring;

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
 * Comprehensive performance monitoring for resource processing operations.
 * Provides detailed metrics, performance analysis, and optimization recommendations
 * specifically for Open Finance Brasil resource management.
 */
@Slf4j
@Component
public class ResourcePerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    
    // Core metrics
    private final Counter resourcesDiscoveredCounter;
    private final Counter resourcesSyncedCounter;
    private final Counter resourcesValidatedCounter;
    private final Counter resourcesMonitoredCounter;
    private final Counter errorCounter;
    private final Counter apiCallCounter;
    
    private final Timer resourceDiscoveryTimer;
    private final Timer resourceSyncTimer;
    private final Timer resourceValidationTimer;
    private final Timer resourceMonitoringTimer;
    private final Timer apiCallTimer;
    private final Timer batchProcessingTimer;
    
    // Performance tracking
    private final AtomicLong totalResourcesDiscovered = new AtomicLong(0);
    private final AtomicLong totalResourcesSynced = new AtomicLong(0);
    private final AtomicLong totalResourcesValidated = new AtomicLong(0);
    private final AtomicLong totalResourcesMonitored = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalApiCalls = new AtomicLong(0);
    private final AtomicInteger activeVirtualThreads = new AtomicInteger(0);
    private final AtomicInteger concurrentResourceOperations = new AtomicInteger(0);
    
    // Timing metrics
    private final DoubleAdder totalDiscoveryTime = new DoubleAdder();
    private final DoubleAdder totalSyncTime = new DoubleAdder();
    private final DoubleAdder totalValidationTime = new DoubleAdder();
    private final DoubleAdder totalMonitoringTime = new DoubleAdder();
    
    // Operation tracking
    private final ConcurrentHashMap<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> operationTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // Performance windows
    private final AtomicLong lastMetricReset = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong operationsInCurrentWindow = new AtomicLong(0);
    private final DoubleAdder timeInCurrentWindow = new DoubleAdder();
    
    public ResourcePerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.resourcesDiscoveredCounter = Counter.builder("openfinance.resources.discovered.total")
                .description("Total number of resources discovered")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourcesSyncedCounter = Counter.builder("openfinance.resources.synced.total")
                .description("Total number of resources synchronized")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourcesValidatedCounter = Counter.builder("openfinance.resources.validated.total")
                .description("Total number of resources validated")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourcesMonitoredCounter = Counter.builder("openfinance.resources.monitored.total")
                .description("Total number of resources monitored")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.errorCounter = Counter.builder("openfinance.resources.errors.total")
                .description("Total number of resource processing errors")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.apiCallCounter = Counter.builder("openfinance.resources.api.calls.total")
                .description("Total number of API calls made")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        // Initialize timers
        this.resourceDiscoveryTimer = Timer.builder("openfinance.resources.discovery.duration")
                .description("Resource discovery operation duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourceSyncTimer = Timer.builder("openfinance.resources.sync.duration")
                .description("Resource synchronization duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourceValidationTimer = Timer.builder("openfinance.resources.validation.duration")
                .description("Resource validation duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.resourceMonitoringTimer = Timer.builder("openfinance.resources.monitoring.duration")
                .description("Resource monitoring duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.apiCallTimer = Timer.builder("openfinance.resources.api.call.duration")
                .description("API call duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        this.batchProcessingTimer = Timer.builder("openfinance.resources.batch.processing.duration")
                .description("Batch processing duration")
                .tag("component", "resource-service")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("openfinance.resources.virtual.threads.active")
                .description("Number of active Virtual Threads for resource processing")
                .tag("component", "resource-service")
                .register(meterRegistry, this, monitor -> monitor.activeVirtualThreads.get());
        
        Gauge.builder("openfinance.resources.operations.concurrent")
                .description("Number of concurrent resource operations")
                .tag("component", "resource-service")
                .register(meterRegistry, this, monitor -> monitor.concurrentResourceOperations.get());
        
        Gauge.builder("openfinance.resources.throughput.current")
                .description("Current resource processing throughput (operations/second)")
                .tag("component", "resource-service")
                .register(meterRegistry, this, this::getCurrentThroughput);
        
        Gauge.builder("openfinance.resources.efficiency.processing")
                .description("Resource processing efficiency ratio")
                .tag("component", "resource-service")
                .register(meterRegistry, this, this::getProcessingEfficiency);
    }
    
    // Recording methods
    
    public void recordResourceOperation(String operationType, boolean success, long durationMs) {
        operationCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        operationTimes.computeIfAbsent(operationType, k -> new DoubleAdder()).add(durationMs);
        
        operationsInCurrentWindow.incrementAndGet();
        timeInCurrentWindow.add(durationMs);
        
        switch (operationType) {
            case "DISCOVERY", "DISCOVERY_START", "DISCOVERY_COMPLETE" -> {
                resourcesDiscoveredCounter.increment();
                if (durationMs > 0) {
                    resourceDiscoveryTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalDiscoveryTime.add(durationMs);
                }
                totalResourcesDiscovered.incrementAndGet();
            }
            case "SYNC", "SYNC_START", "SYNC_COMPLETE" -> {
                resourcesSyncedCounter.increment();
                if (durationMs > 0) {
                    resourceSyncTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalSyncTime.add(durationMs);
                }
                totalResourcesSynced.incrementAndGet();
            }
            case "VALIDATION", "VALIDATION_START", "VALIDATION_COMPLETE" -> {
                resourcesValidatedCounter.increment();
                if (durationMs > 0) {
                    resourceValidationTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalValidationTime.add(durationMs);
                }
                totalResourcesValidated.incrementAndGet();
            }
            case "MONITORING", "HEALTH_CHECK", "HEALTH_CHECK_START", "HEALTH_CHECK_COMPLETE" -> {
                resourcesMonitoredCounter.increment();
                if (durationMs > 0) {
                    resourceMonitoringTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    totalMonitoringTime.add(durationMs);
                }
                totalResourcesMonitored.incrementAndGet();
            }
        }
        
        if (!success) {
            recordError("operation_failure", operationType, true);
        }
        
        log.debug("Recorded resource operation: type={}, success={}, duration={}ms", 
                operationType, success, durationMs);
    }
    
    public void recordBatchProcessing(int resourceCount, double durationMs) {
        totalBatchesProcessed.incrementAndGet();
        batchProcessingTimer.record((long) durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.info("Recorded batch processing: {} resources in {:.2f}ms ({:.2f} resources/second)", 
                resourceCount, durationMs, resourceCount * 1000.0 / durationMs);
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
        concurrentResourceOperations.incrementAndGet();
        apiCallCounter.increment();
        totalApiCalls.incrementAndGet();
    }
    
    public void recordApiCallEnd() {
        concurrentResourceOperations.decrementAndGet();
    }
    
    public void recordApiCall(long durationMs) {
        apiCallTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    // Metrics retrieval
    
    public ResourcePerformanceReport getPerformanceReport() {
        return new ResourcePerformanceReport(
                totalResourcesDiscovered.get(),
                totalResourcesSynced.get(),
                totalResourcesValidated.get(),
                totalResourcesMonitored.get(),
                totalBatchesProcessed.get(),
                totalErrors.get(),
                totalApiCalls.get(),
                getCurrentThroughput(),
                getProcessingEfficiency(),
                getAverageDiscoveryTime(),
                getAverageSyncTime(),
                getAverageValidationTime(),
                getAverageMonitoringTime(),
                activeVirtualThreads.get(),
                concurrentResourceOperations.get(),
                getErrorRate()
        );
    }
    
    public ResourcePerformanceRecommendations getRecommendations() {
        double efficiency = getProcessingEfficiency();
        double throughput = getCurrentThroughput();
        double errorRate = getErrorRate();
        
        int recommendedBatchSize = calculateRecommendedBatchSize(efficiency, throughput);
        int recommendedConcurrency = calculateRecommendedConcurrency(efficiency, throughput);
        double processingEfficiency = efficiency;
        double discoverySuccessRate = calculateDiscoverySuccessRate();
        
        return new ResourcePerformanceRecommendations(
                recommendedBatchSize,
                recommendedConcurrency,
                processingEfficiency,
                discoverySuccessRate,
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
        long totalOperations = totalResourcesDiscovered.get() + totalResourcesSynced.get() + 
                              totalResourcesValidated.get() + totalResourcesMonitored.get();
        if (totalOperations == 0) return 1.0;
        
        long successfulOperations = totalOperations - totalErrors.get();
        return (double) successfulOperations / totalOperations;
    }
    
    private double getAverageDiscoveryTime() {
        long operations = totalResourcesDiscovered.get();
        return operations > 0 ? totalDiscoveryTime.sum() / operations : 0.0;
    }
    
    private double getAverageSyncTime() {
        long operations = totalResourcesSynced.get();
        return operations > 0 ? totalSyncTime.sum() / operations : 0.0;
    }
    
    private double getAverageValidationTime() {
        long operations = totalResourcesValidated.get();
        return operations > 0 ? totalValidationTime.sum() / operations : 0.0;
    }
    
    private double getAverageMonitoringTime() {
        long operations = totalResourcesMonitored.get();
        return operations > 0 ? totalMonitoringTime.sum() / operations : 0.0;
    }
    
    private double getErrorRate() {
        long totalOperations = totalResourcesDiscovered.get() + totalResourcesSynced.get() + 
                              totalResourcesValidated.get() + totalResourcesMonitored.get();
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
    
    private double calculateDiscoverySuccessRate() {
        long totalDiscovery = totalResourcesDiscovered.get();
        if (totalDiscovery == 0) return 1.0;
        
        long discoveryErrors = errorCounts.entrySet().stream()
                .filter(entry -> entry.getKey().contains("discovery"))
                .mapToLong(entry -> entry.getValue().get())
                .sum();
        
        return (double) (totalDiscovery - discoveryErrors) / totalDiscovery;
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
    
    public record ResourcePerformanceReport(
            long totalResourcesDiscovered,
            long totalResourcesSynced,
            long totalResourcesValidated,
            long totalResourcesMonitored,
            long totalBatchesProcessed,
            long totalErrors,
            long totalApiCalls,
            double currentThroughput,
            double processingEfficiency,
            double averageDiscoveryTime,
            double averageSyncTime,
            double averageValidationTime,
            double averageMonitoringTime,
            int activeVirtualThreads,
            int concurrentResourceOperations,
            double errorRate
    ) {}
    
    public record ResourcePerformanceRecommendations(
            int recommendedBatchSize,
            int recommendedConcurrency,
            double processingEfficiency,
            double discoverySuccessRate,
            double throughput,
            String optimizationSuggestions
    ) {}
}