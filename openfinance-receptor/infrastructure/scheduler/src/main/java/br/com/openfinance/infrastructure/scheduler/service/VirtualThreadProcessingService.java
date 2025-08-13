package br.com.openfinance.infrastructure.scheduler.service;

import br.com.openfinance.application.port.input.AccountUseCase;
import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.scheduler.monitoring.SchedulerPerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Advanced processing service using Virtual Threads and Structured Concurrency
 * for maximum performance with minimum resource consumption.
 */
@Slf4j
@Service
public class VirtualThreadProcessingService {

    private final ProcessingQueueRepository queueRepository;
    private final ConsentUseCase consentUseCase;
    private final AccountUseCase accountUseCase;
    private final TaskExecutor virtualThreadExecutor;
    private final TaskExecutor structuredConcurrencyExecutor;
    private final MeterRegistry meterRegistry;
    private final SchedulerPerformanceMonitor performanceMonitor;

    // Configuration
    @Value("${openfinance.scheduler.batch.size:100}")
    private int batchSize;

    @Value("${openfinance.scheduler.batch.max-concurrent:50}")
    private int maxConcurrentBatches;

    @Value("${openfinance.scheduler.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${openfinance.scheduler.timeout.task:30s}")
    private Duration taskTimeout;

    @Value("${openfinance.scheduler.timeout.batch:300s}")
    private Duration batchTimeout;

    // Performance tracking
    private final Semaphore batchSemaphore;

    public VirtualThreadProcessingService(
            ProcessingQueueRepository queueRepository,
            ConsentUseCase consentUseCase,
            AccountUseCase accountUseCase,
            @Qualifier("virtualThreadTaskExecutor") TaskExecutor virtualThreadExecutor,
            @Qualifier("structuredConcurrencyExecutor") TaskExecutor structuredConcurrencyExecutor,
            MeterRegistry meterRegistry,
            SchedulerPerformanceMonitor performanceMonitor) {
        
        this.queueRepository = queueRepository;
        this.consentUseCase = consentUseCase;
        this.accountUseCase = accountUseCase;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.structuredConcurrencyExecutor = structuredConcurrencyExecutor;
        this.meterRegistry = meterRegistry;
        this.performanceMonitor = performanceMonitor;
        this.batchSemaphore = new Semaphore(maxConcurrentBatches);

        log.info("VirtualThreadProcessingService initialized with batch size: {}, max concurrent: {}", 
                batchSize, maxConcurrentBatches);
    }

