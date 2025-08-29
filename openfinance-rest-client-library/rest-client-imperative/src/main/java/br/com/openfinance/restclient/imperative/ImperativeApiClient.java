package br.com.openfinance.restclient.imperative;

import br.com.openfinance.restclient.core.client.ApiClient;
import br.com.openfinance.restclient.core.config.ClientConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Imperative implementation of ApiClient using Spring RestClient with Java 21 Virtual Threads.
 * 
 * This implementation provides:
 * - Blocking I/O operations using Virtual Threads for massive parallelism
 * - Traditional programming model familiar to Spring MVC developers
 * - Structured concurrency for coordinated task execution
 * - Built-in resilience patterns
 * - Optimized for high-concurrency scenarios (up to 50k requests)
 * 
 * Key features:
 * - Virtual Thread pool management
 * - Semaphore-based throttling
 * - Structured task scope for batch operations
 * - Comprehensive error handling and retries
 */
@Slf4j
public class ImperativeApiClient implements ApiClient<Object> {

    private final RestClient restClient;
    private final ClientConfiguration configuration;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final Timer requestTimer;
    private final Semaphore concurrencyLimiter;

    public ImperativeApiClient(RestClient restClient,
                             ClientConfiguration configuration,
                             CircuitBreaker circuitBreaker,
                             Retry retry,
                             RateLimiter rateLimiter,
                             MeterRegistry meterRegistry) {
        this.restClient = restClient;
        this.configuration = configuration;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.requestTimer = Timer.builder("rest_client_request_duration")
                .description("REST client request duration")
                .register(meterRegistry);
        
        // Initialize semaphore for concurrency control
        this.concurrencyLimiter = new Semaphore(
                configuration.getVirtualThreads().getMaxVirtualThreads());
    }

    @Override
    public <R> CompletableFuture<R> getImperative(String endpoint, Class<R> responseType) {
        return executeRequestAsync(() -> 
            executeRequest(HttpMethod.GET, endpoint, null, responseType));
    }

    @Override
    public <R> CompletableFuture<R> postImperative(String endpoint, Object body, Class<R> responseType) {
        return executeRequestAsync(() -> 
            executeRequest(HttpMethod.POST, endpoint, body, responseType));
    }

    @Override
    public <R> CompletableFuture<R> putImperative(String endpoint, Object body, Class<R> responseType) {
        return executeRequestAsync(() -> 
            executeRequest(HttpMethod.PUT, endpoint, body, responseType));
    }

    @Override
    public <R> CompletableFuture<R> deleteImperative(String endpoint, Class<R> responseType) {
        return executeRequestAsync(() -> 
            executeRequest(HttpMethod.DELETE, endpoint, null, responseType));
    }

