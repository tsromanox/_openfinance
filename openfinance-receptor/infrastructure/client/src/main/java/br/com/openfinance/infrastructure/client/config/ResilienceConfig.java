package br.com.openfinance.infrastructure.client.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for resilience patterns optimized for parallel processing.
 */
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreaker openFinanceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Circuit breaker trips when 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30 seconds before trying again
                .slidingWindowSize(20) // Consider last 20 calls
                .minimumNumberOfCalls(10) // Minimum 10 calls before circuit breaker can trip
                .permittedNumberOfCallsInHalfOpenState(5) // Allow 5 calls when half-open
                .slowCallRateThreshold(50.0f) // Consider call slow if > 50% are slow
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // Call is slow if > 10 seconds
                .build();
        
        return CircuitBreaker.of("openfinance-api", config);
    }
    
    @Bean
    public Retry openFinanceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // Maximum 3 attempts
                .waitDuration(Duration.ofSeconds(2)) // Wait 2 seconds between retries
                .exponentialBackoffMultiplier(2.0) // Exponential backoff
                .retryExceptions(Exception.class) // Retry on any exception
                .build();
        
        return Retry.of("openfinance-api", config);
    }
    
    @Bean
    public RateLimiter openFinanceRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1000) // Allow 1000 calls per period
                .limitRefreshPeriod(Duration.ofMinutes(1)) // Reset every minute
                .timeoutDuration(Duration.ofSeconds(5)) // Wait up to 5 seconds for permission
                .build();
        
        return RateLimiter.of("openfinance-api", config);
    }
    
    @Bean
    public Bulkhead openFinanceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(100) // Maximum 100 concurrent calls
                .maxWaitDuration(Duration.ofSeconds(10)) // Wait up to 10 seconds for execution
                .build();
        
        return Bulkhead.of("openfinance-api", config);
    }
}