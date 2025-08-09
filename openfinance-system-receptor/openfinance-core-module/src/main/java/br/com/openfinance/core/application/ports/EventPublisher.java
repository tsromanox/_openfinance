package br.com.openfinance.core.application.ports;

import br.com.openfinance.core.domain.events.DomainEvent;

public interface EventPublisher {
    void publish(DomainEvent event);
    void publishAsync(DomainEvent event);
}
