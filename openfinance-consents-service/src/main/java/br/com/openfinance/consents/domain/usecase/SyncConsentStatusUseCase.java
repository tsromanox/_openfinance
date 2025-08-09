package br.com.openfinance.consents.domain.usecase;

import br.com.openfinance.consents.domain.entity.Consent;
import br.com.openfinance.consents.domain.entity.ConsentRejectionReason;
import br.com.openfinance.consents.domain.entity.ConsentStatus;
import br.com.openfinance.consents.domain.entity.RejectionCode;
import br.com.openfinance.consents.domain.port.out.CacheService;
import br.com.openfinance.consents.domain.port.out.ConsentRepository;
import br.com.openfinance.consents.domain.port.out.OpenFinanceApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncConsentStatusUseCase {

    private final ConsentRepository consentRepository;
    private final OpenFinanceApiClient apiClient;
    private final UpdateConsentStatusUseCase updateStatusUseCase;
    private final CacheService cacheService;
    private final AuthTokenProvider tokenProvider;

    public Mono<Consent> execute(String consentId) {
        return consentRepository.findById(consentId)
                .switchIfEmpty(Mono.error(new ConsentNotFoundException("Consent not found: " + consentId)))
                .flatMap(this::checkIfSyncNeeded)
                .flatMap(this::fetchConsentFromBank)
                .flatMap(this::updateConsentIfChanged)
                .flatMap(this::updateCache)
                .doOnSuccess(consent -> log.info("Consent {} synchronized successfully", consentId))
                .doOnError(error -> log.error("Error synchronizing consent {}", consentId, error));
    }

    private Mono<Consent> checkIfSyncNeeded(Consent consent) {
        // Don't sync if consent is in final state
        if (consent.getStatus() == ConsentStatus.REJECTED ||
                consent.getStatus() == ConsentStatus.REVOKED ||
                consent.getStatus() == ConsentStatus.EXPIRED) {
            log.debug("Consent {} is in final state, no sync needed", consent.getConsentId());
            return Mono.just(consent);
        }

        // Check if recently updated (within last 5 minutes)
        if (consent.getStatusUpdateDateTime().isAfter(LocalDateTime.now().minusMinutes(5))) {
            log.debug("Consent {} was recently updated, skipping sync", consent.getConsentId());
            return Mono.just(consent);
        }

        return Mono.just(consent);
    }

    private Mono<ConsentWithResponse> fetchConsentFromBank(Consent consent) {
        return tokenProvider.getToken(consent.getClientId(), consent.getOrganisationId())
                .flatMap(token -> apiClient.getConsent(
                        consent.getOrganisationId(),
                        consent.getConsentId(),
                        token
                ))
                .map(response -> new ConsentWithResponse(consent, response))
                .onErrorResume(error -> {
                    log.warn("Failed to fetch consent {} from bank: {}", consent.getConsentId(), error.getMessage());
                    return Mono.just(new ConsentWithResponse(consent, null));
                });
    }

    private Mono<Consent> updateConsentIfChanged(ConsentWithResponse consentWithResponse) {
        Consent consent = consentWithResponse.consent();
        ConsentResponse response = consentWithResponse.response();

        if (response == null) {
            return Mono.just(consent);
        }

        ConsentStatus newStatus = ConsentStatus.valueOf(response.getData().getStatus());

        if (consent.getStatus() != newStatus) {
            log.info("Consent {} status changed from {} to {}",
                    consent.getConsentId(), consent.getStatus(), newStatus);

            ConsentRejectionReason reason = null;
            if (response.getData().getRejection() != null) {
                reason = ConsentRejectionReason.builder()
                        .code(RejectionCode.valueOf(response.getData().getRejection().getRejectedBy()))
                        .additionalInformation(response.getData().getRejection().getReason())
                        .build();
            }

            return updateStatusUseCase.execute(consent.getConsentId(), newStatus, reason);
        }

        // Update linked accounts if changed
        if (response.getData().getLinkedAccountIds() != null) {
            consent.setLinkedAccountIds(response.getData().getLinkedAccountIds());
            return consentRepository.save(consent);
        }

        return Mono.just(consent);
    }

    private Mono<Consent> updateCache(Consent consent) {
        if (consent.isActive()) {
            return cacheService.putConsent(
                    "consent:" + consent.getConsentId(),
                    consent,
                    Duration.ofHours(1)
            ).thenReturn(consent);
        }
        return Mono.just(consent);
    }

    private record ConsentWithResponse(Consent consent, ConsentResponse response) {}
}
