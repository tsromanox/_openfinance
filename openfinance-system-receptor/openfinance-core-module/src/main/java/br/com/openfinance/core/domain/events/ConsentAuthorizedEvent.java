package br.com.openfinance.core.domain.events;

import lombok.Getter;
import java.util.Set;

@Getter
public class ConsentAuthorizedEvent extends DomainEvent {
    private final String consentId;
    private final String customerId;
    private final String participantId;
    private final Set<String> permissions;

    public ConsentAuthorizedEvent(String consentId, String customerId,
                                  String participantId, Set<String> permissions) {
        super(consentId);
        this.consentId = consentId;
        this.customerId = customerId;
        this.participantId = participantId;
        this.permissions = permissions;
    }
}
