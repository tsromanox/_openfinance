package br.com.openfinance.core.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateId;

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.aggregateId = aggregateId;
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getAggregateId() { return aggregateId; }
}
