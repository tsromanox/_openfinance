package br.com.openfinance.infrastructure.scheduler;

import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.infrastructure.scheduler.monitoring.SchedulerPerformanceMonitor;
import br.com.openfinance.infrastructure.scheduler.service.AdaptiveResourceManager;
import br.com.openfinance.infrastructure.scheduler.service.VirtualThreadProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced Processing Worker leveraging Virtual Threads, Structured Concurrency,
 * and adaptive resource management for maximum performance.
 */
@Slf4j
@Component
public class ProcessingWorker {

    private final VirtualThreadProcessingService processingService;
    private final AdaptiveResourceManager resourceManager;
    private final SchedulerPerformanceMonitor performanceMonitor;
    private final TaskExecutor virtualThreadExecutor;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile CompletableFuture<Integer> currentProcessingTask;

    @Value("${openfinance.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${openfinance.scheduler.startup-delay:10000}")
    private long startupDelay;

    public ProcessingWorker(
            VirtualThreadProcessingService processingService,
            AdaptiveResourceManager resourceManager,
            SchedulerPerformanceMonitor performanceMonitor,
            @Qualifier("virtualThreadTaskExecutor") TaskExecutor virtualThreadExecutor) {
        
        this.processingService = processingService;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
        this.virtualThreadExecutor = virtualThreadExecutor;
        
        log.info("ProcessingWorker initialized with Virtual Thread support and adaptive management");
    }

    /**
     * Start the enhanced processing worker with adaptive scheduling.
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<Void> startProcessing() {
        if (!schedulerEnabled) {
            log.info("Scheduler is disabled via configuration");
            return CompletableFuture.completedFuture(null);
        }

        log.info("Starting enhanced ProcessingWorker with startup delay of {}ms", startupDelay);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Initial startup delay
                Thread.sleep(startupDelay);
                log.info("ProcessingWorker startup delay completed, beginning processing");

                while (running.get()) {
                    try {
                        // Check if we should process based on adaptive parameters
                        if (shouldProcessNow()) {
                            processWithAdaptiveManagement();
                        }

                        // Adaptive processing interval
                        long processingInterval = resourceManager.getDynamicProcessingInterval();
                        Thread.sleep(processingInterval);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("ProcessingWorker interrupted, shutting down gracefully");
                        break;
                    } catch (Exception e) {
                        log.error("Unexpected error in ProcessingWorker main loop", e);
                        performanceMonitor.recordError("worker_error", "main_loop", false);
                        
                        // Brief pause before retrying
                        Thread.sleep(5000);
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("ProcessingWorker main thread interrupted");
            } catch (Exception e) {
                log.error("Fatal error in ProcessingWorker", e);
            } finally {
                log.info("ProcessingWorker stopped");
            }
        }, virtualThreadExecutor);
    }

    /**
     * Scheduled processing trigger using Spring's scheduling framework.
     * This provides a backup mechanism in addition to the continuous processing loop.
     */
    @Scheduled(fixedDelayString = "${openfinance.scheduler.backup.interval:60000}")
    public void scheduledProcessingTrigger() {
        if (!schedulerEnabled || !running.get()) {
            return;
        }

        try {
            log.debug("Scheduled processing trigger activated");
            
            // Only trigger if no current processing is active
            if (currentProcessingTask == null || currentProcessingTask.isDone()) {
                triggerBatchProcessing();
            } else {
                log.debug("Skipping scheduled trigger - processing already active");
            }
            
        } catch (Exception e) {
            log.error("Error in scheduled processing trigger", e);
            performanceMonitor.recordError("scheduled_trigger_error", "scheduler", false);
        }
    }

    /**
     * Process with adaptive resource management and performance monitoring.
     */
    private void processWithAdaptiveManagement() {
        if (!resourceManager.acquireResources()) {
            log.debug("Unable to acquire resources for processing, skipping this cycle");
            return;
        }

        try {
            triggerBatchProcessing();
        } finally {
            resourceManager.releaseResources();
        }
    }

    /**
     * Trigger batch processing using the Virtual Thread processing service.
     */
    private void triggerBatchProcessing() {
        try {
            // Start batch processing and store the future for tracking
            currentProcessingTask = processingService.processPendingJobsWithVirtualThreads();
            
            // Handle completion asynchronously
            currentProcessingTask.whenComplete((processedCount, throwable) -> {
                if (throwable != null) {
                    log.error("Batch processing failed", throwable);
                    performanceMonitor.recordError("batch_processing_error", "virtual_threads", true);
                } else {
                    log.debug("Batch processing completed successfully. Processed {} jobs", processedCount);
                }
            });
            
        } catch (Exception e) {
            log.error("Error triggering batch processing", e);
            performanceMonitor.recordError("trigger_error", "batch_processing", true);
        }
    }

    /**
     * Determine if processing should occur now based on adaptive parameters.
     */
    private boolean shouldProcessNow() {
        // Check system resources
        var resourceUtilization = resourceManager.getResourceUtilization();
        
        if (resourceUtilization.isUnderPressure()) {
            log.debug("System under pressure, skipping processing cycle");
            return false;
        }

        // Check if we have available processing capacity
        if (resourceUtilization.activeTasks() >= resourceUtilization.maxConcurrency()) {
            log.debug("Maximum concurrency reached, skipping processing cycle");
            return false;
        }

        // Check if previous batch is still running
        if (currentProcessingTask != null && !currentProcessingTask.isDone()) {
            log.debug("Previous batch processing still active, skipping processing cycle");
            return false;
        }

        return true;
    }

    /**
     * Gracefully stop the processing worker.
     */
    public void stop() {
        log.info("Stopping ProcessingWorker...");
        running.set(false);
        
        // Wait for current processing to complete
        if (currentProcessingTask != null && !currentProcessingTask.isDone()) {
            try {
                log.info("Waiting for current batch processing to complete...");
                currentProcessingTask.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(30), 
                        java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("Timeout waiting for batch processing to complete", e);
            }
        }
        
        log.info("ProcessingWorker stopped successfully");
    }

    /**
     * Check if the worker is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Check if batch processing is currently active.
     */
    public boolean isProcessingActive() {
        return currentProcessingTask != null && !currentProcessingTask.isDone();
    }

    /**
     * Get current worker status.
     */
    public WorkerStatus getStatus() {
        var resourceUtilization = resourceManager.getResourceUtilization();
        var processingStats = processingService.getProcessingStatistics();
        
        return new WorkerStatus(
                running.get(),
                isProcessingActive(),
                resourceUtilization.activeTasks(),
                resourceUtilization.maxConcurrency(),
                processingStats.pendingJobs(),
                processingStats.processingJobs(),
                processingStats.completedJobs(),
                processingStats.failedJobs(),
                resourceUtilization.isUnderPressure()
        );
    }

    /**
     * Worker status record for monitoring.
     */
    public record WorkerStatus(
            boolean isRunning,
            boolean isProcessingActive,
            int activeTasks,
            int maxConcurrency,
            long pendingJobs,
            long processingJobs,
            long completedJobs,
            long failedJobs,
            boolean isSystemUnderPressure
    ) {
        public double getConcurrencyUtilization() {
            return maxConcurrency > 0 ? (double) activeTasks / maxConcurrency * 100 : 0.0;
        }
    }
}
