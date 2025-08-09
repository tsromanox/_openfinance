package br.com.openfinance.adapter.outbound.client;

import br.com.openfinance.application.port.out.OpenFinanceApiClient;
import br.com.openfinance.client.api.accounts.AccountsApi;
import br.com.openfinance.client.api.consents.ConsentsApi;
import br.com.openfinance.client.api.resources.ResourcesApi;
import br.com.openfinance.domain.account.Account;
import br.com.openfinance.domain.consent.Consent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
public class OpenFinanceApiClientImpl implements OpenFinanceApiClient {
    private static final Logger LOGGER = Logger.getLogger(OpenFinanceApiClientImpl.class.getName());

    private final RestClient restClient;
    private final ConsentsApi consentsApi;
    private final AccountsApi accountsApi;
    private final ResourcesApi resourcesApi;

    @Autowired
    public OpenFinanceApiClientImpl(
            RestClient.Builder restClientBuilder,
            ConsentsApi consentsApi,
            AccountsApi accountsApi,
            ResourcesApi resourcesApi) {
        this.restClient = restClientBuilder.build();
        this.consentsApi = consentsApi;
        this.accountsApi = accountsApi;
        this.resourcesApi = resourcesApi;
    }

    @Override
    @CircuitBreaker(name = "openFinanceApi", fallbackMethod = "createConsentFallback")
    @Retry(name = "openFinanceApi")
    public Consent createConsent(String organizationId, ConsentRequest request) {
        LOGGER.info("Creating consent for organization: " + organizationId);

        var createConsent = new br.com.openfinance.client.model.consents.CreateConsent();
        // Map request to API model

        var response = consentsApi.consentsPostConsents(
                organizationId,
                UUID.randomUUID().toString(),
                createConsent
        );

        return mapToDomainConsent(response);
    }

    @Override
    @CircuitBreaker(name = "openFinanceApi", fallbackMethod = "getConsentFallback")
    @Retry(name = "openFinanceApi")
    public Consent getConsent(String organizationId, String consentId, String accessToken) {
        LOGGER.info("Getting consent: " + consentId);

        var response = consentsApi.consentsGetConsentsConsentId(
                consentId,
                "Bearer " + accessToken,
                UUID.randomUUID().toString()
        );

        return mapToDomainConsent(response);
    }

    @Override
    @CircuitBreaker(name = "openFinanceApi", fallbackMethod = "getAccountsFallback")
    @Retry(name = "openFinanceApi")
    public List<Account> getAccounts(String organizationId, String accessToken) {
        LOGGER.info("Getting accounts for organization: " + organizationId);

        var response = accountsApi.accountsGetAccounts(
                "Bearer " + accessToken,
                UUID.randomUUID().toString(),
                null, null, null, null, null
        );

        return response.getData().stream()
                .map(this::mapToDomainAccount)
                .toList();
    }

    // Fallback methods
    public Consent createConsentFallback(String organizationId, ConsentRequest request, Exception ex) {
        LOGGER.severe("Circuit breaker triggered for createConsent: " + ex.getMessage());
        throw new ExternalServiceUnavailableException("Consent service temporarily unavailable", ex);
    }

    public Consent getConsentFallback(String organizationId, String consentId, String accessToken, Exception ex) {
        LOGGER.severe("Circuit breaker triggered for getConsent: " + ex.getMessage());
        throw new ExternalServiceUnavailableException("Consent service temporarily unavailable", ex);
    }

    public List<Account> getAccountsFallback(String organizationId, String accessToken, Exception ex) {
        LOGGER.severe("Circuit breaker triggered for getAccounts: " + ex.getMessage());
        return List.of(); // Return empty list or cached data
    }

    // Mapping methods
    private Consent mapToDomainConsent(Object apiResponse) {
        // Implementation of mapping logic
        return null;
    }

    private Account mapToDomainAccount(Object apiAccount) {
        // Implementation of mapping logic
        return null;
    }

    public static class ExternalServiceUnavailableException extends RuntimeException {
        public ExternalServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
