package br.com.openfinance.consents.application.scheduler;


import br.com.openfinance.consents.domain.port.out.ConsentRepository;
import br.com.openfinance.consents.domain.usecase.ProcessExpiredConsentsUseCase;
import br.com.openfinance.consents.domain.usecase.SyncConsentStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentScheduler {

    private final ConsentRepository consentRepository;
    private final SyncConsentStatusUseCase syncConsentStatusUseCase;
    private final ProcessExpiredConsentsUseCase processExpiredConsentsUseCase;
    private final KafkaProducerService kafkaProducerService;

    // Sync consent statuses every 30 minutes
    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncConsentStatuses() {
        log.info("Starting scheduled consent status synchronization");

        consentRepository
                .findByStatus(ConsentStatus.AWAITING_AUTHORISATION)
                .parallel(10)
                .runOn(Schedulers.boundedElastic())
                .flatMap(consent -> syncConsentStatusUseCase.execute(consent.getConsentId())
                        .onErrorResume(error -> {
                            log.error("Error syncing consent {}: {}", consent.getConsentId(), error.getMessage());
                            return Mono.empty();
                        }))
                .sequential()
                .collectList()
                .subscribe(
                        results -> log.info("Consent sync completed. Processed {} consents", results.size()),
                        error -> log.error("Error in consent sync scheduler", error)
                );
    }

    // Process expired consents every hour
    @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void processExpiredConsents() {
        log.info("Starting expired consents processing");

        processExpiredConsentsUseCase.execute()
                .subscribe(
                        result -> log.info("Expired consents processing completed: {}", result),
                        error -> log.error("Error processing expired consents", error)
                );
    }

    // Queue active consents for data collection every 12 hours
    @Scheduled(cron = "0 0 */12 * * *")
    public void queueConsentsForDataCollection() {
        log.info("Starting to queue consents for data collection");

        consentRepository
                .findByStatus(ConsentStatus.AUTHORISED)
                .flatMap(consent -> {
                    ConsentProcessingTask task = ConsentProcessingTask.builder()
                            .consentId(consent.getConsentId())
                            .clientId(consent.getClientId())
                            .organisationId(consent.getOrganisationId())
                            .permissions(consent.getPermissions())
                            .linkedAccountIds(consent.getLinkedAccountIds())
                            .build();

                    return kafkaProducerService.sendConsentTask(task);
                })
                .collectList()
                .subscribe(
                        tasks -> log.info("Queued {} consents for data collection", tasks.size()),
                        error -> log.error("Error queuing consents for data collection", error)
                );
    }
}
