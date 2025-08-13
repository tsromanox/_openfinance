package br.com.openfinance.infrastructure.client.auth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * OAuth2 client service for OpenFinance authentication with parallel processing support.
 */
@Slf4j
@Service
public class OAuth2ClientService {
    
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;
    
    // Configuration
    @Value("${openfinance.oauth2.token-url:https://auth.openfinance.org.br/token}")
    private String tokenUrl;
    
    @Value("${openfinance.oauth2.client-id}")
    private String clientId;
    
    @Value("${openfinance.oauth2.client-secret}")
    private String clientSecret;
    
    @Value("${openfinance.oauth2.scope:accounts}")
    private String scope;
    
    // Metrics
    private final Counter tokenRequestCounter;
    private final Counter tokenErrorCounter;
    private final Timer tokenRequestTimer;
    
    public OAuth2ClientService(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.tokenRequestCounter = Counter.builder("oauth2.token.requests")
                .description("Number of OAuth2 token requests")
                .register(meterRegistry);
        this.tokenErrorCounter = Counter.builder("oauth2.token.errors")
                .description("Number of OAuth2 token errors")
                .register(meterRegistry);
        this.tokenRequestTimer = Timer.builder("oauth2.token.request.time")
                .description("Time taken for OAuth2 token requests")
                .register(meterRegistry);
    }
    
    /**
     * Get access token using client credentials flow - reactive version.
     */
    @Cacheable(value = "oauth2-tokens", key = "#orgId", unless = "#result == null")
    public Mono<AccessToken> getAccessTokenReactive(String orgId) {
        return Timer.Sample.start(meterRegistry)
                .stop(tokenRequestTimer)
                .wrap(createTokenRequest()
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSubscribe(subscription -> {
                            tokenRequestCounter.increment();
                            log.debug("Requesting OAuth2 token for org: {}", orgId);
                        })
                        .doOnSuccess(token -> log.debug("Successfully obtained token for org: {}", orgId))
                        .doOnError(error -> {
                            tokenErrorCounter.increment();
                            log.error("Failed to obtain token for org {}: {}", orgId, error.getMessage());
                        }));
    }
    
    /**
     * Get access token - blocking version for backward compatibility.
     */
    public AccessToken getAccessToken(String orgId) {
        return getAccessTokenReactive(orgId)
                .block(Duration.ofSeconds(30));
    }
    
    /**
     * Get multiple access tokens in parallel.
     */
    public CompletableFuture<java.util.Map<String, AccessToken>> getAccessTokensAsync(java.util.List<String> orgIds) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.Map<String, CompletableFuture<AccessToken>> futures = orgIds.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            orgId -> orgId,
                            orgId -> getAccessTokenReactive(orgId).toFuture()
                    ));
            
            return futures.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            java.util.Map.Entry::getKey,
                            entry -> entry.getValue().join()
                    ));
        });
    }
    
    private Mono<AccessToken> createTokenRequest() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("scope", scope);
        
        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        
        return webClient.post()
                .uri(tokenUrl)
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new OAuth2Exception(
                                        "Token request failed: " + errorBody)))
                )
                .bodyToMono(TokenResponse.class)
                .map(this::toAccessToken);
    }
    
    private AccessToken toAccessToken(TokenResponse response) {
        Instant expiresAt = response.expiresIn() != null
                ? Instant.now().plusSeconds(response.expiresIn())
                : Instant.now().plusSeconds(3600); // Default 1 hour
        
        return new AccessToken(
                response.accessToken(),
                response.tokenType(),
                expiresAt,
                response.scope()
        );
    }
    
    /**
     * Check if token is valid and not expired.
     */
    public boolean isTokenValid(AccessToken token) {
        return token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(60)); // 1 minute buffer
    }
    
    /**
     * Refresh token if it's close to expiring.
     */
    public Mono<AccessToken> refreshIfNeeded(String orgId, AccessToken currentToken) {
        if (isTokenValid(currentToken)) {
            return Mono.just(currentToken);
        }
        
        log.debug("Token expired or close to expiring for org: {}, refreshing", orgId);
        return getAccessTokenReactive(orgId);
    }
    
    // DTOs
    public record TokenResponse(
            String accessToken,
            String tokenType,
            Long expiresIn,
            String scope
    ) {}
    
    public record AccessToken(
            String token,
            String type,
            Instant expiresAt,
            String scope
    ) {
        public String getAuthorizationHeader() {
            return type + " " + token;
        }
        
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
    
    public static class OAuth2Exception extends RuntimeException {
        public OAuth2Exception(String message) {
            super(message);
        }
        
        public OAuth2Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}