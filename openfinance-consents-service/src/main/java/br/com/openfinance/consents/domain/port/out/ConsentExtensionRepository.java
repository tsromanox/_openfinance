package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.ConsentExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConsentExtensionRepository {
    Mono<ConsentExtension> save(ConsentExtension extension);
    Flux<ConsentExtension> findByConsentId(String consentId);
    Mono<ConsentExtension> findById(String id);
}