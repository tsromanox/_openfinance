package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Configuration settings for resource monitoring and interaction.
 */
@Value
@Builder(toBuilder = true)
public class ResourceConfiguration {
    
    // Discovery settings
    Duration discoveryInterval;
    boolean autoDiscoveryEnabled;
    Set<String> discoveryEndpoints;
    
    // Synchronization settings
    Duration syncInterval;
    boolean autoSyncEnabled;
    int maxConcurrentSyncs;
    
    // Validation settings
    Duration validationInterval;
    boolean autoValidationEnabled;
    Set<String> validationChecks;
    
    // Health monitoring settings
    Duration healthCheckInterval;
    boolean healthMonitoringEnabled;
    int healthCheckTimeout;
    double healthThreshold;
    
    // Performance settings
    boolean performanceMonitoringEnabled;
    Duration metricsRetentionPeriod;
    int maxMetricsHistory;
    
    // Security settings
    Map<String, String> authenticationConfig;
    Set<String> allowedCertificates;
    boolean tlsVerificationEnabled;
    
    // Rate limiting and throttling
    boolean rateLimitingEnabled;
    int defaultRateLimit;
    Duration rateLimitWindow;
    
    // Retry and circuit breaker settings
    int maxRetries;
    Duration retryBackoff;
    boolean circuitBreakerEnabled;
    double circuitBreakerThreshold;
    Duration circuitBreakerTimeout;
    
    // Custom configuration
    Map<String, Object> customSettings;
    
    /**
     * Gets the effective discovery interval.
     */
    public Duration getEffectiveDiscoveryInterval() {
        return discoveryInterval != null ? discoveryInterval : Duration.ofHours(6);
    }
    
    /**
     * Gets the effective sync interval.
     */
    public Duration getEffectiveSyncInterval() {
        return syncInterval != null ? syncInterval : Duration.ofHours(1);
    }
    
    /**
     * Gets the effective validation interval.
     */
    public Duration getEffectiveValidationInterval() {
        return validationInterval != null ? validationInterval : Duration.ofMinutes(30);
    }
    
    /**
     * Gets the effective health check interval.
     */
    public Duration getEffectiveHealthCheckInterval() {
        return healthCheckInterval != null ? healthCheckInterval : Duration.ofMinutes(5);
    }
    
    /**
     * Determines if a validation check is enabled.
     */
    public boolean isValidationCheckEnabled(String checkName) {
        return validationChecks == null || validationChecks.contains(checkName);
    }
    
    /**
     * Gets a custom setting value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomSetting(String key, Class<T> type, T defaultValue) {
        if (customSettings == null) return defaultValue;
        
        Object value = customSettings.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Creates a default configuration.
     */
    public static ResourceConfiguration defaultConfiguration() {
        return ResourceConfiguration.builder()
                .discoveryInterval(Duration.ofHours(6))
                .autoDiscoveryEnabled(true)
                .syncInterval(Duration.ofHours(1))
                .autoSyncEnabled(true)
                .maxConcurrentSyncs(10)
                .validationInterval(Duration.ofMinutes(30))
                .autoValidationEnabled(true)
                .healthCheckInterval(Duration.ofMinutes(5))
                .healthMonitoringEnabled(true)
                .healthCheckTimeout(10000) // 10 seconds
                .healthThreshold(0.8)
                .performanceMonitoringEnabled(true)
                .metricsRetentionPeriod(Duration.ofDays(7))
                .maxMetricsHistory(1000)
                .rateLimitingEnabled(true)
                .defaultRateLimit(100)
                .rateLimitWindow(Duration.ofMinutes(1))
                .maxRetries(3)
                .retryBackoff(Duration.ofSeconds(2))
                .circuitBreakerEnabled(true)
                .circuitBreakerThreshold(0.5)
                .circuitBreakerTimeout(Duration.ofSeconds(30))
                .tlsVerificationEnabled(true)
                .build();
    }
    
    /**
     * Creates a high-performance configuration.
     */
    public static ResourceConfiguration highPerformanceConfiguration() {
        return ResourceConfiguration.builder()
                .discoveryInterval(Duration.ofHours(2))
                .autoDiscoveryEnabled(true)
                .syncInterval(Duration.ofMinutes(30))
                .autoSyncEnabled(true)
                .maxConcurrentSyncs(50)
                .validationInterval(Duration.ofMinutes(10))
                .autoValidationEnabled(true)
                .healthCheckInterval(Duration.ofMinutes(2))
                .healthMonitoringEnabled(true)
                .healthCheckTimeout(5000) // 5 seconds
                .healthThreshold(0.9)
                .performanceMonitoringEnabled(true)
                .metricsRetentionPeriod(Duration.ofDays(30))
                .maxMetricsHistory(10000)
                .rateLimitingEnabled(true)
                .defaultRateLimit(1000)
                .rateLimitWindow(Duration.ofMinutes(1))
                .maxRetries(2)
                .retryBackoff(Duration.ofMillis(500))
                .circuitBreakerEnabled(true)
                .circuitBreakerThreshold(0.3)
                .circuitBreakerTimeout(Duration.ofSeconds(10))
                .tlsVerificationEnabled(true)
                .build();
    }
}