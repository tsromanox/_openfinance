package br.com.openfinance.infrastructure.scheduler.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Advanced performance monitoring for Virtual Thread scheduler operations.
 * Tracks processing efficiency, resource utilization, and provides adaptive feedback.
 */
@Slf4j
@Component
public class SchedulerPerformanceMonitor {

    private final MeterRegistry meterRegistry;
    
    // Performance metrics
    private final AtomicLong totalJobsProcessed = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final DoubleAdder totalProcessingTime = new DoubleAdder();
    private final AtomicLong activeVirtualThreads = new AtomicLong(0);
    private final AtomicLong peakVirtualThreads = new AtomicLong(0);
    
    // Batch processing metrics
    private final DoubleAdder averageBatchSize = new DoubleAdder();
    private final DoubleAdder averageBatchDuration = new DoubleAdder();
    private final AtomicLong totalBatchDuration = new AtomicLong(0);
    
    // Error tracking
    private final LongAdder processingErrors = new LongAdder();
    private final LongAdder timeoutErrors = new LongAdder();
    private final LongAdder retryableErrors = new LongAdder();
    
    // Performance tracking by job type
    private final ConcurrentMap<String, JobTypeMetrics> jobTypeMetrics = new ConcurrentHashMap<>();
    
    // Adaptive performance feedback
    private volatile double currentThroughput = 0.0;
    private volatile double targetThroughput = 1000.0; // jobs per minute
    private volatile int recommendedBatchSize = 100;
    private volatile int recommendedConcurrency = 50;

    public SchedulerPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        // Job processing metrics
        Gauge.builder("scheduler.jobs.processed.total")
                .description("Total number of jobs processed")
                .register(meterRegistry, this, monitor -> totalJobsProcessed.get());

        Gauge.builder("scheduler.batches.processed.total")
                .description("Total number of batches processed")
                .register(meterRegistry, this, monitor -> totalBatchesProcessed.get());

        Gauge.builder("scheduler.processing.time.total")
                .description("Total processing time in milliseconds")
                .register(meterRegistry, this, monitor -> totalProcessingTime.doubleValue());

        // Virtual Thread metrics
        Gauge.builder("scheduler.virtual.threads.active")
                .description("Current number of active virtual threads")
                .register(meterRegistry, this, monitor -> activeVirtualThreads.get());

        Gauge.builder("scheduler.virtual.threads.peak")
                .description("Peak number of virtual threads")
                .register(meterRegistry, this, monitor -> peakVirtualThreads.get());

        // Performance metrics
        Gauge.builder("scheduler.throughput.current")
                .description("Current processing throughput (jobs/minute)")
                .register(meterRegistry, this, monitor -> currentThroughput);

        Gauge.builder("scheduler.batch.size.average")
                .description("Average batch size")
                .register(meterRegistry, this, monitor -> averageBatchSize.doubleValue());

        Gauge.builder("scheduler.batch.duration.average")
                .description("Average batch duration in milliseconds")
                .register(meterRegistry, this, monitor -> averageBatchDuration.doubleValue());

        // Adaptive recommendations
        Gauge.builder("scheduler.recommended.batch.size")
                .description("Recommended batch size based on performance")
                .register(meterRegistry, this, monitor -> recommendedBatchSize);

        Gauge.builder("scheduler.recommended.concurrency")
                .description("Recommended concurrency level")
                .register(meterRegistry, this, monitor -> recommendedConcurrency);

        // Error metrics
        Gauge.builder("scheduler.errors.processing")
                .description("Processing errors count")
                .register(meterRegistry, this, monitor -> processingErrors.doubleValue());

        Gauge.builder("scheduler.errors.timeout")
                .description("Timeout errors count")
                .register(meterRegistry, this, monitor -> timeoutErrors.doubleValue());

