package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.Consent;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CacheService {
    Mono<Void> putConsent(String key, Consent consent, Duration ttl);
    Mono<Consent> getConsent(String key);
    Mono<Boolean> evictConsent(String key);
    Mono<Boolean> evictAllConsentsForClient(String clientId);
}
