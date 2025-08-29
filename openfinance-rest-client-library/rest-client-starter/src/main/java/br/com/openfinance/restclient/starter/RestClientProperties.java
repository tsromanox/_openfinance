package br.com.openfinance.restclient.starter;

import br.com.openfinance.restclient.core.client.RestClientStrategy;
import br.com.openfinance.restclient.core.config.ClientConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration properties for the REST client library.
 * 
 * Example usage in application.yml:
 * 
 * ```yaml
 * openfinance:
 *   rest-client:
 *     base-url: https://api.openfinance.org.br
 *     strategy: reactive
 *     max-connections: 1000
 *     connection-timeout: 10s
 *     read-timeout: 30s
 *     resilience:
 *       circuit-breaker-enabled: true
 *       failure-rate-threshold: 60.0
 *       retry-enabled: true
 *       max-retry-attempts: 3
 *     virtual-threads:
 *       enabled: true
 *       max-virtual-threads: 50000
 * ```
 */
@Data
@ConfigurationProperties(prefix = "openfinance.rest-client")
public class RestClientProperties {

    /**
     * Base URL for the API
     */
    private String baseUrl = "https://api.openfinance.org.br";

    /**
     * Client implementation strategy (REACTIVE or IMPERATIVE)
     */
    private RestClientStrategy strategy = RestClientStrategy.REACTIVE;

    /**
     * Connection timeout
     */
    private Duration connectionTimeout = Duration.ofSeconds(10);

    /**
     * Read timeout
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    /**
     * Write timeout
     */
    private Duration writeTimeout = Duration.ofSeconds(30);

    /**
     * Maximum connections in pool
     */
    private int maxConnections = 1000;

    /**
     * Maximum connections per route
     */
    private int maxConnectionsPerRoute = 100;

    /**
     * Connection idle timeout
     */
    private Duration idleTimeout = Duration.ofMinutes(2);

    /**
     * Keep alive timeout
     */
    private Duration keepAliveTimeout = Duration.ofMinutes(5);

    /**
     * Virtual Threads configuration
     */
    private VirtualThreadsProperties virtualThreads = new VirtualThreadsProperties();

    /**
     * Resilience configuration
     */
    private ResilienceProperties resilience = new ResilienceProperties();

    /**
     * Security configuration
     */
    private SecurityProperties security = new SecurityProperties();

    /**
     * Metrics configuration
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * Additional headers to include in all requests
     */
    private Map<String, String> defaultHeaders;

    /**
     * Convert to ClientConfiguration
     */
    public ClientConfiguration toClientConfiguration() {
        return ClientConfiguration.builder()
                .baseUrl(baseUrl)
                .strategy(strategy)
                .connectionTimeout(connectionTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .maxConnections(maxConnections)
                .maxConnectionsPerRoute(maxConnectionsPerRoute)
                .idleTimeout(idleTimeout)
                .keepAliveTimeout(keepAliveTimeout)
                .virtualThreads(virtualThreads.toVirtualThreadConfiguration())
                .resilience(resilience.toResilienceConfiguration())
                .security(security.toSecurityConfiguration())
                .metrics(metrics.toMetricsConfiguration())
                .defaultHeaders(defaultHeaders)
                .build();
    }

    @Data
    public static class VirtualThreadsProperties {
        private boolean enabled = true;
        private int maxVirtualThreads = 50000;
        private String threadNamePrefix = "rest-client-vt-";
        private boolean structuredConcurrency = true;

        public ClientConfiguration.VirtualThreadConfiguration toVirtualThreadConfiguration() {
            return ClientConfiguration.VirtualThreadConfiguration.builder()
                    .enabled(enabled)
                    .maxVirtualThreads(maxVirtualThreads)
                    .threadNamePrefix(threadNamePrefix)
                    .structuredConcurrency(structuredConcurrency)
                    .build();
        }
    }

    @Data
    public static class ResilienceProperties {
        private boolean circuitBreakerEnabled = true;
        private float failureRateThreshold = 60.0f;
        private int minimumNumberOfCalls = 10;
        private int slidingWindowSize = 100;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private boolean retryEnabled = true;
        private int maxRetryAttempts = 3;
        private Duration retryWaitDuration = Duration.ofSeconds(1);
        private boolean rateLimiterEnabled = true;
        private int limitForPeriod = 1000;
        private Duration limitRefreshPeriod = Duration.ofSeconds(1);
        private Duration timeoutDuration = Duration.ofSeconds(5);
        private boolean bulkheadEnabled = true;
        private int maxConcurrentCalls = 1000;
        private Duration maxWaitDuration = Duration.ofSeconds(10);

        public ClientConfiguration.ResilienceConfiguration toResilienceConfiguration() {
            return ClientConfiguration.ResilienceConfiguration.builder()
                    .circuitBreakerEnabled(circuitBreakerEnabled)
                    .failureRateThreshold(failureRateThreshold)
                    .minimumNumberOfCalls(minimumNumberOfCalls)
                    .slidingWindowSize(slidingWindowSize)
                    .waitDurationInOpenState(waitDurationInOpenState)
                    .retryEnabled(retryEnabled)
                    .maxRetryAttempts(maxRetryAttempts)
                    .retryWaitDuration(retryWaitDuration)
                    .rateLimiterEnabled(rateLimiterEnabled)
                    .limitForPeriod(limitForPeriod)
                    .limitRefreshPeriod(limitRefreshPeriod)
                    .timeoutDuration(timeoutDuration)
                    .bulkheadEnabled(bulkheadEnabled)
                    .maxConcurrentCalls(maxConcurrentCalls)
                    .maxWaitDuration(maxWaitDuration)
                    .build();
        }
    }

    @Data
    public static class SecurityProperties {
        private boolean oauth2Enabled = false;
        private String tokenEndpoint;
        private String clientId;
        private String clientSecret;
        private String scope;
        private Duration tokenCacheDuration = Duration.ofMinutes(30);
        private boolean tlsEnabled = true;
        private String trustStorePath;
        private String trustStorePassword;

        public ClientConfiguration.SecurityConfiguration toSecurityConfiguration() {
            return ClientConfiguration.SecurityConfiguration.builder()
                    .oauth2Enabled(oauth2Enabled)
                    .tokenEndpoint(tokenEndpoint)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .scope(scope)
                    .tokenCacheDuration(tokenCacheDuration)
                    .tlsEnabled(tlsEnabled)
                    .trustStorePath(trustStorePath)
                    .trustStorePassword(trustStorePassword)
                    .build();
        }
    }

    @Data
    public static class MetricsProperties {
        private boolean enabled = true;
        private String prefix = "openfinance_rest_client";
        private boolean timingEnabled = true;
        private boolean sizeMetricsEnabled = true;
        private boolean errorMetricsEnabled = true;

        public ClientConfiguration.MetricsConfiguration toMetricsConfiguration() {
            return ClientConfiguration.MetricsConfiguration.builder()
                    .enabled(enabled)
                    .prefix(prefix)
                    .timingEnabled(timingEnabled)
                    .sizeMetricsEnabled(sizeMetricsEnabled)
                    .errorMetricsEnabled(errorMetricsEnabled)
                    .build();
        }
    }
}