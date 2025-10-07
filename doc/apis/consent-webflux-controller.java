package com.bank.consent.infrastructure.adapter.in.rest;

import com.bank.consent.api.model.ConsentResponse;
import com.bank.consent.application.port.in.GetConsentUseCase;
import com.bank.consent.domain.model.Consent;
import com.bank.consent.infrastructure.adapter.in.rest.mapper.ConsentWebMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Reactive WebFlux Controller for Open Finance Brasil Consents API.
 * 
 * This controller provides reactive, non-blocking endpoints for consent operations.
 * Use this in a separate microservice or when you need streaming/backpressure capabilities.
 * 
 * For high-throughput imperative operations, prefer ConsentController with Virtual Threads.
 * 
 * Key Features:
 * - Reactive streams with Project Reactor
 * - Non-blocking I/O operations
 * - Rate limiting with Resilience4j reactive operators
 * - Retry with exponential backoff
 * - Comprehensive metrics and tracing
 * - Open Finance Brasil compliance (FAPI headers)
 * 
 * @author Open Finance Team
 * @version 2.0
 */
@RestController
@RequestMapping("/open-banking/consents/v2")
public class ConsentWebFluxController {
    
    private static final Logger log = LoggerFactory.getLogger(ConsentWebFluxController.class);
    
    private final GetConsentUseCase getConsentUseCase;
    private final ConsentWebMapper mapper;
    private final io.github.resilience4j.ratelimiter.RateLimiter rateLimiter;
    private final Retry retry;
    
    // Metrics
    private final Counter requestCounter;
    private final Counter successCounter;
    private final Counter notFoundCounter;
    private final Counter rateLimitCounter;
    private final Timer responseTimer;
    
    public ConsentWebFluxController(
            GetConsentUseCase getConsentUseCase,
            ConsentWebMapper mapper,
            io.github.resilience4j.ratelimiter.RateLimiter consentApiRateLimiter,
            Retry consentApiRetry,
            MeterRegistry meterRegistry) {
        this.getConsentUseCase = getConsentUseCase;
        this.mapper = mapper;
        this.rateLimiter = consentApiRateLimiter;
        this.retry = consentApiRetry;
        
        // Initialize metrics
        this.requestCounter = meterRegistry.counter("consent.webflux.requests.total");
        this.successCounter = meterRegistry.counter("consent.webflux.success.total");
        this.notFoundCounter = meterRegistry.counter("consent.webflux.notfound.total");
        this.rateLimitCounter = meterRegistry.counter("consent.webflux.ratelimit.total");
        this.responseTimer = meterRegistry.timer("consent.webflux.response.time");
    }
    