    /**
     * Process pending jobs using Virtual Threads with adaptive batch sizing.
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<Integer> processPendingJobsWithVirtualThreads() {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting Virtual Thread batch processing");
                
                int totalProcessed = 0;
                boolean hasMoreJobs = true;
                
                while (hasMoreJobs) {
                    List<ProcessingJob> batch = queueRepository.fetchNextBatch(batchSize);
                    
                    if (batch.isEmpty()) {
                        hasMoreJobs = false;
                        break;
                    }
                    
                    int processed = processJobBatchWithStructuredConcurrency(batch);
                    totalProcessed += processed;
                    
                    // Adaptive batch sizing based on performance
                    adaptiveBatchSizing(processed, batch.size());
                    
                    // Prevent overwhelming the system
                    if (!batchSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        log.warn("Rate limiting batch processing due to high load");
                        break;
                    }
                    batchSemaphore.release();
                }
                
                log.info("Virtual Thread batch processing completed. Total processed: {}", totalProcessed);
                performanceMonitor.recordBatchProcessing(totalProcessed, sample.stop(
                    Timer.builder("scheduler.batch.processing.duration")
                            .tag("strategy", "virtual-threads")
                            .register(meterRegistry)
                ));
                
                return totalProcessed;
                
            } catch (Exception e) {
                log.error("Error in Virtual Thread batch processing", e);
                meterRegistry.counter("scheduler.batch.errors", "strategy", "virtual-threads")
                        .increment();
                throw new RuntimeException(e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Process a batch of jobs using Structured Concurrency for optimal resource management.
     */
    private int processJobBatchWithStructuredConcurrency(List<ProcessingJob> jobs) {
        if (jobs.isEmpty()) return 0;
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        log.debug("Processing batch of {} jobs with Structured Concurrency", jobs.size());
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Submit all jobs for concurrent processing
            List<StructuredTaskScope.Subtask<ProcessingResult>> subtasks = new ArrayList<>();
            
            for (ProcessingJob job : jobs) {
                var subtask = scope.fork(() -> processIndividualJob(job));
                subtasks.add(subtask);
            }
            
            // Wait for all tasks to complete or any to fail
            scope.join();
            scope.throwIfFailed();
            
            // Collect results and update job statuses
            int successCount = 0;
            int failureCount = 0;
            
            for (var subtask : subtasks) {
                ProcessingResult result = subtask.get();
                
                if (result.success()) {
                    queueRepository.updateStatus(result.jobId(), JobStatus.COMPLETED);
                    successCount++;
                } else {
                    handleJobFailure(result);
                    failureCount++;
                }
            }
            
            log.debug("Batch processing completed: {} successful, {} failed", successCount, failureCount);
            
            sample.stop(Timer.builder("scheduler.batch.structured.concurrency.duration")
                    .tag("batch_size", String.valueOf(jobs.size()))
                    .register(meterRegistry));
            
            // Record metrics
            meterRegistry.counter("scheduler.jobs.processed", "status", "success")
                    .increment(successCount);
            meterRegistry.counter("scheduler.jobs.processed", "status", "failure")
                    .increment(failureCount);
            
            return successCount;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Structured Concurrency batch processing interrupted", e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Structured Concurrency batch processing failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Process an individual job with timeout and error handling.
     */
    private ProcessingResult processIndividualJob(ProcessingJob job) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            log.trace("Processing job: {}", job.getId());
            
            // Process different job types
            switch (job.getJobType()) {
                case CONSENT_PROCESSING:
                    consentUseCase.processConsent(job.getConsentId());
                    break;
                    
                case ACCOUNT_SYNC:
                    if (job.getOrganizationId() != null) {
                        accountUseCase.syncAccountsForOrganization(job.getOrganizationId());
                    }
                    break;
                    
                case ACCOUNT_BALANCE_UPDATE:
                    if (job.getAccountId() != null) {
                        accountUseCase.updateAccountBalance(job.getAccountId());
                    }
                    break;
                    
                default:
                    log.warn("Unknown job type: {}", job.getJobType());
                    return new ProcessingResult(job.getId(), false, "Unknown job type", null);
            }
            
            long duration = sample.stop(Timer.builder("scheduler.job.processing.duration")
                    .tag("job_type", job.getJobType().name())
                    .tag("status", "success")
                    .register(meterRegistry));
            
            log.trace("Job {} processed successfully in {}ms", job.getId(), duration);
            return new ProcessingResult(job.getId(), true, null, null);
            
        } catch (Exception e) {
            sample.stop(Timer.builder("scheduler.job.processing.duration")
                    .tag("job_type", job.getJobType().name())
                    .tag("status", "error")
                    .register(meterRegistry));
            
            log.error("Error processing job {}: {}", job.getId(), e.getMessage(), e);
            return new ProcessingResult(job.getId(), false, e.getMessage(), e);
        }
    }

    /**
     * Handle job failure with retry logic.
     */
    private void handleJobFailure(ProcessingResult result) {
        try {
            ProcessingJob job = queueRepository.findById(result.jobId()).orElse(null);
            if (job == null) {
                log.error("Job not found for failure handling: {}", result.jobId());
                return;
            }
            
            int currentRetryCount = job.getRetryCount();
            
            if (currentRetryCount < maxRetryAttempts) {
                // Retry the job
                ProcessingJob retryJob = job.toBuilder()
                        .retryCount(currentRetryCount + 1)
                        .status(JobStatus.PENDING)
                        .lastError(result.errorMessage())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                queueRepository.save(retryJob);
                log.info("Job {} scheduled for retry ({}/{})", job.getId(), 
                        currentRetryCount + 1, maxRetryAttempts);
                
            } else {
                // Max retries reached, mark as failed
                ProcessingJob failedJob = job.toBuilder()
                        .status(JobStatus.FAILED)
                        .lastError(result.errorMessage())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                queueRepository.save(failedJob);
                log.error("Job {} failed permanently after {} retries: {}", 
                        job.getId(), maxRetryAttempts, result.errorMessage());
                
                meterRegistry.counter("scheduler.jobs.permanently.failed")
                        .increment();
            }
            
        } catch (Exception e) {
            log.error("Error handling job failure for job {}: {}", result.jobId(), e.getMessage(), e);
        }
    }

    /**
     * Adaptive batch sizing based on processing performance.
     */
    private void adaptiveBatchSizing(int processed, int batchSize) {
        double efficiency = (double) processed / batchSize;
        
        if (efficiency > 0.9 && this.batchSize < 500) {
            // High efficiency, increase batch size
            this.batchSize = Math.min(this.batchSize + 10, 500);
            log.debug("Increased batch size to {}", this.batchSize);
            
        } else if (efficiency < 0.6 && this.batchSize > 10) {
            // Low efficiency, decrease batch size
            this.batchSize = Math.max(this.batchSize - 10, 10);
            log.debug("Decreased batch size to {}", this.batchSize);
        }
        
        // Record efficiency metrics
        meterRegistry.gauge("scheduler.batch.efficiency", efficiency);
        meterRegistry.gauge("scheduler.batch.size.current", this.batchSize);
    }

    /**
     * Process specific job types with Virtual Thread optimization.
     */
    @Async("virtualThreadTaskExecutor")
    public CompletableFuture<Void> processConsentJobs(List<UUID> consentIds) {
        return CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                List<StructuredTaskScope.Subtask<Void>> subtasks = consentIds.stream()
                        .map(consentId -> scope.fork(() -> {
                            consentUseCase.processConsent(consentId);
                            return null;
                        }))
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                log.info("Processed {} consent jobs successfully", consentIds.size());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Get current processing statistics.
     */
    public ProcessingStatistics getProcessingStatistics() {
        long pendingJobs = queueRepository.countByStatus(JobStatus.PENDING);
        long processingJobs = queueRepository.countByStatus(JobStatus.PROCESSING);
        long completedJobs = queueRepository.countByStatus(JobStatus.COMPLETED);
        long failedJobs = queueRepository.countByStatus(JobStatus.FAILED);
        
        return new ProcessingStatistics(
                pendingJobs,
                processingJobs,
                completedJobs,
                failedJobs,
                batchSemaphore.availablePermits(),
                batchSize
        );
    }

    /**
     * Processing result record.
     */
    private record ProcessingResult(
            Long jobId,
            boolean success,
            String errorMessage,
            Throwable exception
    ) {}

    /**
     * Processing statistics record.
     */
    public record ProcessingStatistics(
            long pendingJobs,
            long processingJobs,
            long completedJobs,
            long failedJobs,
            int availableBatchSlots,
            int currentBatchSize
    ) {}

    /**
     * Job type enumeration for different processing strategies.
     */
    public enum JobType {
        CONSENT_PROCESSING,
        ACCOUNT_SYNC,
        ACCOUNT_BALANCE_UPDATE,
        CUSTOM_PROCESSING
    }
}