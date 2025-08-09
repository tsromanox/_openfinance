package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.ConsentEvent;
import reactor.core.publisher.Mono;

public interface ConsentEventPublisher {
    Mono<Void> publishConsentEvent(ConsentEvent event);
    Mono<Void> publishConsentUpdate(String consentId, ConsentUpdateEvent updateEvent);
}
