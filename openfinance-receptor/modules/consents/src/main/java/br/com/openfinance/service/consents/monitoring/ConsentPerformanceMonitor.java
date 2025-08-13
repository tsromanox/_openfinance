package br.com.openfinance.service.consents.monitoring;

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
 * Advanced performance monitoring specifically for consent operations.
 * Tracks consent processing efficiency, validation performance, and provides adaptive feedback.
 */
@Slf4j
@Component
public class ConsentPerformanceMonitor {

    private final MeterRegistry meterRegistry;
    
    // Consent-specific metrics
    private final AtomicLong totalConsentsProcessed = new AtomicLong(0);
    private final AtomicLong totalConsentsCreated = new AtomicLong(0);
    private final AtomicLong totalConsentsRevoked = new AtomicLong(0);
    private final AtomicLong totalConsentsValidated = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    
    // Performance metrics
    private final DoubleAdder totalProcessingTime = new DoubleAdder();
    private final DoubleAdder averageValidationTime = new DoubleAdder();
    private final DoubleAdder averageBatchSize = new DoubleAdder();
    private final DoubleAdder averageBatchDuration = new DoubleAdder();
    
    // Virtual Thread metrics
    private final AtomicLong activeVirtualThreads = new AtomicLong(0);
    private final AtomicLong peakVirtualThreads = new AtomicLong(0);
    private final AtomicLong concurrentApiCalls = new AtomicLong(0);
    private final AtomicLong peakConcurrentApiCalls = new AtomicLong(0);
    
    // Error tracking
    private final LongAdder validationErrors = new LongAdder();
    private final LongAdder processingErrors = new LongAdder();
    private final LongAdder apiTimeouts = new LongAdder();
    private final LongAdder creationErrors = new LongAdder();
    
    // Consent status tracking
    private final ConcurrentMap<String, ConsentStatusMetrics> statusMetrics = new ConcurrentHashMap<>();
    
    // Performance tracking by operation type
    private final ConcurrentMap<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    
    // Adaptive performance feedback
    private volatile double currentThroughput = 0.0;
    private volatile double targetThroughput = 2000.0; // consents per minute
    private volatile int recommendedBatchSize = 200;
    private volatile int recommendedConcurrency = 100;
    private volatile double validationSuccessRate = 1.0;

    public ConsentPerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        // Consent operation metrics
        Gauge.builder("consent.processed.total")
                .description("Total number of consents processed")
                .register(meterRegistry, this, monitor -> totalConsentsProcessed.get());

        Gauge.builder("consent.created.total")
                .description("Total number of consents created")
                .register(meterRegistry, this, monitor -> totalConsentsCreated.get());

        Gauge.builder("consent.revoked.total")
                .description("Total number of consents revoked")
                .register(meterRegistry, this, monitor -> totalConsentsRevoked.get());

        Gauge.builder("consent.validated.total")
                .description("Total number of consent validations performed")
                .register(meterRegistry, this, monitor -> totalConsentsValidated.get());

        // Batch processing metrics
        Gauge.builder("consent.batches.processed.total")
                .description("Total number of consent batches processed")
                .register(meterRegistry, this, monitor -> totalBatchesProcessed.get());

        Gauge.builder("consent.batch.size.average")
                .description("Average consent batch size")
                .register(meterRegistry, this, monitor -> averageBatchSize.doubleValue());

        Gauge.builder("consent.batch.duration.average")
                .description("Average consent batch duration in milliseconds")
                .register(meterRegistry, this, monitor -> averageBatchDuration.doubleValue());

        // Virtual Thread metrics
        Gauge.builder("consent.virtual.threads.active")
                .description("Current number of active virtual threads for consent processing")
                .register(meterRegistry, this, monitor -> activeVirtualThreads.get());

        Gauge.builder("consent.virtual.threads.peak")
                .description("Peak number of virtual threads for consent processing")
                .register(meterRegistry, this, monitor -> peakVirtualThreads.get());

        Gauge.builder("consent.api.calls.concurrent")
                .description("Current number of concurrent API calls")
                .register(meterRegistry, this, monitor -> concurrentApiCalls.get());

        Gauge.builder("consent.api.calls.peak")
                .description("Peak number of concurrent API calls")
                .register(meterRegistry, this, monitor -> peakConcurrentApiCalls.get());

        // Performance metrics
        Gauge.builder("consent.throughput.current")
                .description("Current consent processing throughput (consents/minute)")
                .register(meterRegistry, this, monitor -> currentThroughput);

        Gauge.builder("consent.validation.success.rate")
                .description("Consent validation success rate")
                .register(meterRegistry, this, monitor -> validationSuccessRate);

        // Adaptive recommendations
        Gauge.builder("consent.recommended.batch.size")
                .description("Recommended batch size based on performance")
                .register(meterRegistry, this, monitor -> recommendedBatchSize);

