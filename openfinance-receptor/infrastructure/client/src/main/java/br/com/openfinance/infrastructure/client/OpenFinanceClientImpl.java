package br.com.openfinance.infrastructure.client;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * High-performance OpenFinance client implementation with parallel processing optimizations.
 */
@Slf4j
@Component
public class OpenFinanceClientImpl implements OpenFinanceClient, ReactiveOpenFinanceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;
    private final MeterRegistry meterRegistry;
    
    // Metrics timers
    private final Timer createConsentTimer;
    private final Timer getConsentTimer;
    private final Timer getAccountsTimer;
    private final Timer getBalanceTimer;

    public OpenFinanceClientImpl(
            @Qualifier("openFinanceWebClient") WebClient webClient,
            CircuitBreaker circuitBreaker,
            Retry retry,
            RateLimiter rateLimiter,
            Bulkhead bulkhead,
            MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.createConsentTimer = Timer.builder("openfinance.client.create_consent")
                .description("Time taken to create consent")
                .register(meterRegistry);
        this.getConsentTimer = Timer.builder("openfinance.client.get_consent")
                .description("Time taken to get consent")
                .register(meterRegistry);
        this.getAccountsTimer = Timer.builder("openfinance.client.get_accounts")
                .description("Time taken to get accounts")
                .register(meterRegistry);
        this.getBalanceTimer = Timer.builder("openfinance.client.get_balance")
                .description("Time taken to get balance")
                .register(meterRegistry);
    }

    // Blocking interface implementation for backward compatibility
    @Override
    public ConsentResponse createConsent(String orgId, ConsentRequest request) {
        return createConsentReactive(orgId, request)
                .block(Duration.ofSeconds(30));
    }

    @Override
    public ConsentResponse getConsent(String orgId, String consentId) {
        return getConsentReactive(orgId, consentId)
                .block(Duration.ofSeconds(30));
    }

    @Override
    public AccountsResponse getAccounts(String orgId, String token) {
        return getAccountsReactive(orgId, token)
                .block(Duration.ofSeconds(30));
    }

    @Override
    public BalanceResponse getBalance(String orgId, String accountId, String token) {
        return getBalanceReactive(orgId, accountId, token)
                .block(Duration.ofSeconds(30));
    }

    // Reactive implementation with parallel processing optimizations
    @Override
    public Mono<ConsentResponse> createConsent(String orgId, ConsentRequest request) {
        return createConsentReactive(orgId, request);
    }

    @Override
    public Mono<ConsentResponse> getConsent(String orgId, String consentId) {
        return getConsentReactive(orgId, consentId);
    }

    @Override
    public Mono<AccountsResponse> getAccounts(String orgId, String token) {
        return getAccountsReactive(orgId, token);
    }

    @Override
    public Mono<BalanceResponse> getBalance(String orgId, String accountId, String token) {
        return getBalanceReactive(orgId, accountId, token);
    }

    @Override
    public Flux<BalanceResponse> getBalances(String orgId, List<String> accountIds, String token) {
        return Flux.fromIterable(accountIds)
                .parallel(Math.min(accountIds.size(), 10)) // Limit concurrency to 10
                .runOn(Schedulers.parallel())
                .flatMap(accountId -> getBalanceReactive(orgId, accountId, token)
                        .onErrorContinue((throwable, obj) -> 
                                log.error("Failed to get balance for account {}: {}", obj, throwable.getMessage())))
                .sequential();
    }

    @Override
    public Flux<ConsentResponse> getConsents(String orgId, List<String> consentIds) {
        return Flux.fromIterable(consentIds)
                .parallel(Math.min(consentIds.size(), 10))
                .runOn(Schedulers.parallel())
                .flatMap(consentId -> getConsentReactive(orgId, consentId)
                        .onErrorContinue((throwable, obj) -> 
                                log.error("Failed to get consent {}: {}", obj, throwable.getMessage())))
                .sequential();
    }

    @Override
    public Flux<AccountsResponse> getAccountsForOrganizations(List<String> orgIds, String token) {
        return Flux.fromIterable(orgIds)
                .parallel(Math.min(orgIds.size(), 5)) // Limit org concurrency to 5
                .runOn(Schedulers.parallel())
                .flatMap(orgId -> getAccountsReactive(orgId, token)
                        .onErrorContinue((throwable, obj) -> 
                                log.error("Failed to get accounts for org {}: {}", obj, throwable.getMessage())))
                .sequential();
    }

    // Private reactive implementations with resilience patterns
    private Mono<ConsentResponse> createConsentReactive(String orgId, ConsentRequest request) {
        return Timer.Sample.start(meterRegistry)
                .stop(createConsentTimer)
                .wrap(webClient.post()
                        .uri("/open-banking/consents/v3/consents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateClientToken(orgId))
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .header("x-fapi-auth-date", java.time.Instant.now().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(HttpStatus::isError, response -> 
                                response.bodyToMono(String.class)
                                        .flatMap(errorBody -> Mono.error(new WebClientResponseException(
                                                response.statusCode().value(),
                                                "Error creating consent: " + errorBody,
                                                null, null, null))))
                        .bodyToMono(ConsentResponse.class))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .doOnSuccess(response -> log.debug("Successfully created consent for org: {}", orgId))
                .doOnError(throwable -> log.error("Failed to create consent for org {}: {}", orgId, throwable.getMessage()));
    }

    private Mono<ConsentResponse> getConsentReactive(String orgId, String consentId) {
        return Timer.Sample.start(meterRegistry)
                .stop(getConsentTimer)
                .wrap(webClient.get()
                        .uri("/open-banking/consents/v3/consents/{consentId}", consentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateClientToken(orgId))
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .bodyToMono(ConsentResponse.class))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .doOnSuccess(response -> log.debug("Successfully retrieved consent {} for org: {}", consentId, orgId))
                .doOnError(throwable -> log.error("Failed to get consent {} for org {}: {}", consentId, orgId, throwable.getMessage()));
    }

    private Mono<AccountsResponse> getAccountsReactive(String orgId, String token) {
        return Timer.Sample.start(meterRegistry)
                .stop(getAccountsTimer)
                .wrap(webClient.get()
                        .uri("/open-banking/accounts/v2/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .bodyToMono(AccountsResponse.class))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .doOnSuccess(response -> log.debug("Successfully retrieved accounts for org: {}", orgId))
                .doOnError(throwable -> log.error("Failed to get accounts for org {}: {}", orgId, throwable.getMessage()));
    }

    private Mono<BalanceResponse> getBalanceReactive(String orgId, String accountId, String token) {
        return Timer.Sample.start(meterRegistry)
                .stop(getBalanceTimer)
                .wrap(webClient.get()
                        .uri("/open-banking/accounts/v2/accounts/{accountId}/balances", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .bodyToMono(BalanceResponse.class))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .doOnSuccess(response -> log.debug("Successfully retrieved balance for account {} org: {}", accountId, orgId))
                .doOnError(throwable -> log.error("Failed to get balance for account {} org {}: {}", accountId, orgId, throwable.getMessage()));
    }

    // Helper method to generate client token (implement based on OAuth2 flow)
    private String generateClientToken(String orgId) {
        // This should implement the actual OAuth2 client credentials flow
        // For now, return a placeholder
        return "client-token-for-" + orgId;
    }

    // Utility method for parallel processing with CompletableFuture (for non-reactive contexts)
    public CompletableFuture<List<BalanceResponse>> getBalancesAsync(String orgId, List<String> accountIds, String token) {
        List<CompletableFuture<BalanceResponse>> futures = accountIds.stream()
                .map(accountId -> getBalanceReactive(orgId, accountId, token).toFuture())
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
}