    @Override
    public <R> CompletableFuture<List<R>> getBatchImperative(List<String> endpoints, Class<R> responseType) {
        return CompletableFuture.supplyAsync(() -> {
            if (configuration.getVirtualThreads().isStructuredConcurrency()) {
                return executeBatchWithStructuredConcurrency(endpoints, responseType);
            } else {
                return executeBatchWithCompletableFuture(endpoints, responseType);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Execute batch requests using Java 21 Structured Concurrency
     */
    private <R> List<R> executeBatchWithStructuredConcurrency(List<String> endpoints, Class<R> responseType) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Submit all tasks to the structured scope
            List<StructuredTaskScope.Subtask<R>> subtasks = endpoints.stream()
                    .map(endpoint -> scope.fork(() -> executeRequest(HttpMethod.GET, endpoint, null, responseType)))
                    .toList();
            
            // Wait for all tasks to complete or fail
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Structured concurrency batch execution failed", e);
            throw new RuntimeException("Batch execution failed", e);
        }
    }

    /**
     * Execute batch requests using CompletableFuture (fallback method)
     */
    private <R> List<R> executeBatchWithCompletableFuture(List<String> endpoints, Class<R> responseType) {
        List<CompletableFuture<R>> futures = endpoints.stream()
                .map(endpoint -> getImperative(endpoint, responseType))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> 
            futures.stream()
                    .map(CompletableFuture::join)
                    .toList()
        ).join();
    }

    /**
     * Execute request asynchronously using Virtual Threads
     */
    private <R> CompletableFuture<R> executeRequestAsync(Supplier<R> requestSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Acquire semaphore permit for throttling
                concurrencyLimiter.acquire();
                
                // Execute with resilience patterns
                return Retry.decorateSupplier(retry, () ->
                    CircuitBreaker.decorateSupplier(circuitBreaker, () ->
                        RateLimiter.decorateSupplier(rateLimiter, requestSupplier)
                                  .get())
                            .get())
                        .get();
                        
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            } finally {
                concurrencyLimiter.release();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Core method to execute HTTP requests
     */
    private <R> R executeRequest(HttpMethod method, String endpoint, Object body, Class<R> responseType) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            var requestSpec = restClient.method(method)
                    .uri(endpoint)
                    .accept(MediaType.APPLICATION_JSON);

            if (body != null) {
                requestSpec = requestSpec.contentType(MediaType.APPLICATION_JSON)
                                       .body(body);
            }

            ResponseEntity<R> response = requestSpec.retrieve()
                    .toEntity(responseType);

            // Record success metrics
            sample.stop(requestTimer.tag("method", method.name())
                                  .tag("endpoint", endpoint)
                                  .tag("status", "success"));
            meterRegistry.counter("rest_client_requests_total",
                    "method", method.name(),
                    "endpoint", endpoint,
                    "status", "success").increment();

            return response.getBody();
            
        } catch (RestClientResponseException ex) {
            // Record error metrics
            sample.stop(requestTimer.tag("method", method.name())
                                  .tag("endpoint", endpoint)
                                  .tag("status", "error"));
            meterRegistry.counter("rest_client_requests_total",
                    "method", method.name(),
                    "endpoint", endpoint,
                    "status", "error").increment();
            
            log.error("Request failed for endpoint: {}, method: {}, status: {}, error: {}", 
                    endpoint, method, ex.getStatusCode(), ex.getMessage());
            
            throw mapRestClientException(ex);
            
        } catch (RestClientException ex) {
            sample.stop(requestTimer.tag("method", method.name())
                                  .tag("endpoint", endpoint)
                                  .tag("status", "error"));
            meterRegistry.counter("rest_client_requests_total",
                    "method", method.name(),
                    "endpoint", endpoint,
                    "status", "error").increment();
            
            log.error("Request failed for endpoint: {}, method: {}, error: {}", 
                    endpoint, method, ex.getMessage());
            throw new RuntimeException("Request failed for endpoint: " + endpoint, ex);
        }
    }

    /**
     * Map RestClient exceptions to more meaningful errors
     */
    private RuntimeException mapRestClientException(RestClientResponseException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String message = String.format("HTTP %d: %s - %s", 
                status.value(), status.getReasonPhrase(), ex.getResponseBodyAsString());
        
        return switch (status.series()) {
            case CLIENT_ERROR -> new IllegalArgumentException(message, ex);
            case SERVER_ERROR -> new RuntimeException("Server error: " + message, ex);
            default -> new RuntimeException("Unexpected error: " + message, ex);
        };
    }

    // Reactive bridge methods (convert to reactive using Mono/Flux)
    @Override
    public <R> Mono<R> getReactive(String endpoint, Class<R> responseType) {
        return Mono.fromFuture(getImperative(endpoint, responseType));
    }

    @Override
    public <R> Mono<R> postReactive(String endpoint, Object body, Class<R> responseType) {
        return Mono.fromFuture(postImperative(endpoint, body, responseType));
    }

    @Override
    public <R> Mono<R> putReactive(String endpoint, Object body, Class<R> responseType) {
        return Mono.fromFuture(putImperative(endpoint, body, responseType));
    }

    @Override
    public <R> Mono<R> deleteReactive(String endpoint, Class<R> responseType) {
        return Mono.fromFuture(deleteImperative(endpoint, responseType));
    }

    @Override
    public <R> Flux<R> getBatchReactive(Flux<String> endpoints, Class<R> responseType) {
        return endpoints.collectList()
                .flatMapMany(endpointList -> 
                    Flux.fromFuture(getBatchImperative(endpointList, responseType)))
                .flatMap(Flux::fromIterable);
    }

    @Override
    public ClientConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Mono<Boolean> healthCheck() {
        return Mono.fromFuture(
            executeRequestAsync(() -> {
                try {
                    ResponseEntity<String> response = restClient.get()
                            .uri("/health")
                            .retrieve()
                            .toEntity(String.class);
                    
                    boolean healthy = response.getStatusCode().is2xxSuccessful();
                    meterRegistry.gauge("rest_client_health", healthy ? 1.0 : 0.0);
                    return healthy;
                } catch (Exception e) {
                    log.warn("Health check failed", e);
                    meterRegistry.gauge("rest_client_health", 0.0);
                    return false;
                }
            })
        );
    }
}