    /**
     * GET /open-banking/consents/v2/consents/{consentId}
     * 
     * Retrieves a consent by its unique identifier using reactive streams.
     * 
     * Open Finance Brasil Requirements:
     * - Must validate FAPI headers (x-fapi-interaction-id, x-fapi-customer-ip-address)
     * - Must return 404 for expired or non-existent consents
     * - Must implement rate limiting (300 TPS minimum)
     * - Must include correlation headers in response
     * 
     * @param consentId Unique consent identifier (format: urn:bancoex:{uuid})
     * @param exchange ServerWebExchange for accessing request headers
     * @return Mono<ResponseEntity<ConsentResponse>> Reactive response with consent data
     */
    @GetMapping(
        value = "/consents/{consentId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Timed(value = "consent.webflux.get", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public Mono<ResponseEntity<ConsentResponse>> getConsentByConsentId(
            @PathVariable String consentId,
            ServerWebExchange exchange) {
        
        requestCounter.increment();
        
        // Extract FAPI headers for compliance
        String interactionId = exchange.getRequest().getHeaders()
            .getFirst("x-fapi-interaction-id");
        String customerIp = exchange.getRequest().getHeaders()
            .getFirst("x-fapi-customer-ip-address");
        
        // Generate request ID if not provided
        String requestId = interactionId != null ? interactionId : UUID.randomUUID().toString();
        
        log.info("Processing reactive consent request: consentId={}, requestId={}, customerIp={}", 
            consentId, requestId, customerIp);
        
        return Mono.fromCallable(() -> {
                // Call synchronous use case (wrapped in Mono for reactive chain)
                return getConsentUseCase.execute(consentId);
            })
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            
            // Apply rate limiting
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
            
            // Apply retry with exponential backoff
            .transformDeferred(RetryOperator.of(retry))
            
            // Record timing
            .doOnNext(consent -> responseTimer.record(Duration.ofMillis(System.currentTimeMillis())))
            
            // Map to response
            .flatMap(optionalConsent -> {
                if (optionalConsent.isPresent()) {
                    successCounter.increment();
                    Consent consent = optionalConsent.get();
                    ConsentResponse response = mapper.toResponse(consent);
                    
                    return Mono.just(ResponseEntity.ok()
                        .header("X-Request-ID", requestId)
                        .header("x-fapi-interaction-id", requestId)
                        .header("X-Content-Type-Options", "nosniff")
                        .header("X-Frame-Options", "DENY")
                        .header("Cache-Control", "no-store, no-cache, must-revalidate")
                        .body(response));
                } else {
                    notFoundCounter.increment();
                    log.warn("Consent not found: consentId={}, requestId={}", consentId, requestId);
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Request-ID", requestId)
                        .header("x-fapi-interaction-id", requestId)
                        .build());
                }
            })
            
            // Error handling
            .onErrorResume(io.github.resilience4j.ratelimiter.RequestNotPermitted.class, ex -> {
                rateLimitCounter.increment();
                log.warn("Rate limit exceeded: consentId={}, requestId={}", consentId, requestId);
                
                return Mono.just(ResponseEntity.status(429)
                    .header("X-Request-ID", requestId)
                    .header("X-RateLimit-Limit", String.valueOf(rateLimiter.getRateLimiterConfig().getLimitForPeriod()))
                    .header("X-RateLimit-Remaining", "0")
                    .header("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000))
                    .header("Retry-After", "60")
                    .build());
            })
            
            .onErrorResume(Exception.class, ex -> {
                log.error("Error processing consent request: consentId={}, requestId={}, error={}", 
                    consentId, requestId, ex.getMessage(), ex);
                
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Request-ID", requestId)
                    .build());
            })
            
            // Timeout protection (Open Finance requires max 1s response time)
            .timeout(Duration.ofMillis(1000))
            
            .doOnError(ex -> log.error("Reactive consent processing failed: consentId={}, error={}", 
                consentId, ex.getMessage()))
            
            .doFinally(signal -> log.debug("Consent request completed: consentId={}, signal={}", 
                consentId, signal));
    }
    
    /**
     * GET /open-banking/consents/v2/consents
     * 
     * Lists consents for a specific customer (reactive streaming).
     * Useful for scenarios requiring backpressure when dealing with large result sets.
     * 
     * @param customerId Customer unique identifier
     * @param exchange ServerWebExchange for accessing request headers
     * @return Flux<ConsentResponse> Reactive stream of consents
     */
    @GetMapping(
        value = "/consents",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Timed(value = "consent.webflux.list", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public Mono<ResponseEntity<?>> listConsents(
            @RequestParam(required = false) String customerId,
            ServerWebExchange exchange) {
        
        // Generate request ID
        String requestId = exchange.getRequest().getHeaders()
            .getFirst("x-fapi-interaction-id");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        
        log.info("Listing consents: customerId={}, requestId={}", customerId, requestId);
        
        // For this example, return not implemented
        // In production, implement reactive repository method
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .header("X-Request-ID", requestId)
            .body("List endpoint not yet implemented in reactive version"));
    }
    
    /**
     * Health check endpoint for reactive controller
     * 
     * @return Mono<String> Health status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok()
            .body("{\"status\":\"UP\",\"controller\":\"WebFlux\"}"));
    }
}
