package br.com.openfinance.restclient.core.config;

import br.com.openfinance.restclient.core.client.RestClientStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration class for REST client library.
 * Supports both reactive and imperative implementations with comprehensive
 * settings for high-concurrency scenarios (up to 50k requests).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "openfinance.rest-client")
public class ClientConfiguration {

    /**
     * Base URL for the API
     */
    private String baseUrl;

    /**
     * Client implementation strategy (REACTIVE or IMPERATIVE)
     */
    @Builder.Default
    private RestClientStrategy strategy = RestClientStrategy.REACTIVE;

    /**
     * Connection timeout
     */
    @Builder.Default
    private Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout
     */
    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Write timeout
     */
    @Builder.Default
    private Duration writeTimeout = Duration.ofSeconds(30);

    /**
     * Maximum connections in pool
     */
    @Builder.Default
    private int maxConnections = 1000;

    /**
     * Maximum connections per route
     */
    @Builder.Default
    private int maxConnectionsPerRoute = 100;

    /**
     * Connection idle timeout
     */
    @Builder.Default
    private Duration idleTimeout = Duration.ofMinutes(2);

    /**
     * Keep alive timeout
     */
    @Builder.Default
    private Duration keepAliveTimeout = Duration.ofMinutes(5);

    /**
     * Virtual Threads configuration
     */
    private VirtualThreadConfiguration virtualThreads = new VirtualThreadConfiguration();

    /**
     * Resilience configuration
     */
    private ResilienceConfiguration resilience = new ResilienceConfiguration();

    /**
     * Security configuration
     */
    private SecurityConfiguration security = new SecurityConfiguration();

    /**
     * Metrics configuration
     */
    private MetricsConfiguration metrics = new MetricsConfiguration();

    /**
     * Additional headers to include in all requests
     */
    private Map<String, String> defaultHeaders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VirtualThreadConfiguration {
        /**
         * Enable virtual threads for imperative client
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * Maximum virtual threads for concurrent operations
         */
        @Builder.Default
        private int maxVirtualThreads = 50000;

        /**
         * Thread name prefix
         */
        @Builder.Default
        private String threadNamePrefix = "rest-client-vt-";

        /**
         * Enable structured concurrency
         */
        @Builder.Default
        private boolean structuredConcurrency = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResilienceConfiguration {
        /**
         * Enable circuit breaker
         */
        @Builder.Default
        private boolean circuitBreakerEnabled = true;

        /**
         * Circuit breaker failure rate threshold (percentage)
         */
        @Builder.Default
        private float failureRateThreshold = 60.0f;

        /**
         * Circuit breaker minimum number of calls before evaluation
         */
        @Builder.Default
        private int minimumNumberOfCalls = 10;

        /**
         * Circuit breaker sliding window size
         */
        @Builder.Default
        private int slidingWindowSize = 100;

        /**
         * Circuit breaker wait duration in open state
         */
        @Builder.Default
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);

        /**
         * Enable retry mechanism
         */
        @Builder.Default
        private boolean retryEnabled = true;

        /**
         * Maximum retry attempts
         */
        @Builder.Default
        private int maxRetryAttempts = 3;

        /**
         * Retry wait duration
         */
        @Builder.Default
        private Duration retryWaitDuration = Duration.ofSeconds(1);

        /**
         * Enable rate limiting
         */
        @Builder.Default
        private boolean rateLimiterEnabled = true;

        /**
         * Rate limiter permits per period
         */
        @Builder.Default
        private int limitForPeriod = 1000;

        /**
         * Rate limiter refresh period
         */
        @Builder.Default
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);

        /**
         * Rate limiter timeout duration
         */
        @Builder.Default
        private Duration timeoutDuration = Duration.ofSeconds(5);

        /**
         * Enable bulkhead pattern
         */
        @Builder.Default
        private boolean bulkheadEnabled = true;

        /**
         * Maximum concurrent calls
         */
        @Builder.Default
        private int maxConcurrentCalls = 1000;

        /**
         * Maximum wait time for bulkhead
         */
        @Builder.Default
        private Duration maxWaitDuration = Duration.ofSeconds(10);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityConfiguration {
        /**
         * Enable OAuth2 client credentials flow
         */
        @Builder.Default
        private boolean oauth2Enabled = true;

        /**
         * OAuth2 token endpoint
         */
        private String tokenEndpoint;

        /**
         * OAuth2 client ID
         */
        private String clientId;

        /**
         * OAuth2 client secret
         */
        private String clientSecret;

        /**
         * OAuth2 scope
         */
        private String scope;

        /**
         * Token cache duration
         */
        @Builder.Default
        private Duration tokenCacheDuration = Duration.ofMinutes(30);

        /**
         * Enable TLS/SSL
         */
        @Builder.Default
        private boolean tlsEnabled = true;

        /**
         * Trust store path
         */
        private String trustStorePath;

        /**
         * Trust store password
         */
        private String trustStorePassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsConfiguration {
        /**
         * Enable metrics collection
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * Metrics prefix
         */
        @Builder.Default
        private String prefix = "openfinance_rest_client";

        /**
         * Enable request/response timing
         */
        @Builder.Default
        private boolean timingEnabled = true;

        /**
         * Enable request/response size metrics
         */
        @Builder.Default
        private boolean sizeMetricsEnabled = true;

        /**
         * Enable detailed error metrics
         */
        @Builder.Default
        private boolean errorMetricsEnabled = true;
    }
}