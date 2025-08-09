package br.com.openfinance.adapter.inbound.scheduler;

import br.com.openfinance.application.port.in.ProcessConsentUseCase;
import br.com.openfinance.application.port.out.ProcessingQueueRepository;
import br.com.openfinance.domain.processing.ConsentProcessingJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public class ConsentProcessingWorker {
    private static final Logger LOGGER = Logger.getLogger(ConsentProcessingWorker.class.getName());

    private final ProcessingQueueRepository queueRepository;
    private final ProcessConsentUseCase processConsentUseCase;
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Value("${openfinance.worker.batch-size:100}")
    private int batchSize;

    @Value("${openfinance.worker.poll-interval-ms:5000}")
    private long pollIntervalMs;

    public ConsentProcessingWorker(
            ProcessingQueueRepository queueRepository,
            ProcessConsentUseCase processConsentUseCase) {
        this.queueRepository = queueRepository;
        this.processConsentUseCase = processConsentUseCase;
    }

    @Async
    public CompletableFuture<Void> startProcessing() {
        LOGGER.info("Starting consent processing worker");

        while (running.get()) {
            try {
                processNextBatch();
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Worker interrupted");
                break;
            } catch (Exception e) {
                LOGGER.severe("Error in processing loop: " + e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private void processNextBatch() {
        queueRepository.fetchNextBatch(batchSize)
                .ifPresent(jobs -> {
                    LOGGER.info("Processing batch of " + jobs.size() + " jobs");

                    // Process jobs using virtual threads
                    jobs.parallelStream().forEach(this::processJob);
                });
    }

    private void processJob(ConsentProcessingJob job) {
        try {
            LOGGER.info("Processing job: " + job.getJobId());

            processConsentUseCase.processConsent(job.getConsentId());

            queueRepository.updateStatus(job.getJobId(), ConsentProcessingJob.JobStatus.COMPLETED);

        } catch (Exception e) {
            handleJobFailure(job, e);
        }
    }

    private void handleJobFailure(ConsentProcessingJob job, Exception e) {
        LOGGER.severe("Job failed: " + job.getJobId() + " - " + e.getMessage());

        queueRepository.incrementRetryCount(job.getJobId());

        if (job.getRetryCount() >= job.getMaxRetries()) {
            queueRepository.moveToDeadLetter(job.getJobId(), e.getMessage());
        } else {
            queueRepository.updateStatus(job.getJobId(), ConsentProcessingJob.JobStatus.PENDING);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutting down consent processing worker");
        running.set(false);
    }
}
