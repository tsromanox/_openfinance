package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.Consent;
import br.com.openfinance.consents.domain.entity.ConsentStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ConsentRepository {
    Mono<Consent> save(Consent consent);
    Mono<Consent> findById(String consentId);
    Mono<Consent> findByIdAndClientId(String consentId, String clientId);
    Flux<Consent> findByClientId(String clientId);
    Flux<Consent> findByStatus(ConsentStatus status);
    Flux<Consent> findByStatusAndExpirationDateTimeBefore(ConsentStatus status, LocalDateTime dateTime);
    Flux<Consent> findActiveConsentsForClient(String clientId);
    Mono<Long> countByClientIdAndStatus(String clientId, ConsentStatus status);
    Mono<Void> deleteById(String consentId);
    Flux<Consent> findConsentsToProcess(int limit);
}
