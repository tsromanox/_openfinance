package br.com.openfinance.domain.event;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Domain event fired when a new consent is created.
 */
public class ConsentCreatedEvent implements DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final UUID consentId;
    private final String organizationId;
    private final String customerId;
    private final Set<String> permissions;
    
    public ConsentCreatedEvent(UUID consentId, String organizationId, String customerId, Set<String> permissions) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.consentId = consentId;
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.permissions = Set.copyOf(permissions);
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
        return "ConsentCreated";
    }
    
    @Override
    public UUID getAggregateId() {
        return consentId;
    }
    
    public UUID getConsentId() {
        return consentId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
}