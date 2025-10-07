package com.bank.consent.infrastructure.adapter.in.rest;

import com.bank.consent.api.model.ConsentResponse;
import com.bank.consent.application.port.in.GetConsentUseCase;
import com.bank.consent.domain.model.Consent;
import com.bank.consent.domain.model.ConsentId;
import com.bank.consent.domain.model.ConsentStatus;
import com.bank.consent.infrastructure.adapter.in.rest.mapper.ConsentWebMapper;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários e de integração para ConsentWebFluxController.
 * 
 * Testa:
 * - Cenários de sucesso (200 OK)
 * - Cenários de erro (404, 429, 500)
 * - Rate limiting
 * - Retry automático
 * - Timeout
 * - Headers FAPI
 * - Métricas
 * 
 * @author Open Finance Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consent WebFlux Controller Tests")
class ConsentWebFluxControllerTest {
    
    @Mock
    private GetConsentUseCase getConsentUseCase;
    
    @Mock
    private ConsentWebMapper mapper;
    
    private ConsentWebFluxController controller;
    private WebTestClient webTestClient;
    private SimpleMeterRegistry meterRegistry;
    private RateLimiter rateLimiter;
    private Retry retry;
    
    @BeforeEach
    void setUp() {
        // Setup Meter Registry
        meterRegistry = new SimpleMeterRegistry();
        
        // Setup Rate Limiter
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))
            .build();
        
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig);
        rateLimiter = rateLimiterRegistry.rateLimiter("consentApiRateLimiter");
        
        // Setup Retry
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .build();
        
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        retry = retryRegistry.retry("consentApiRetry");
        
        // Create controller
        controller = new ConsentWebFluxController(
            getConsentUseCase,
            mapper,
            rateLimiter,
            retry,
            meterRegistry
        );
        
        // Setup WebTestClient
        webTestClient = WebTestClient.bindToController(controller)
            .configureClient()
            .baseUrl("http://localhost:8080")
            .build();
    }
    
    @Test
    @DisplayName("Should return 200 OK when consent exists")
    void shouldReturn200WhenConsentExists() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        Consent consent = createValidConsent(consentId);
        ConsentResponse response = createConsentResponse(consentId);
        
        when(getConsentUseCase.execute(consentId))
            .thenReturn(Optional.of(consent));
        when(mapper.toResponse(consent))
            .thenReturn(response);
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "test-123")
            .header("x-fapi-customer-ip-address", "192.168.1.1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Request-ID")
            .expectHeader().exists("x-fapi-interaction-id")
            .expectHeader().valueEquals("Cache-Control", "no-store, no-cache, must-revalidate")
            .expectBody(ConsentResponse.class)
            .value(body -> {
                assertThat(body).isNotNull();
                assertThat(body.getData().getConsentId()).isEqualTo(consentId);
            });
        
        // Verify metrics
        assertThat(meterRegistry.counter("consent.webflux.requests.total").count()).isEqualTo(1);
        assertThat(meterRegistry.counter("consent.webflux.success.total").count()).isEqualTo(1);
        
        verify(getConsentUseCase, times(1)).execute(consentId);
    }
    
    @Test
    @DisplayName("Should return 404 when consent not found")
    void shouldReturn404WhenConsentNotFound() {
        // Given
        String consentId = "urn:bancoex:nonexistent";
        when(getConsentUseCase.execute(consentId))
            .thenReturn(Optional.empty());
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "test-404")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().exists("X-Request-ID")
            .expectBody().isEmpty();
        
        // Verify metrics
        assertThat(meterRegistry.counter("consent.webflux.notfound.total").count()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    void shouldReturn429WhenRateLimitExceeded() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        
        // Exhaust rate limiter
        for (int i = 0; i < 100; i++) {
            rateLimiter.acquirePermission();
        }
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "test-ratelimit")
            .exchange()
            .expectStatus().isEqualTo(429)
            .expectHeader().exists("X-RateLimit-Limit")
            .expectHeader().exists("Retry-After")
            .expectHeader().valueEquals("Retry-After", "60");
        
        // Verify metrics
        assertThat(meterRegistry.counter("consent.webflux.ratelimit.total").count()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should generate request ID when not provided")
    void shouldGenerateRequestIdWhenNotProvided() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        Consent consent = createValidConsent(consentId);
        ConsentResponse response = createConsentResponse(consentId);
        
        when(getConsentUseCase.execute(consentId)).thenReturn(Optional.of(consent));
        when(mapper.toResponse(consent)).thenReturn(response);
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            // NO x-fapi-interaction-id header
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("X-Request-ID")
            .expectHeader().value("X-Request-ID", requestId -> {
                assertThat(requestId).isNotBlank();
                assertThat(UUID.fromString(requestId)).isNotNull();
            });
    }
    
    @Test
    @DisplayName("Should handle exceptions with 500 error")
    void shouldHandle500OnException() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        when(getConsentUseCase.execute(consentId))
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "test-error")
            .exchange()
            .expectStatus().isEqualTo(500)
            .expectHeader().exists("X-Request-ID");
    }
    
    @Test
    @DisplayName("Should timeout after 1 second")
    void shouldTimeoutAfter1Second() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        when(getConsentUseCase.execute(consentId))
            .thenAnswer(invocation -> {
                Thread.sleep(2000); // Simulate slow operation
                return Optional.empty();
            });
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "test-timeout")
            .exchange()
            .expectStatus().isEqualTo(500); // Timeout results in error
    }
    
    @Test
    @DisplayName("Should include security headers in response")
    void shouldIncludeSecurityHeaders() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        Consent consent = createValidConsent(consentId);
        ConsentResponse response = createConsentResponse(consentId);
        
        when(getConsentUseCase.execute(consentId)).thenReturn(Optional.of(consent));
        when(mapper.toResponse(consent)).thenReturn(response);
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().valueEquals("X-Frame-Options", "DENY")
            .expectHeader().valueEquals("Cache-Control", "no-store, no-cache, must-revalidate");
    }
    
    @Test
    @DisplayName("Health endpoint should return UP")
    void healthEndpointShouldReturnUp() {
        webTestClient.get()
            .uri("/open-banking/consents/v2/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertThat(body).contains("UP");
                assertThat(body).contains("WebFlux");
            });
    }
    
    // ========== Helper Methods ==========
    
    private Consent createValidConsent(String consentId) {
        return Consent.builder()
            .id(new ConsentId(UUID.randomUUID().toString()))
            .consentId(consentId)
            .customerId("customer-123")
            .status(ConsentStatus.AUTHORISED)
            .permissions(Collections.emptyList())
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();
    }
    
    private ConsentResponse createConsentResponse(String consentId) {
        ConsentResponse response = new ConsentResponse();
        ConsentResponse.Data data = new ConsentResponse.Data();
        data.setConsentId(consentId);
        data.setStatus("AUTHORISED");
        response.setData(data);
        return response;
    }
}

/**
 * Testes de integração com MongoDB reativo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("reactive-test")
@Testcontainers
class ConsentWebFluxControllerIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withExposedPorts(27017);
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private ReactiveConsentMongoRepository repository;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll().block();
    }
    
    @Test
    @DisplayName("Integration: Should retrieve consent from MongoDB")
    void shouldRetrieveConsentFromMongoDB() {
        // Given
        String consentId = "urn:bancoex:" + UUID.randomUUID();
        ConsentEntity entity = new ConsentEntity();
        entity.setConsentId(consentId);
        entity.setCustomerId("customer-123");
        entity.setStatus("AUTHORISED");
        entity.setPermissions(Collections.emptyList());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        repository.save(entity).block();
        
        // When & Then
        webTestClient.get()
            .uri("/open-banking/consents/v2/consents/{consentId}", consentId)
            .header("x-fapi-interaction-id", "integration-test")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.consentId").isEqualTo(consentId)
            .jsonPath("$.data.status").isEqualTo("AUTHORISED");
    }
}
