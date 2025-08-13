package br.com.openfinance.domain.event;

import br.com.openfinance.domain.consent.ConsentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event fired when a consent status changes.
 */
public class ConsentStatusChangedEvent implements DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final UUID consentId;
    private final ConsentStatus previousStatus;
    private final ConsentStatus newStatus;
    
    public ConsentStatusChangedEvent(UUID consentId, ConsentStatus previousStatus, ConsentStatus newStatus) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.consentId = consentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "ConsentStatusChanged";
    }
    
    @Override
    public UUID getAggregateId() {
        return consentId;
    }
    
    public UUID getConsentId() {
        return consentId;
    }
    
    public ConsentStatus getPreviousStatus() {
        return previousStatus;
    }
    
    public ConsentStatus getNewStatus() {
        return newStatus;
    }
    
    public boolean wasAuthorized() {
        return newStatus == ConsentStatus.AUTHORISED;
    }
    
    public boolean wasRevoked() {
        return newStatus == ConsentStatus.REJECTED;
    }
    
    public boolean wasExpired() {
        return newStatus == ConsentStatus.EXPIRED;
    }
}