        Gauge.builder("consent.recommended.concurrency")
                .description("Recommended concurrency level for consent processing")
                .register(meterRegistry, this, monitor -> recommendedConcurrency);

        // Error metrics
        Gauge.builder("consent.errors.validation")
                .description("Total validation errors")
                .register(meterRegistry, this, monitor -> validationErrors.doubleValue());

        Gauge.builder("consent.errors.processing")
                .description("Total processing errors")
                .register(meterRegistry, this, monitor -> processingErrors.doubleValue());

        Gauge.builder("consent.errors.api.timeout")
                .description("Total API timeout errors")
                .register(meterRegistry, this, monitor -> apiTimeouts.doubleValue());

        log.info("Consent performance monitoring initialized");
    }

    /**
     * Record consent operation metrics.
     */
    public void recordConsentOperation(String operationType, boolean success, long durationMs) {
        OperationMetrics metrics = operationMetrics.computeIfAbsent(operationType, 
                k -> new OperationMetrics());

        if (success) {
            metrics.successCount.increment();
            
            // Update specific counters
            switch (operationType.toUpperCase()) {
                case "CREATE" -> totalConsentsCreated.incrementAndGet();
                case "PROCESS" -> totalConsentsProcessed.incrementAndGet();
                case "REVOKE" -> totalConsentsRevoked.incrementAndGet();
                case "VALIDATE" -> totalConsentsValidated.incrementAndGet();
            }
        } else {
            metrics.errorCount.increment();
            
            // Update error counters
            switch (operationType.toUpperCase()) {
                case "CREATE" -> creationErrors.increment();
                case "PROCESS" -> processingErrors.increment();
                case "VALIDATE" -> validationErrors.increment();
            }
        }

        metrics.totalDuration.add(durationMs);
        metrics.lastProcessed = LocalDateTime.now();

        // Record timer metrics
        Timer.builder("consent.operation.duration")
                .tag("operation", operationType.toLowerCase())
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Update counters
        meterRegistry.counter("consent.operations.total", 
                "operation", operationType.toLowerCase(),
                "status", success ? "success" : "failure")
                .increment();

        log.debug("Recorded consent operation: {} - {} in {}ms", 
                operationType, success ? "SUCCESS" : "FAILURE", durationMs);
    }

    /**
     * Record batch processing metrics.
     */
    public void recordBatchProcessing(int consentsProcessed, long durationMs) {
        totalBatchesProcessed.incrementAndGet();

        // Update running averages
        updateBatchAverages(consentsProcessed, durationMs);

        // Record detailed metrics
        meterRegistry.counter("consent.batches.completed")
                .increment();

        Timer.builder("consent.batch.processing.time")
                .tag("batch_size", String.valueOf(consentsProcessed))
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        log.debug("Recorded consent batch processing: {} consents in {:.2f}ms", consentsProcessed, durationMs);
    }

    /**
     * Record Virtual Thread usage for consent operations.
     */
    public void recordVirtualThreadUsage(long activeThreads) {
        activeVirtualThreads.set(activeThreads);
        
        if (activeThreads > peakVirtualThreads.get()) {
            peakVirtualThreads.set(activeThreads);
        }

        meterRegistry.gauge("consent.virtual.threads.current", activeThreads);
    }

    /**
     * Record API call concurrency.
     */
    public void recordApiCallStart() {
        long current = concurrentApiCalls.incrementAndGet();
        if (current > peakConcurrentApiCalls.get()) {
            peakConcurrentApiCalls.set(current);
        }
    }

    public void recordApiCallEnd() {
        concurrentApiCalls.decrementAndGet();
    }

    /**
     * Record consent status change.
     */
    public void recordConsentStatusChange(String fromStatus, String toStatus) {
        ConsentStatusMetrics fromMetrics = statusMetrics.computeIfAbsent(fromStatus, 
                k -> new ConsentStatusMetrics());
        ConsentStatusMetrics toMetrics = statusMetrics.computeIfAbsent(toStatus, 
                k -> new ConsentStatusMetrics());

        fromMetrics.transitionsOut.increment();
        toMetrics.transitionsIn.increment();
        toMetrics.currentCount.increment();

        if (fromMetrics.currentCount.get() > 0) {
            fromMetrics.currentCount.decrement();
        }

        meterRegistry.counter("consent.status.transitions",
                "from_status", fromStatus.toLowerCase(),
                "to_status", toStatus.toLowerCase())
                .increment();

        log.debug("Recorded consent status change: {} -> {}", fromStatus, toStatus);
    }

    /**
     * Record error occurrence.
     */
    public void recordError(String errorType, String operationType, boolean retryable) {
        switch (errorType.toLowerCase()) {
            case "validation_error" -> validationErrors.increment();
            case "processing_error" -> processingErrors.increment();
            case "timeout" -> apiTimeouts.increment();
            case "creation_error" -> creationErrors.increment();
            default -> log.warn("Unknown error type: {}", errorType);
        }

        meterRegistry.counter("consent.errors.total",
                "error_type", errorType.toLowerCase(),
                "operation_type", operationType.toLowerCase(),
                "retryable", String.valueOf(retryable))
                .increment();

        log.warn("Recorded consent error: type={}, operation={}, retryable={}", 
                errorType, operationType, retryable);
    }

    /**
     * Calculate and update performance metrics periodically.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void updatePerformanceMetrics() {
        try {
            // Calculate current throughput (consents per minute)
            if (totalBatchesProcessed.get() > 0) {
                double avgBatchDurationMinutes = averageBatchDuration.doubleValue() / 60000.0;
                double avgConsentsPerBatch = averageBatchSize.doubleValue();
                
                if (avgBatchDurationMinutes > 0) {
                    currentThroughput = avgConsentsPerBatch / avgBatchDurationMinutes;
                }
            }

            // Calculate validation success rate
            long totalValidations = totalConsentsValidated.get();
            if (totalValidations > 0) {
                long validationErrorCount = validationErrors.longValue();
                validationSuccessRate = (double) (totalValidations - validationErrorCount) / totalValidations;
            }

            // Update adaptive recommendations
            updateAdaptiveRecommendations();

            // Log performance summary
            logPerformanceSummary();

        } catch (Exception e) {
            log.error("Error updating consent performance metrics", e);
        }
    }

    /**
     * Update adaptive recommendations based on current performance.
     */
    private void updateAdaptiveRecommendations() {
        double processingEfficiency = calculateProcessingEfficiency();
        double errorRate = calculateErrorRate();

        // Adjust batch size based on efficiency and error rate
        if (processingEfficiency > 0.95 && errorRate < 0.02) {
            // High efficiency, low errors - increase batch size
            recommendedBatchSize = Math.min(recommendedBatchSize + 20, 500);
        } else if (processingEfficiency < 0.8 || errorRate > 0.1) {
            // Low efficiency or high errors - decrease batch size
            recommendedBatchSize = Math.max(recommendedBatchSize - 20, 50);
        }

        // Adjust concurrency based on throughput and API performance
        if (currentThroughput < targetThroughput * 0.8 && apiTimeouts.longValue() < totalConsentsProcessed.get() * 0.05) {
            // Below target and low timeouts - increase concurrency
            recommendedConcurrency = Math.min(recommendedConcurrency + 10, 500);
        } else if (currentThroughput > targetThroughput * 1.2 || apiTimeouts.longValue() > totalConsentsProcessed.get() * 0.15) {
            // Above target or high timeouts - decrease concurrency
            recommendedConcurrency = Math.max(recommendedConcurrency - 10, 20);
        }

        // Record adaptive metrics
        meterRegistry.gauge("consent.efficiency.processing", processingEfficiency);
        meterRegistry.gauge("consent.rate.error", errorRate);
    }

    /**
     * Calculate processing efficiency (successful operations / total operations).
     */
    private double calculateProcessingEfficiency() {
        long totalOperations = operationMetrics.values().stream()
                .mapToLong(metrics -> metrics.successCount.longValue() + metrics.errorCount.longValue())
                .sum();
        
        long successfulOperations = operationMetrics.values().stream()
                .mapToLong(metrics -> metrics.successCount.longValue())
                .sum();
        
        if (totalOperations == 0) return 1.0;
        
        return (double) successfulOperations / totalOperations;
    }

    /**
     * Calculate overall error rate.
     */
    private double calculateErrorRate() {
        long totalErrors = processingErrors.longValue() + validationErrors.longValue() + 
                          apiTimeouts.longValue() + creationErrors.longValue();
        
        long totalOperations = totalConsentsProcessed.get() + totalConsentsCreated.get() + 
                              totalConsentsRevoked.get() + totalConsentsValidated.get();
        
        if (totalOperations == 0) return 0.0;
        
        return (double) totalErrors / totalOperations;
    }

    /**
     * Update batch processing averages.
     */
    private void updateBatchAverages(int consentsProcessed, double durationMs) {
        long batchCount = totalBatchesProcessed.get();
        
        if (batchCount == 1) {
            averageBatchSize.reset();
            averageBatchSize.add(consentsProcessed);
            averageBatchDuration.reset();
            averageBatchDuration.add(durationMs);
        } else {
            // Weighted average with recent batches having more weight
            double weightedBatchSize = (averageBatchSize.doubleValue() * 0.8) + (consentsProcessed * 0.2);
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
        log.info("=== Consent Performance Summary ===");
        log.info("Total Consents Created: {}", totalConsentsCreated.get());
        log.info("Total Consents Processed: {}", totalConsentsProcessed.get());
        log.info("Total Consents Validated: {}", totalConsentsValidated.get());
        log.info("Total Consents Revoked: {}", totalConsentsRevoked.get());
        log.info("Current Throughput: {:.2f} consents/minute", currentThroughput);
        log.info("Average Batch Size: {:.1f}", averageBatchSize.doubleValue());
        log.info("Average Batch Duration: {:.1f}ms", averageBatchDuration.doubleValue());
        log.info("Processing Efficiency: {:.2f}%", calculateProcessingEfficiency() * 100);
        log.info("Error Rate: {:.2f}%", calculateErrorRate() * 100);
        log.info("Validation Success Rate: {:.2f}%", validationSuccessRate * 100);
        log.info("Active Virtual Threads: {}", activeVirtualThreads.get());
        log.info("Peak Virtual Threads: {}", peakVirtualThreads.get());
        log.info("Concurrent API Calls: {}", concurrentApiCalls.get());
        log.info("Peak API Calls: {}", peakConcurrentApiCalls.get());
        log.info("Recommended Batch Size: {}", recommendedBatchSize);
        log.info("Recommended Concurrency: {}", recommendedConcurrency);
        log.info("=====================================");
    }

    /**
     * Get performance recommendations.
     */
    public ConsentPerformanceRecommendations getRecommendations() {
        return new ConsentPerformanceRecommendations(
                recommendedBatchSize,
                recommendedConcurrency,
                calculateProcessingEfficiency(),
                calculateErrorRate(),
                validationSuccessRate,
                currentThroughput
        );
    }

    /**
     * Get comprehensive performance report.
     */
    public ConsentPerformanceReport getPerformanceReport() {
        return new ConsentPerformanceReport(
                LocalDateTime.now(),
                totalConsentsCreated.get(),
                totalConsentsProcessed.get(),
                totalConsentsValidated.get(),
                totalConsentsRevoked.get(),
                totalBatchesProcessed.get(),
                currentThroughput,
                averageBatchSize.doubleValue(),
                averageBatchDuration.doubleValue(),
                calculateProcessingEfficiency(),
                calculateErrorRate(),
                validationSuccessRate,
                activeVirtualThreads.get(),
                peakVirtualThreads.get(),
                concurrentApiCalls.get(),
                peakConcurrentApiCalls.get(),
                operationMetrics.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> entry.getValue().toReport()
                        )),
                statusMetrics.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> entry.getValue().toReport()
                        ))
        );
    }

    // Supporting classes
    private static class OperationMetrics {
        final LongAdder successCount = new LongAdder();
        final LongAdder errorCount = new LongAdder();
        final DoubleAdder totalDuration = new DoubleAdder();
        volatile LocalDateTime lastProcessed = LocalDateTime.now();

        OperationReport toReport() {
            long total = successCount.longValue() + errorCount.longValue();
            double avgDuration = total > 0 ? totalDuration.doubleValue() / total : 0.0;
            double successRate = total > 0 ? (double) successCount.longValue() / total : 1.0;

            return new OperationReport(
                    successCount.longValue(),
                    errorCount.longValue(),
                    avgDuration,
                    successRate,
                    lastProcessed
            );
        }
    }

    private static class ConsentStatusMetrics {
        final LongAdder currentCount = new LongAdder();
        final LongAdder transitionsIn = new LongAdder();
        final LongAdder transitionsOut = new LongAdder();

        ConsentStatusReport toReport() {
            return new ConsentStatusReport(
                    currentCount.longValue(),
                    transitionsIn.longValue(),
                    transitionsOut.longValue()
            );
        }
    }

    // Report records
    public record ConsentPerformanceRecommendations(
            int recommendedBatchSize,
            int recommendedConcurrency,
            double processingEfficiency,
            double errorRate,
            double validationSuccessRate,
            double currentThroughput
    ) {}

    public record ConsentPerformanceReport(
            LocalDateTime timestamp,
            long totalConsentsCreated,
            long totalConsentsProcessed,
            long totalConsentsValidated,
            long totalConsentsRevoked,
            long totalBatchesProcessed,
            double currentThroughput,
            double averageBatchSize,
            double averageBatchDuration,
            double processingEfficiency,
            double errorRate,
            double validationSuccessRate,
            long activeVirtualThreads,
            long peakVirtualThreads,
            long concurrentApiCalls,
            long peakConcurrentApiCalls,
            java.util.Map<String, OperationReport> operationMetrics,
            java.util.Map<String, ConsentStatusReport> statusMetrics
    ) {}

    public record OperationReport(
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

    public record ConsentStatusReport(
            long currentCount,
            long transitionsIn,
            long transitionsOut
    ) {}
}