package br.com.openfinance.consents.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentQueueProcessor {

    private final ConsentQueueRepository queueRepository;
    private final TransmitterClient transmitterClient;
    private final ConsentApplicationService consentService;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(10);

    @Scheduled(fixedDelay = 5000) // Process every 5 seconds
    @Transactional
    public void processQueue() {
        List<ConsentQueueEntity> pendingItems = queueRepository
                .findPendingConsents(100); // Batch size of 100

        if (pendingItems.isEmpty()) {
            return;
        }

        log.info("Processing {} consent queue items", pendingItems.size());

        List<CompletableFuture<Void>> futures = pendingItems.stream()
                .map(item -> CompletableFuture.runAsync(
                        () -> processQueueItem(item), executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
    }

    private void processQueueItem(ConsentQueueEntity item) {
        try {
            item.setStatus(ConsentQueueEntity.QueueStatus.PROCESSING);
            queueRepository.save(item);

            // Fetch consent from transmitter
            ConsentData consentData = transmitterClient
                    .fetchConsent(item.getParticipantId(), item.getConsentId());

            // Process and save consent
            consentService.processConsent(consentData);

            // Mark as completed
            item.setStatus(ConsentQueueEntity.QueueStatus.COMPLETED);
            item.setProcessedAt(LocalDateTime.now());
            queueRepository.save(item);

            log.info("Successfully processed consent: {}", item.getConsentId());

        } catch (Exception e) {
            handleProcessingError(item, e);
        }
    }

    private void handleProcessingError(ConsentQueueEntity item, Exception e) {
        log.error("Error processing consent {}: {}",
                item.getConsentId(), e.getMessage());

        item.setRetryCount(item.getRetryCount() + 1);
        item.setErrorMessage(e.getMessage());

        if (item.getRetryCount() < 3) {
            item.setStatus(ConsentQueueEntity.QueueStatus.RETRYING);
            item.setNextRetryAt(LocalDateTime.now()
                    .plusMinutes(Math.pow(2, item.getRetryCount())));
        } else {
            item.setStatus(ConsentQueueEntity.QueueStatus.FAILED);
        }

        queueRepository.save(item);
    }
}
