package br.com.openfinance.adapter.inbound.scheduler;

import br.com.openfinance.application.port.out.ConsentRepository;
import br.com.openfinance.application.port.out.ProcessingQueueRepository;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.processing.ConsentProcessingJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/scheduler")
public class BatchSchedulerController {
    private static final Logger LOGGER = Logger.getLogger(BatchSchedulerController.class.getName());

    private final ConsentRepository consentRepository;
    private final ProcessingQueueRepository queueRepository;

    public BatchSchedulerController(
            ConsentRepository consentRepository,
            ProcessingQueueRepository queueRepository) {
        this.consentRepository = consentRepository;
        this.queueRepository = queueRepository;
    }

    @PostMapping("/jobs/start")
    public ResponseEntity<BatchJobResponse> startBatchJob() {
        LOGGER.info("Starting batch job processing");

        var processingRunId = generateProcessingRunId();
        var jobsCreated = createProcessingJobs(processingRunId);

        return ResponseEntity.ok(new BatchJobResponse(
                processingRunId,
                jobsCreated,
                LocalDateTime.now()
        ));
    }

    private int createProcessingJobs(String processingRunId) {
        // Query all active consents and create jobs
        var activeConsents = consentRepository.findActiveConsents();

        activeConsents.forEach(consent -> {
            var job = ConsentProcessingJob.builder()
                    .consentId(consent.getInternalId())
                    .customerId(consent.getCustomerId())
                    .organizationId(consent.getOrganizationId())
                    .status(ConsentProcessingJob.JobStatus.PENDING)
                    .maxRetries(3)
                    .processingRunId(processingRunId)
                    .createdAt(LocalDateTime.now())
                    .build();

            queueRepository.save(job);
        });

        return activeConsents.size();
    }

    private String generateProcessingRunId() {
        return LocalDateTime.now().toString() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    record BatchJobResponse(
            String processingRunId,
            int jobsCreated,
            LocalDateTime startedAt
    ) {}
}
