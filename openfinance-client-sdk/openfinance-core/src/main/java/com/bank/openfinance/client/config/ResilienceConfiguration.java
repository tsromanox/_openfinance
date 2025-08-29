package com.bank.openfinance.client.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
public class ResilienceConfiguration {

    private final ClientConfiguration clientConfig;
    private final MeterRegistry meterRegistry;

    public ResilienceConfiguration(ClientConfiguration clientConfig,
                                   MeterRegistry meterRegistry) {
        this.clientConfig = clientConfig;
        this.meterRegistry = meterRegistry;
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreaker circuitBreaker() {
        ClientConfiguration.CircuitBreakerConfig cbConfig = clientConfig.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbConfig.getFailureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(cbConfig.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(cbConfig.getPermittedCallsInHalfOpen())
                .slidingWindowSize(cbConfig.getSlidingWindowSize())
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(cbConfig.getMinimumNumberOfCalls())
                .recordExceptions(IOException.class, TimeoutException.class, ConnectException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker("openfinance-api");

        // Register metrics
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn("Circuit breaker state transition: from {} to {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                    meterRegistry.counter("circuit_breaker.state_transition",
                            "from", event.getStateTransition().getFromState().toString(),
                            "to", event.getStateTransition().getToState().toString()
                    ).increment();
                })
                .onFailureRateExceeded(event -> {
                    log.error("Circuit breaker failure rate exceeded: {}%",
                            event.getFailureRate());
                    meterRegistry.gauge("circuit_breaker.failure_rate", event.getFailureRate());
                })
                .onCallNotPermitted(event ->
                        meterRegistry.counter("circuit_breaker.calls_not_permitted").increment()
                );

        return circuitBreaker;
    }

    @Bean
    @ConditionalOnMissingBean
    public Retry retry() {
        ClientConfiguration.RetryConfig retryConfig = clientConfig.getRetry();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retryConfig.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        retryConfig.getWaitDuration(),
                        retryConfig.getMultiplier(),
                        retryConfig.getMaxWaitDuration()
                ))
                .retryOnResult(response -> response == null)
                .retryExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry("openfinance-api");

        retry.getEventPublisher()
                .onRetry(event -> {
                    log.debug("Retry attempt {} for {}",
                            event.getNumberOfRetryAttempts(),
                            event.getName());
                    meterRegistry.counter("retry.attempts",
                            "attempt", String.valueOf(event.getNumberOfRetryAttempts())
                    ).increment();
                });

        return retry;
    }

    @Bean
    public Bulkhead bulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(clientConfig.getMaxConnections())
                .maxWaitDuration(Duration.ofSeconds(5))
                .build();

        Bulkhead bulkhead = Bulkhead.of("openfinance-api", config);

        bulkhead.getEventPublisher()
                .onCallPermitted(event ->
                        meterRegistry.counter("bulkhead.calls_permitted").increment()
                )
                .onCallRejected(event -> {
                    log.warn("Bulkhead call rejected");
                    meterRegistry.counter("bulkhead.calls_rejected").increment();
                });

        return bulkhead;
    }

    @Bean
    public TimeLimiter timeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(clientConfig.getTimeout()))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiter.of("openfinance-api", config);
    }

    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1000) // 1000 requests per second
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiter rateLimiter = RateLimiter.of("openfinance-api", config);

        rateLimiter.getEventPublisher()
                .onSuccess(event ->
                        meterRegistry.counter("rate_limiter.successful").increment()
                )
                .onFailure(event -> {
                    log.warn("Rate limit exceeded");
                    meterRegistry.counter("rate_limiter.rejected").increment();
                });

        return rateLimiter;
    }
}
