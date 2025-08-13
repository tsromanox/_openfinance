package br.com.openfinance.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event fired when account data is synchronized.
 */
public class AccountSyncedEvent implements DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredAt;
    private final UUID consentId;
    private final String accountId;
    private final String organizationId;
    private final boolean successful;
    private final String errorMessage;
    
    public AccountSyncedEvent(UUID consentId, String accountId, String organizationId, boolean successful, String errorMessage) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = LocalDateTime.now();
        this.consentId = consentId;
        this.accountId = accountId;
        this.organizationId = organizationId;
        this.successful = successful;
        this.errorMessage = errorMessage;
    }
    
    public static AccountSyncedEvent success(UUID consentId, String accountId, String organizationId) {
        return new AccountSyncedEvent(consentId, accountId, organizationId, true, null);
    }
    
    public static AccountSyncedEvent failure(UUID consentId, String accountId, String organizationId, String errorMessage) {
        return new AccountSyncedEvent(consentId, accountId, organizationId, false, errorMessage);
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
        return "AccountSynced";
    }
    
    @Override
    public UUID getAggregateId() {
        return consentId;
    }
    
    public UUID getConsentId() {
        return consentId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}