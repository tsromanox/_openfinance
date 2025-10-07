package com.bank.consent.infrastructure.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.Duration;

/**
 * Reactive WebFlux Configuration for Open Finance Consents API.
 * 
 * IMPORTANT: This configuration is mutually exclusive with Spring MVC.
 * Use @Profile("reactive") to enable only when needed.
 * 
 * Architecture Decision:
 * - For most Open Finance use cases, prefer Virtual Threads (MVC) for simplicity
 * - Use WebFlux only when you need:
 *   1. Streaming with backpressure (large datasets)
 *   2. Server-Sent Events (SSE)
 *   3. WebSocket connections
 *   4. Complex async composition
 * 
 * For this project, recommend running as SEPARATE microservice:
 * - consent-api-imperative (MVC + Virtual Threads) - Main API
 * - consent-api-reactive (WebFlux) - Optional streaming endpoint
 * 
 * @author Open Finance Team
 */
@Configuration
@EnableWebFlux
@Profile("reactive")  // Only activate with --spring.profiles.active=reactive
public class ReactiveConfiguration implements WebFluxConfigurer {
    
    /**
     * Rate Limiter for reactive endpoints.
     * Configured for Open Finance Brasil requirements (300 TPS minimum).
     */
    @Bean
    public RateLimiter consentApiRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(2500)  // 2500 requests
            .limitRefreshPeriod(Duration.ofMinutes(1))  // per minute (TPM)
            .timeoutDuration(Duration.ofMillis(100))  // max wait time
            .build();
        
        return registry.rateLimiter("consentApiRateLimiter", config);
    }
    
    /**
     * Retry configuration for transient failures.
     * Uses exponential backoff as recommended by Open Finance.
     */
    @Bean
    public Retry consentApiRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(500, 2))  // 500ms, 1000ms, 2000ms
            .retryExceptions(
                com.mongodb.MongoException.class,
                java.io.IOException.class
            )
            .build();
        
        return registry.retry("consentApiRetry", config);
    }
}
