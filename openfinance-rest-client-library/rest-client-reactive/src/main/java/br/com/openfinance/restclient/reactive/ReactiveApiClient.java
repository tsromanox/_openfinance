package br.com.openfinance.restclient.reactive;

import br.com.openfinance.restclient.core.client.ApiClient;
import br.com.openfinance.restclient.core.config.ClientConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Reactive implementation of ApiClient using Spring WebClient.
 * 
 * This implementation provides:
 * - Non-blocking I/O operations
 * - Reactive streams support (Mono/Flux)
 * - Backpressure handling
 * - Built-in resilience patterns
 * - Comprehensive metrics
 * - OAuth2 token management
 * 
 * Optimized for high-throughput scenarios with efficient resource usage.
 */
@Slf4j
public class ReactiveApiClient implements ApiClient<Object> {

    private final WebClient webClient;
    private final ClientConfiguration configuration;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;

    public ReactiveApiClient(WebClient webClient, 
                           ClientConfiguration configuration,
                           CircuitBreaker circuitBreaker,
                           Retry retry,
                           RateLimiter rateLimiter,
                           MeterRegistry meterRegistry) {
        this.webClient = webClient;
        this.configuration = configuration;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.requestTimer = Timer.builder("rest_client_request_duration")
                .description("REST client request duration")
                .register(meterRegistry);
    }

    @Override
    public <R> Mono<R> getReactive(String endpoint, Class<R> responseType) {
        return executeRequest(HttpMethod.GET, endpoint, null, responseType);
    }

    @Override
    public <R> Mono<R> postReactive(String endpoint, Object body, Class<R> responseType) {
        return executeRequest(HttpMethod.POST, endpoint, body, responseType);
    }

    @Override
    public <R> Mono<R> putReactive(String endpoint, Object body, Class<R> responseType) {
        return executeRequest(HttpMethod.PUT, endpoint, body, responseType);
    }

    @Override
    public <R> Mono<R> deleteReactive(String endpoint, Class<R> responseType) {
        return executeRequest(HttpMethod.DELETE, endpoint, null, responseType);
    }

    @Override
    public <R> Flux<R> getBatchReactive(Flux<String> endpoints, Class<R> responseType) {
        return endpoints
                .flatMap(endpoint -> getReactive(endpoint, responseType)
                        .onErrorResume(throwable -> {
                            log.warn("Failed to get data from endpoint: {}, error: {}", 
                                   endpoint, throwable.getMessage());
                            return Mono.empty(); // Skip failed requests
                        }))
                .buffer(configuration.getResilienceConfiguration().getMaxConcurrentCalls())
                .flatMap(Flux::fromIterable);
    }

    /**
     * Core method to execute HTTP requests with all resilience patterns applied
     */
    private <R> Mono<R> executeRequest(HttpMethod method, String endpoint, Object body, Class<R> responseType) {
        return Mono.fromCallable(() -> Timer.Sample.start(meterRegistry))
                .flatMap(sample -> {
                    var request = webClient.method(method)
                            .uri(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON);

                    if (body != null) {
                        request = request.bodyValue(body);
                    }

                    return request.retrieve()
                            .bodyToMono(responseType)
                            .doOnSuccess(result -> {
                                sample.stop(requestTimer.tag("method", method.name())
                                                      .tag("endpoint", endpoint)
                                                      .tag("status", "success"));
                                meterRegistry.counter("rest_client_requests_total",
                                        "method", method.name(),
                                        "endpoint", endpoint,
                                        "status", "success").increment();
                            })
                            .doOnError(error -> {
                                sample.stop(requestTimer.tag("method", method.name())
                                                      .tag("endpoint", endpoint)
                                                      .tag("status", "error"));
                                meterRegistry.counter("rest_client_requests_total",
                                        "method", method.name(),
                                        "endpoint", endpoint,
                                        "status", "error").increment();
                                log.error("Request failed for endpoint: {}, method: {}, error: {}", 
                                        endpoint, method, error.getMessage());
                            });
                })
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .timeout(configuration.getReadTimeout())
                .onErrorMap(TimeoutException.class, 
                           ex -> new RuntimeException("Request timeout for endpoint: " + endpoint, ex))
                .onErrorMap(WebClientResponseException.class, this::mapWebClientException);
    }

    /**
     * Map WebClient exceptions to more meaningful errors
     */
    private RuntimeException mapWebClientException(WebClientResponseException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String message = String.format("HTTP %d: %s - %s", 
                status.value(), status.getReasonPhrase(), ex.getResponseBodyAsString());
        
        return switch (status.series()) {
            case CLIENT_ERROR -> new IllegalArgumentException(message, ex);
            case SERVER_ERROR -> new RuntimeException("Server error: " + message, ex);
            default -> new RuntimeException("Unexpected error: " + message, ex);
        };
    }

    // Imperative methods (bridge to reactive)
    @Override
    public <R> CompletableFuture<R> getImperative(String endpoint, Class<R> responseType) {
        return getReactive(endpoint, responseType).toFuture();
    }

    @Override
    public <R> CompletableFuture<R> postImperative(String endpoint, Object body, Class<R> responseType) {
        return postReactive(endpoint, body, responseType).toFuture();
    }

    @Override
    public <R> CompletableFuture<R> putImperative(String endpoint, Object body, Class<R> responseType) {
        return putReactive(endpoint, body, responseType).toFuture();
    }

    @Override
    public <R> CompletableFuture<R> deleteImperative(String endpoint, Class<R> responseType) {
        return deleteReactive(endpoint, responseType).toFuture();
    }

    @Override
    public <R> CompletableFuture<java.util.List<R>> getBatchImperative(
            java.util.List<String> endpoints, Class<R> responseType) {
        return getBatchReactive(Flux.fromIterable(endpoints), responseType)
                .collectList()
                .toFuture();
    }

    @Override
    public ClientConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Mono<Boolean> healthCheck() {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .onErrorReturn(false)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(healthy -> 
                    meterRegistry.gauge("rest_client_health", healthy ? 1.0 : 0.0));
    }
}