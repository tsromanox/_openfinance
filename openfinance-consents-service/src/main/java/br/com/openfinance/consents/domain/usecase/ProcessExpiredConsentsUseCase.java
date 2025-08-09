package br.com.openfinance.consents.domain.usecase;

import br.com.openfinance.consents.domain.entity.*;
import br.com.openfinance.consents.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessExpiredConsentsUseCase {

    private final ConsentRepository consentRepository;
    private final UpdateConsentStatusUseCase updateStatusUseCase;

    public Mono<ProcessingResult> execute() {
        LocalDateTime now = LocalDateTime.now();

        return consentRepository
                .findByStatusAndExpirationDateTimeBefore(ConsentStatus.AUTHORISED, now)
                .flatMap(this::expireConsent)
                .collectList()
                .map(results -> ProcessingResult.builder()
                        .totalProcessed(results.size())
                        .successful(results.stream().filter(r -> r).count())
                        .failed(results.stream().filter(r -> !r).count())
                        .build())
                .doOnSuccess(result -> log.info("Processed {} expired consents", result.getTotalProcessed()));
    }

    private Mono<Boolean> expireConsent(Consent consent) {
        ConsentRejectionReason reason = ConsentRejectionReason.builder()
                .code(RejectionCode.CONSENT_EXPIRED)
                .additionalInformation("Consent expired at " + consent.getExpirationDateTime())
                .build();

        return updateStatusUseCase.execute(consent.getConsentId(), ConsentStatus.EXPIRED, reason)
                .map(c -> true)
                .onErrorResume(error -> {
                    log.error("Failed to expire consent {}: {}", consent.getConsentId(), error.getMessage());
                    return Mono.just(false);
                });
    }
}
