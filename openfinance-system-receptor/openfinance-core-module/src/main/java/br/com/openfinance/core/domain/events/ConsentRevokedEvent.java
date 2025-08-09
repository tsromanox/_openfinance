package br.com.openfinance.core.domain.events;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ConsentRevokedEvent extends DomainEvent {
    private final String consentId;
    private final String customerId;
    private final LocalDateTime revokedAt;
    private final String reason;

    public ConsentRevokedEvent(String consentId, String customerId) {
        this(consentId, customerId, "USER_REQUESTED");
    }

    public ConsentRevokedEvent(String consentId, String customerId, String reason) {
        super(consentId);
        this.consentId = consentId;
        this.customerId = customerId;
        this.revokedAt = LocalDateTime.now();
        this.reason = reason;
    }
}
