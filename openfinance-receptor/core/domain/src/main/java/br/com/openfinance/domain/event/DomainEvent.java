package br.com.openfinance.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base interface for all domain events.
 */
public interface DomainEvent {
    
    /**
     * Unique identifier for this event.
     */
    UUID getEventId();
    
    /**
     * Timestamp when the event occurred.
     */
    LocalDateTime getOccurredAt();
    
    /**
     * Type of the event.
     */
    String getEventType();
    
    /**
     * Aggregate root ID that generated this event.
     */
    UUID getAggregateId();
}