        log.info("Scheduler performance monitoring initialized");
    }

    /**
     * Record batch processing metrics.
     */
    public void recordBatchProcessing(int jobsProcessed, double durationMs) {
        totalJobsProcessed.addAndGet(jobsProcessed);
        totalBatchesProcessed.incrementAndGet();
        totalProcessingTime.add(durationMs);
        totalBatchDuration.addAndGet((long) durationMs);

        // Update running averages
        updateBatchAverages(jobsProcessed, durationMs);

        // Record detailed metrics
        meterRegistry.counter("scheduler.batches.completed")
                .increment();

        Timer.builder("scheduler.batch.processing.time")
                .tag("jobs_processed", String.valueOf(jobsProcessed))
                .register(meterRegistry)
                .record((long) durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        log.debug("Recorded batch processing: {} jobs in {:.2f}ms", jobsProcessed, durationMs);
    }

    /**
     * Record individual job processing metrics.
     */
    public void recordJobProcessing(String jobType, boolean success, long durationMs) {
        JobTypeMetrics metrics = jobTypeMetrics.computeIfAbsent(jobType, 
                k -> new JobTypeMetrics());

        if (success) {
            metrics.successCount.increment();
        } else {
            metrics.errorCount.increment();
            processingErrors.increment();
        }

        metrics.totalDuration.add(durationMs);
        metrics.lastProcessed = LocalDateTime.now();

        // Record timer metrics
        Timer.builder("scheduler.job.processing.time")
                .tag("job_type", jobType)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        meterRegistry.counter("scheduler.jobs.completed", 
                "job_type", jobType, 
                "status", success ? "success" : "failure")
                .increment();
    }

    /**
     * Record Virtual Thread usage.
     */
    public void recordVirtualThreadUsage(long activeThreads) {
        activeVirtualThreads.set(activeThreads);
        
        if (activeThreads > peakVirtualThreads.get()) {
            peakVirtualThreads.set(activeThreads);
        }

        meterRegistry.gauge("scheduler.virtual.threads.current", activeThreads);
    }

    /**
     * Record error occurrence.
     */
    public void recordError(String errorType, String jobType, boolean retryable) {
        processingErrors.increment();

        if (retryable) {
            retryableErrors.increment();
        }

        if ("timeout".equals(errorType)) {
            timeoutErrors.increment();
        }

        meterRegistry.counter("scheduler.errors.total",
                "error_type", errorType,
                "job_type", jobType,
                "retryable", String.valueOf(retryable))
                .increment();

        log.warn("Recorded error: type={}, jobType={}, retryable={}", 
                errorType, jobType, retryable);
    }

    /**
     * Calculate and update performance metrics periodically.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void updatePerformanceMetrics() {
        try {
            // Calculate current throughput (jobs per minute)
            long currentJobs = totalJobsProcessed.get();
            long currentTime = System.currentTimeMillis();
            
            // Simple throughput calculation based on recent activity
            if (totalBatchesProcessed.get() > 0) {
                double avgBatchDurationMinutes = averageBatchDuration.doubleValue() / 60000.0;
                double avgJobsPerBatch = averageBatchSize.doubleValue();
                
                if (avgBatchDurationMinutes > 0) {
                    currentThroughput = avgJobsPerBatch / avgBatchDurationMinutes;
                }
            }

            // Update adaptive recommendations
            updateAdaptiveRecommendations();

            // Log performance summary
            logPerformanceSummary();

        } catch (Exception e) {
            log.error("Error updating performance metrics", e);
        }
    }

    /**
     * Update adaptive recommendations based on current performance.
     */
    private void updateAdaptiveRecommendations() {
        double efficiency = calculateProcessingEfficiency();
        double errorRate = calculateErrorRate();

        // Adjust batch size based on efficiency
        if (efficiency > 0.9 && errorRate < 0.05) {
            // High efficiency, low errors - increase batch size
            recommendedBatchSize = Math.min(recommendedBatchSize + 10, 500);
        } else if (efficiency < 0.6 || errorRate > 0.15) {
            // Low efficiency or high errors - decrease batch size
            recommendedBatchSize = Math.max(recommendedBatchSize - 10, 10);
        }

        // Adjust concurrency based on throughput
        if (currentThroughput < targetThroughput * 0.8) {
            // Below target, increase concurrency
            recommendedConcurrency = Math.min(recommendedConcurrency + 5, 200);
        } else if (currentThroughput > targetThroughput * 1.2) {
            // Above target, decrease concurrency to save resources
            recommendedConcurrency = Math.max(recommendedConcurrency - 5, 10);
        }

        // Record adaptive metrics
        meterRegistry.gauge("scheduler.efficiency.processing", efficiency);
        meterRegistry.gauge("scheduler.rate.error", errorRate);
    }

    /**
     * Calculate processing efficiency (successful jobs / total jobs).
     */
    private double calculateProcessingEfficiency() {
        long totalJobs = totalJobsProcessed.get();
        long totalErrors = processingErrors.longValue();
        
        if (totalJobs == 0) return 1.0;
        
        return (double) (totalJobs - totalErrors) / totalJobs;
    }

    /**
     * Calculate error rate.
     */
    private double calculateErrorRate() {
        long totalJobs = totalJobsProcessed.get();
        long totalErrors = processingErrors.longValue();
        
        if (totalJobs == 0) return 0.0;
        
        return (double) totalErrors / totalJobs;
    }

    /**
     * Update batch processing averages.
     */
    private void updateBatchAverages(int jobsProcessed, double durationMs) {
        // Simple moving average approximation
        long batchCount = totalBatchesProcessed.get();
        
        if (batchCount == 1) {
            averageBatchSize.reset();
            averageBatchSize.add(jobsProcessed);
            averageBatchDuration.reset();
            averageBatchDuration.add(durationMs);
        } else {
            // Weighted average with recent batches having more weight
            double weightedBatchSize = (averageBatchSize.doubleValue() * 0.8) + (jobsProcessed * 0.2);
            double weightedDuration = (averageBatchDuration.doubleValue() * 0.8) + (durationMs * 0.2);
            
            averageBatchSize.reset();
            averageBatchSize.add(weightedBatchSize);
            averageBatchDuration.reset();
            averageBatchDuration.add(weightedDuration);
        }
    }

    /**
     * Log comprehensive performance summary.
     */
    private void logPerformanceSummary() {
        log.info("=== Scheduler Performance Summary ===");
        log.info("Total Jobs Processed: {}", totalJobsProcessed.get());
        log.info("Total Batches Processed: {}", totalBatchesProcessed.get());
        log.info("Current Throughput: {:.2f} jobs/minute", currentThroughput);
        log.info("Average Batch Size: {:.1f}", averageBatchSize.doubleValue());
        log.info("Average Batch Duration: {:.1f}ms", averageBatchDuration.doubleValue());
        log.info("Processing Efficiency: {:.2f}%", calculateProcessingEfficiency() * 100);
        log.info("Error Rate: {:.2f}%", calculateErrorRate() * 100);
        log.info("Active Virtual Threads: {}", activeVirtualThreads.get());
        log.info("Peak Virtual Threads: {}", peakVirtualThreads.get());
        log.info("Recommended Batch Size: {}", recommendedBatchSize);
        log.info("Recommended Concurrency: {}", recommendedConcurrency);
        log.info("=====================================");
    }

    /**
     * Get performance recommendations.
     */
    public PerformanceRecommendations getRecommendations() {
        return new PerformanceRecommendations(
                recommendedBatchSize,
                recommendedConcurrency,
                calculateProcessingEfficiency(),
                calculateErrorRate(),
                currentThroughput
        );
    }

    /**
     * Get comprehensive performance report.
     */
    public PerformanceReport getPerformanceReport() {
        return new PerformanceReport(
                LocalDateTime.now(),
                totalJobsProcessed.get(),
                totalBatchesProcessed.get(),
                currentThroughput,
                averageBatchSize.doubleValue(),
                averageBatchDuration.doubleValue(),
                calculateProcessingEfficiency(),
                calculateErrorRate(),
                activeVirtualThreads.get(),
                peakVirtualThreads.get(),
                jobTypeMetrics.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> entry.getValue().toReport()
                        ))
        );
    }

    /**
     * Job type metrics tracking.
     */
    private static class JobTypeMetrics {
        final LongAdder successCount = new LongAdder();
        final LongAdder errorCount = new LongAdder();
        final DoubleAdder totalDuration = new DoubleAdder();
        volatile LocalDateTime lastProcessed = LocalDateTime.now();

        JobTypeReport toReport() {
            long total = successCount.longValue() + errorCount.longValue();
            double avgDuration = total > 0 ? totalDuration.doubleValue() / total : 0.0;
            double successRate = total > 0 ? (double) successCount.longValue() / total : 1.0;

            return new JobTypeReport(
                    successCount.longValue(),
                    errorCount.longValue(),
                    avgDuration,
                    successRate,
                    lastProcessed
            );
        }
    }

    /**
     * Performance recommendations record.
     */
    public record PerformanceRecommendations(
            int recommendedBatchSize,
            int recommendedConcurrency,
            double processingEfficiency,
            double errorRate,
            double currentThroughput
    ) {}

    /**
     * Comprehensive performance report.
     */
    public record PerformanceReport(
            LocalDateTime timestamp,
            long totalJobsProcessed,
            long totalBatchesProcessed,
            double currentThroughput,
            double averageBatchSize,
            double averageBatchDuration,
            double processingEfficiency,
            double errorRate,
            long activeVirtualThreads,
            long peakVirtualThreads,
            java.util.Map<String, JobTypeReport> jobTypeMetrics
    ) {}

    /**
     * Job type performance report.
     */
    public record JobTypeReport(
            long successCount,
            long errorCount,
            double averageDuration,
            double successRate,
            LocalDateTime lastProcessed
    ) {
        public long getTotalCount() {
            return successCount + errorCount;
        }
    }
}