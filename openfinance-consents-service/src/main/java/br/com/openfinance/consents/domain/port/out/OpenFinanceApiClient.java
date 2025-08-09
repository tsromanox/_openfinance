package br.com.openfinance.consents.domain.port.out;

import br.com.openfinance.consents.domain.entity.Consent;
import reactor.core.publisher.Mono;

public interface OpenFinanceApiClient {
    Mono<ConsentResponse> getConsent(String organisationId, String consentId, String token);
    Mono<ConsentResponse> createConsent(String organisationId, ConsentRequest request, String token);
    Mono<Void> revokeConsent(String organisationId, String consentId, String token);
}
