package br.com.openfinance.application.port.input;

import br.com.openfinance.domain.consent.Consent;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface ConsentUseCase {
    Consent createConsent(CreateConsentCommand command);
    Consent getConsent(UUID consentId);
    void processConsent(UUID consentId);
    void revokeConsent(UUID consentId);

    record CreateConsentCommand(
            String organizationId,
            String customerId,
            Set<String> permissions,
            LocalDateTime expirationDateTime
    ) {}
}
