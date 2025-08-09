package br.com.openfinance.consents.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {
    private UUID consentId;
    private String customerId;
    private String participantId;
    private ConsentStatus status;
    private Set<ConsentPermission> permissions;
    private LocalDateTime creationDateTime;
    private LocalDateTime expirationDateTime;
    private LocalDateTime statusUpdateDateTime;
    private String transmitterConsentId;

    public boolean isValid() {
        return status == ConsentStatus.AUTHORISED
                && LocalDateTime.now().isBefore(expirationDateTime);
    }

    public void revoke() {
        this.status = ConsentStatus.REJECTED;
        this.statusUpdateDateTime = LocalDateTime.now();
    }
}
