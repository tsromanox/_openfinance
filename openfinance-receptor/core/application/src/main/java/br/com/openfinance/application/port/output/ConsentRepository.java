package br.com.openfinance.application.port.output;

import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository {
    Consent save(Consent consent);
    Optional<Consent> findById(UUID id);
    Optional<Consent> findByConsentId(String consentId);
    void updateStatus(UUID id, ConsentStatus status);
    List<Consent> findActiveConsents();
}
