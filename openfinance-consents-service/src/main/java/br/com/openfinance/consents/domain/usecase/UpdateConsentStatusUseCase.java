package br.com.openfinance.consents.domain.usecase;


import br.com.openfinance.consents.domain.entity.Consent;
import br.com.openfinance.consents.domain.entity.ConsentRejectionReason;
import br.com.openfinance.consents.domain.entity.ConsentStatus;
import br.com.openfinance.consents.domain.port.out.CacheService;
import br.com.openfinance.consents.domain.port.out.ConsentEventPublisher;
import br.com.openfinance.consents.domain.port.out.ConsentEventRepository;
import br.com.openfinance.consents.domain.port.out.ConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateConsentStatusUseCase {

    private final ConsentRepository consentRepository;
    private final ConsentEventRepository eventRepository;
    private final ConsentEventPublisher eventPublisher;
    private final CacheService cacheService;

    public Mono<Consent> execute(String consentId, ConsentStatus newStatus, ConsentRejectionReason reason) {
        return consentRepository.findById(consentId)
                .switchIfEmpty(Mono.error(new ConsentNotFoundException("Consent not found: " + consentId)))
                .flatMap(consent -> validateStatusTransition(consent, newStatus))
                .flatMap(consent -> updateConsentStatus(consent, newStatus, reason))
                .flatMap(consentRepository::save)
                .flatMap(this::evictCache)
                .flatMap(consent -> publishStatusChangeEvent(consent, newStatus))
                .doOnSuccess(consent -> log.info("Consent {} status updated to {}", consentId, newStatus))
                .doOnError(error -> log.error("Error updating consent status", error));
    }

    private Mono<Consent> validateStatusTransition(Consent consent, ConsentStatus newStatus) {
        return Mono.fromCallable(() -> {
            if (!isValidTransition(consent.getStatus(), newStatus)) {
                throw new InvalidStatusTransitionException(
                        String.format("Cannot transition from %s to %s", consent.getStatus(), newStatus)
                );
            }
            return consent;
        });
    }

    private boolean isValidTransition(ConsentStatus current, ConsentStatus next) {
        return switch (current) {
            case AWAITING_AUTHORISATION ->
                    next == ConsentStatus.AUTHORISED ||
                            next == ConsentStatus.REJECTED;
            case AUTHORISED ->
                    next == ConsentStatus.CONSUMED ||
                            next == ConsentStatus.REVOKED ||
                            next == ConsentStatus.EXPIRED;
            case CONSUMED ->
                    next == ConsentStatus.REVOKED;
            default -> false;
        };
    }

    private Mono<Consent> updateConsentStatus(Consent consent, ConsentStatus newStatus, ConsentRejectionReason reason) {
        consent.setStatus(newStatus);
        consent.setStatusUpdateDateTime(LocalDateTime.now());

        if (reason != null) {
            consent.setRejectionReason(reason);
        }

        return Mono.just(consent);
    }

    private Mono<Consent> evictCache(Consent consent) {
        return cacheService.evictConsent("consent:" + consent.getConsentId())
                .then(cacheService.evictAllConsentsForClient(consent.getClientId()))
                .thenReturn(consent);
    }

    private Mono<Consent> publishStatusChangeEvent(Consent consent, ConsentStatus previousStatus) {
        ConsentEvent event = ConsentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .consentId(consent.getConsentId())
                .type(getEventTypeForStatus(consent.getStatus()))
                .timestamp(LocalDateTime.now())
                .previousStatus(previousStatus)
                .newStatus(consent.getStatus())
                .build();

        return eventRepository.save(event)
                .then(eventPublisher.publishConsentEvent(event))
                .thenReturn(consent);
    }

    private ConsentEventType getEventTypeForStatus(ConsentStatus status) {
        return switch (status) {
            case AUTHORISED -> ConsentEventType.CONSENT_AUTHORISED;
            case REJECTED -> ConsentEventType.CONSENT_REJECTED;
            case REVOKED -> ConsentEventType.CONSENT_REVOKED;
            case CONSUMED -> ConsentEventType.CONSENT_CONSUMED;
            case EXPIRED -> ConsentEventType.CONSENT_EXPIRED;
            default -> ConsentEventType.CONSENT_UPDATED;
        };
    }
}
