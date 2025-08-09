package br.com.openfinance.consents.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {
    private String consentId;
    private String clientId;
    private String organisationId;
    private ConsentStatus status;
    private LocalDateTime creationDateTime;
    private LocalDateTime statusUpdateDateTime;
    private LocalDateTime expirationDateTime;
    private List<ConsentPermission> permissions;
    private String loggedUserId;
    private String businessEntityId;
    private Set<String> linkedAccountIds;
    private Set<String> linkedCreditCardAccountIds;
    private ConsentRejectionReason rejectionReason;
    private LocalDateTime transactionFromDateTime;
    private LocalDateTime transactionToDateTime;

    // Business rules
    public boolean isActive() {
        return status == ConsentStatus.AUTHORISED &&
                (expirationDateTime == null || expirationDateTime.isAfter(LocalDateTime.now()));
    }

    public boolean isExpired() {
        return expirationDateTime != null && expirationDateTime.isBefore(LocalDateTime.now());
    }

    public boolean canAccessAccount(String accountId) {
        return isActive() && linkedAccountIds.contains(accountId);
    }

    public boolean hasPermission(PermissionType permissionType) {
        return permissions.stream()
                .anyMatch(p -> p.getType() == permissionType);
    }


}
