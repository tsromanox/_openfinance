package br.com.openfinance.resources.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
@RequiredArgsConstructor
public class VirtualThreadMetrics implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    
    private Counter virtualThreadCreatedCounter;
    private Counter virtualThreadCompletedCounter;
    private Counter batchProcessingCounter;
    private Counter syncErrorCounter;
    private Timer batchProcessingTimer;
    private Timer resourceSyncTimer;
    
    private final AtomicLong activeVirtualThreads = new AtomicLong(0);
    private final AtomicLong totalResourcesProcessed = new AtomicLong(0);
    private final AtomicLong totalSyncErrors = new AtomicLong(0);

    @PostConstruct
    public void initMetrics() {
        log.info("Initializing Virtual Thread metrics");
        
        // Counters
        virtualThreadCreatedCounter = Counter.builder("virtual_threads_created_total")
                .description("Total number of virtual threads created")
                .register(meterRegistry);
                
        virtualThreadCompletedCounter = Counter.builder("virtual_threads_completed_total")
                .description("Total number of virtual threads completed")
                .register(meterRegistry);
                
        batchProcessingCounter = Counter.builder("batch_processing_total")
                .description("Total number of batch processing operations")
                .register(meterRegistry);
                
        syncErrorCounter = Counter.builder("resource_sync_errors_total")
                .description("Total number of resource sync errors")
                .register(meterRegistry);
        
        // Timers
        batchProcessingTimer = Timer.builder("batch_processing_duration")
                .description("Duration of batch processing operations")
                .register(meterRegistry);
                
        resourceSyncTimer = Timer.builder("resource_sync_duration")
                .description("Duration of individual resource sync operations")
                .register(meterRegistry);
        
        // Gauges
        Gauge.builder("virtual_threads_active")
                .description("Number of currently active virtual threads")
                .register(meterRegistry, activeVirtualThreads, AtomicLong::get);
                
        Gauge.builder("resources_processed_total")
                .description("Total number of resources processed")
                .register(meterRegistry, totalResourcesProcessed, AtomicLong::get);
                
        Gauge.builder("sync_errors_total")
                .description("Total number of sync errors")
                .register(meterRegistry, totalSyncErrors, AtomicLong::get);
        
        // JVM Virtual Thread metrics
        Gauge.builder("jvm_threads_virtual_total")
                .description("Total virtual threads in JVM")
                .register(meterRegistry, this, VirtualThreadMetrics::getVirtualThreadCount);
        
        log.info("Virtual Thread metrics initialized successfully");
    }

    public void recordVirtualThreadCreated() {
        virtualThreadCreatedCounter.increment();
        activeVirtualThreads.incrementAndGet();
    }

    public void recordVirtualThreadCompleted() {
        virtualThreadCompletedCounter.increment();
        activeVirtualThreads.decrementAndGet();
    }

    public void recordBatchProcessing() {
        batchProcessingCounter.increment();
    }

    public Timer.Sample startBatchProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordBatchProcessingTime(Timer.Sample sample) {
        sample.stop(batchProcessingTimer);
    }

    public Timer.Sample startResourceSyncTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordResourceSyncTime(Timer.Sample sample) {
        sample.stop(resourceSyncTimer);
    }

    public void recordResourceProcessed() {
        totalResourcesProcessed.incrementAndGet();
    }

    public void recordSyncError() {
        syncErrorCounter.increment();
        totalSyncErrors.incrementAndGet();
    }

    private double getVirtualThreadCount() {
        try {
            // This is a simplified implementation
            // In a real scenario, you might use JFR or other JVM metrics
            return Thread.getAllStackTraces().keySet().stream()
                    .filter(Thread::isVirtual)
                    .count();
        } catch (Exception e) {
            log.warn("Failed to get virtual thread count: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public Health health() {
        try {
            long activeThreads = activeVirtualThreads.get();
            long processedResources = totalResourcesProcessed.get();
            long errors = totalSyncErrors.get();
            
            Health.Builder builder = Health.up()
                    .withDetail("activeVirtualThreads", activeThreads)
                    .withDetail("processedResources", processedResources)
                    .withDetail("totalErrors", errors);
            
            // Health checks
            if (activeThreads > 50000) {
                builder.withDetail("warning", "High number of active virtual threads");
            }
            
            if (errors > processedResources * 0.1) { // More than 10% error rate
                builder.down().withDetail("error", "High error rate detected");
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Failed to check virtual thread health")
                    .withException(e)
                    .build();
        }
    }

    public long getActiveVirtualThreads() {
        return activeVirtualThreads.get();
    }

    public long getTotalResourcesProcessed() {
        return totalResourcesProcessed.get();
    }

    public long getTotalSyncErrors() {
        return totalSyncErrors.get();
    }
}