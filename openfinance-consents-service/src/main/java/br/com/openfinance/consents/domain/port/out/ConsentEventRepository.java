package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.ConsentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ConsentEventRepository {
    Mono<ConsentEvent> save(ConsentEvent event);
    Flux<ConsentEvent> findByConsentId(String consentId);
    Flux<ConsentEvent> findByConsentIdAndTimestampBetween(
            String consentId,
            LocalDateTime start,
            LocalDateTime end
    );
}
