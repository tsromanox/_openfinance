package br.com.openfinance.infrastructure.client.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance monitoring component for tracking Virtual Threads and parallel processing metrics.
 */
@Slf4j
@Component
public class ClientPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final ThreadMXBean threadMXBean;
    private final OperatingSystemMXBean osMXBean;
    
    // Performance metrics
    private final AtomicLong virtualThreadCount = new AtomicLong(0);
    private final AtomicLong platformThreadCount = new AtomicLong(0);
    private final LongAdder totalRequestsProcessed = new LongAdder();
    private final LongAdder concurrentRequestsActive = new LongAdder();
    
    // Performance history for trend analysis
    private volatile double avgCpuUsage = 0.0;
    private volatile long avgMemoryUsage = 0L;
    private volatile double avgResponseTime = 0.0;
    
    public ClientPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        
        initializeMetrics();
    }
    
    private void initializeMetrics() {
        // Virtual Thread metrics
        Gauge.builder("openfinance.threads.virtual")
                .description("Number of virtual threads currently active")
                .register(meterRegistry, this, monitor -> virtualThreadCount.get());
        
        Gauge.builder("openfinance.threads.platform")
                .description("Number of platform threads currently active")
                .register(meterRegistry, this, monitor -> platformThreadCount.get());
        
        // Request metrics
        Gauge.builder("openfinance.requests.active")
                .description("Number of requests currently being processed")
                .register(meterRegistry, this, monitor -> concurrentRequestsActive.sum());
        
        Gauge.builder("openfinance.requests.total")
                .description("Total number of requests processed")
                .register(meterRegistry, this, monitor -> totalRequestsProcessed.sum());
        
        // System metrics
        Gauge.builder("openfinance.system.cpu.usage")
                .description("System CPU usage percentage")
                .register(meterRegistry, this, monitor -> avgCpuUsage);
        
        Gauge.builder("openfinance.system.memory.usage")
                .description("JVM memory usage in bytes")
                .register(meterRegistry, this, monitor -> avgMemoryUsage);
        
        // Performance metrics
        Gauge.builder("openfinance.performance.response.time.avg")
                .description("Average response time in seconds")
                .register(meterRegistry, this, monitor -> avgResponseTime);
        
        log.info("Performance monitoring initialized with metrics registry");
    }
    
    /**
     * Record the start of a request processing.
     */
    public Timer.Sample startRequest() {
        concurrentRequestsActive.increment();
        return Timer.Sample.start(meterRegistry);
    }
    
    /**
     * Record the completion of a request processing.
     */
    public void completeRequest(Timer.Sample sample) {
        concurrentRequestsActive.decrement();
        totalRequestsProcessed.increment();
        
        sample.stop(Timer.builder("openfinance.request.duration")
                .description("Request processing time")
                .register(meterRegistry));
    }
    
    /**
     * Update thread counts - called by scheduler or on-demand.
     */
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void updateThreadMetrics() {
        try {
            // Update platform thread count
            platformThreadCount.set(threadMXBean.getThreadCount());
            
            // Estimate virtual threads (Java 21+ specific)
            long totalThreads = Thread.getAllStackTraces().keySet().size();
            long estimatedVirtualThreads = totalThreads - threadMXBean.getThreadCount();
            virtualThreadCount.set(Math.max(0, estimatedVirtualThreads));
            
            log.debug("Thread metrics updated - Platform: {}, Virtual: {}", 
                     platformThreadCount.get(), virtualThreadCount.get());
            
        } catch (Exception e) {
            log.warn("Failed to update thread metrics: {}", e.getMessage());
        }
    }
    
    /**
     * Update system performance metrics.
     */
    @Scheduled(fixedDelay = 10000) // Every 10 seconds
    public void updateSystemMetrics() {
        try {
            // CPU usage
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean osBean) {
                double cpuUsage = osBean.getCpuLoad() * 100;
                if (cpuUsage >= 0) {
                    this.avgCpuUsage = cpuUsage;
                }
            }
            
            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            this.avgMemoryUsage = totalMemory - freeMemory;
            
            log.debug("System metrics updated - CPU: {:.2f}%, Memory: {} MB", 
                     avgCpuUsage, avgMemoryUsage / (1024 * 1024));
            
        } catch (Exception e) {
            log.warn("Failed to update system metrics: {}", e.getMessage());
        }
    }
    
    /**
     * Calculate and update average response time.
     */
    @Scheduled(fixedDelay = 15000) // Every 15 seconds
    public void updatePerformanceMetrics() {
        try {
            Timer requestTimer = meterRegistry.find("openfinance.request.duration").timer();
            if (requestTimer != null) {
                this.avgResponseTime = requestTimer.mean(java.util.concurrent.TimeUnit.SECONDS);
                
                log.debug("Performance metrics updated - Avg response time: {:.3f}s", 
                         avgResponseTime);
            }
        } catch (Exception e) {
            log.warn("Failed to update performance metrics: {}", e.getMessage());
        }
    }
    
    /**
     * Generate performance report for monitoring dashboards.
     */
    public PerformanceReport generateReport() {
        return new PerformanceReport(
                LocalDateTime.now(),
                virtualThreadCount.get(),
                platformThreadCount.get(),
                totalRequestsProcessed.sum(),
                concurrentRequestsActive.sum(),
                avgCpuUsage,
                avgMemoryUsage,
                avgResponseTime,
                calculateThroughput(),
                assessHealthStatus()
        );
    }
    
    private double calculateThroughput() {
        Timer requestTimer = meterRegistry.find("openfinance.request.duration").timer();
        if (requestTimer != null && requestTimer.count() > 0) {
            return requestTimer.count() / requestTimer.totalTime(java.util.concurrent.TimeUnit.MINUTES);
        }
        return 0.0;
    }
    
    private HealthStatus assessHealthStatus() {
        // Simple health assessment based on key metrics
        if (avgCpuUsage > 90) {
            return HealthStatus.CRITICAL;
        } else if (avgCpuUsage > 70 || avgResponseTime > 5.0) {
            return HealthStatus.WARNING;
        } else if (concurrentRequestsActive.sum() > 500) {
            return HealthStatus.WARNING;
        }
        return HealthStatus.HEALTHY;
    }
    
    /**
     * Log performance summary for operations teams.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void logPerformanceSummary() {
        PerformanceReport report = generateReport();
        
        log.info("Performance Summary - Threads[Virtual: {}, Platform: {}], " +
                "Requests[Total: {}, Active: {}], System[CPU: {:.1f}%, Memory: {} MB], " +
                "Performance[Avg Response: {:.3f}s, Throughput: {:.1f} req/min], Status: {}",
                report.virtualThreads(),
                report.platformThreads(),
                report.totalRequests(),
                report.activeRequests(),
                report.cpuUsage(),
                report.memoryUsageMB(),
                report.avgResponseTimeSeconds(),
                report.throughputPerMinute(),
                report.healthStatus());
    }
    
    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL
    }
    
    public record PerformanceReport(
            LocalDateTime timestamp,
            long virtualThreads,
            long platformThreads,
            long totalRequests,
            long activeRequests,
            double cpuUsage,
            long memoryUsageBytes,
            double avgResponseTimeSeconds,
            double throughputPerMinute,
            HealthStatus healthStatus
    ) {
        public long memoryUsageMB() {
            return memoryUsageBytes / (1024 * 1024);
        }
    }
}