package br.com.openfinance.consents.domain.services;

import br.com.openfinance.consents.domain.model.Consent;
import br.com.openfinance.consents.domain.model.ConsentStatus;
import br.com.openfinance.core.domain.events.ConsentAuthorizedEvent;
import br.com.openfinance.core.domain.events.ConsentRevokedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsentDomainService {

    private final EventPublisher eventPublisher;

    public void authorizeConsent(Consent consent) {
        if (consent.getStatus() != ConsentStatus.AWAITING_AUTHORISATION) {
            throw new IllegalStateException(
                    "Consent must be in AWAITING_AUTHORISATION status");
        }

        consent.setStatus(ConsentStatus.AUTHORISED);
        consent.setStatusUpdateDateTime(LocalDateTime.now());

        // Publish domain event
        eventPublisher.publish(new ConsentAuthorizedEvent(
                consent.getConsentId().toString(),
                consent.getCustomerId(),
                consent.getParticipantId(),
                consent.getPermissions().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        ));
    }

    public void revokeConsent(Consent consent) {
        if (consent.getStatus() != ConsentStatus.AUTHORISED) {
            throw new IllegalStateException("Only AUTHORISED consents can be revoked");
        }

        consent.revoke();

        // Publish domain event
        eventPublisher.publish(new ConsentRevokedEvent(
                consent.getConsentId().toString(),
                consent.getCustomerId()
        ));
    }

    public boolean isConsentValidForOperation(Consent consent, String permission) {
        return consent.isValid() &&
                consent.getPermissions().stream()
                        .anyMatch(p -> p.name().equals(permission));
    }
}
