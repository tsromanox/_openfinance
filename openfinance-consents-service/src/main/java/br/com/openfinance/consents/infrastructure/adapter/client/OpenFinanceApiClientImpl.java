package br.com.openfinance.consents.infrastructure.adapter.client;


import br.com.openfinance.core.client.BaseOpenFinanceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class OpenFinanceApiClientImpl extends BaseOpenFinanceClient implements OpenFinanceApiClient {

    public OpenFinanceApiClientImpl(WebClient.Builder webClientBuilder) {
        super(webClientBuilder);
    }

    @Override
    @CircuitBreaker(name = "openfinance-api", fallbackMethod = "getConsentFallback")
    @Retry(name = "openfinance-api")
    public Mono<ResponseConsentRead> getConsent(String organisationId, String consentId, String token) {
        String url = buildUrl(organisationId, "/consents/v3/consents/" + consentId);

        return webClient.get()
                .uri(url)
                .headers(h -> addCommonHeaders(h, token))
                .retrieve()
                .bodyToMono(ResponseConsentRead.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.debug("Retrieved consent {} from bank", consentId))
                .doOnError(error -> log.error("Error retrieving consent {} from bank", consentId, error));
    }

    @Override
    @CircuitBreaker(name = "openfinance-api", fallbackMethod = "createConsentFallback")
    @Retry(name = "openfinance-api")
    public Mono<ResponseConsent> createConsent(String organisationId, CreateConsent request, String token) {
        String url = buildUrl(organisationId, "/consents/v3/consents");

        return webClient.post()
                .uri(url)
                .headers(h -> addCommonHeaders(h, token))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ResponseConsent.class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(response -> log.info("Created consent at bank with id {}",
                        response.getData().getConsentId()))
                .doOnError(error -> log.error("Error creating consent at bank", error));
    }

    @Override
    @CircuitBreaker(name = "openfinance-api", fallbackMethod = "revokeConsentFallback")
    @Retry(name = "openfinance-api")
    public Mono<Void> revokeConsent(String organisationId, String consentId, String token) {
        String url = buildUrl(organisationId, "/consents/v3/consents/" + consentId);

        return webClient.delete()
                .uri(url)
                .headers(h -> addCommonHeaders(h, token))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(v -> log.info("Revoked consent {} at bank", consentId))
                .doOnError(error -> log.error("Error revoking consent {} at bank", consentId, error))
                .then();
    }

    @Override
    @CircuitBreaker(name = "openfinance-api", fallbackMethod = "extendConsentFallback")
    @Retry(name = "openfinance-api")
    public Mono<ResponseConsentExtensions> extendConsent(String organisationId, String consentId,
                                                         CreateConsentExtensions request, String token) {
        String url = buildUrl(organisationId, "/consents/v3/consents/" + consentId + "/extends");

        return webClient.post()
                .uri(url)
                .headers(h -> addCommonHeaders(h, token))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ResponseConsentExtensions.class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(response -> log.info("Extended consent {} at bank", consentId))
                .doOnError(error -> log.error("Error extending consent {} at bank", consentId, error));
    }

    @Override
    @CircuitBreaker(name = "openfinance-api", fallbackMethod = "getConsentExtensionsFallback")
    @Retry(name = "openfinance-api")
    public Mono<ResponseConsentReadExtensions> getConsentExtensions(String organisationId,
                                                                    String consentId, String token) {
        String url = buildUrl(organisationId, "/consents/v3/consents/" + consentId + "/extensions");

        return webClient.get()
                .uri(url)
                .headers(h -> addCommonHeaders(h, token))
                .retrieve()
                .bodyToMono(ResponseConsentReadExtensions.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.debug("Retrieved consent extensions for {}", consentId))
                .doOnError(error -> log.error("Error retrieving consent extensions for {}", consentId, error));
    }

    // Fallback methods
    public Mono<ResponseConsentRead> getConsentFallback(String organisationId, String consentId,
                                                        String token, Exception ex) {
        log.warn("Circuit breaker activated for getConsent. ConsentId: {}, Error: {}",
                consentId, ex.getMessage());
        return Mono.empty();
    }

    public Mono<ResponseConsent> createConsentFallback(String organisationId, CreateConsent request,
                                                       String token, Exception ex) {
        log.warn("Circuit breaker activated for createConsent. Error: {}", ex.getMessage());
        return Mono.error(new ServiceUnavailableException("Bank API is temporarily unavailable"));
    }

    public Mono<Void> revokeConsentFallback(String organisationId, String consentId,
                                            String token, Exception ex) {
        log.warn("Circuit breaker activated for revokeConsent. ConsentId: {}, Error: {}",
                consentId, ex.getMessage());
        return Mono.error(new ServiceUnavailableException("Unable to revoke consent at this time"));
    }

    public Mono<ResponseConsentExtensions> extendConsentFallback(String organisationId, String consentId,
                                                                 CreateConsentExtensions request,
                                                                 String token, Exception ex) {
        log.warn("Circuit breaker activated for extendConsent. ConsentId: {}, Error: {}",
                consentId, ex.getMessage());
        return Mono.error(new ServiceUnavailableException("Unable to extend consent at this time"));
    }

    public Mono<ResponseConsentReadExtensions> getConsentExtensionsFallback(String organisationId,
                                                                            String consentId,
                                                                            String token, Exception ex) {
        log.warn("Circuit breaker activated for getConsentExtensions. ConsentId: {}, Error: {}",
                consentId, ex.getMessage());
        return Mono.empty();
    }
}