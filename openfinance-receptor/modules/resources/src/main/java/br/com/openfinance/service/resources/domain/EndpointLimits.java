package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Rate limits and constraints for an API endpoint.
 */
@Value
@Builder(toBuilder = true)
public class EndpointLimits {
    
    // Rate limiting
    int requestsPerSecond;
    int requestsPerMinute;
    int requestsPerHour;
    int requestsPerDay;
    
    // Timeout settings
    Duration connectTimeout;
    Duration readTimeout;
    Duration totalTimeout;
    
    // Data limits
    long maxRequestSize;     // bytes
    long maxResponseSize;    // bytes
    
    // Concurrent request limits
    int maxConcurrentRequests;
    int maxConnectionsPerHost;
    
    // Retry settings
    int maxRetries;
    Duration retryBackoff;
    
    /**
     * Determines if a request rate is within limits.
     */
    public boolean isWithinRateLimit(double currentRequestsPerSecond) {
        return requestsPerSecond <= 0 || currentRequestsPerSecond <= requestsPerSecond;
    }
    
    /**
     * Determines if a request size is within limits.
     */
    public boolean isRequestSizeAllowed(long requestSize) {
        return maxRequestSize <= 0 || requestSize <= maxRequestSize;
    }
    
    /**
     * Determines if a response size is within limits.
     */
    public boolean isResponseSizeAllowed(long responseSize) {
        return maxResponseSize <= 0 || responseSize <= maxResponseSize;
    }
    
    /**
     * Gets the effective timeout for requests.
     */
    public Duration getEffectiveTimeout() {
        if (totalTimeout != null) return totalTimeout;
        if (readTimeout != null) return readTimeout;
        return Duration.ofSeconds(30); // Default
    }
    
    /**
     * Gets the daily request limit.
     */
    public int getDailyLimit() {
        if (requestsPerDay > 0) return requestsPerDay;
        if (requestsPerHour > 0) return requestsPerHour * 24;
        if (requestsPerMinute > 0) return requestsPerMinute * 60 * 24;
        if (requestsPerSecond > 0) return requestsPerSecond * 60 * 60 * 24;
        return Integer.MAX_VALUE; // No limit
    }
    
    /**
     * Creates limits with conservative defaults.
     */
    public static EndpointLimits defaultLimits() {
        return EndpointLimits.builder()
                .requestsPerSecond(10)
                .requestsPerMinute(300)
                .requestsPerHour(1000)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .totalTimeout(Duration.ofSeconds(60))
                .maxRequestSize(1024 * 1024)      // 1MB
                .maxResponseSize(10 * 1024 * 1024) // 10MB
                .maxConcurrentRequests(10)
                .maxConnectionsPerHost(5)
                .maxRetries(3)
                .retryBackoff(Duration.ofSeconds(1))
                .build();
    }
    
    /**
     * Creates limits for high-performance scenarios.
     */
    public static EndpointLimits highPerformanceLimits() {
        return EndpointLimits.builder()
                .requestsPerSecond(100)
                .requestsPerMinute(3000)
                .requestsPerHour(10000)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(10))
                .totalTimeout(Duration.ofSeconds(30))
                .maxRequestSize(5 * 1024 * 1024)   // 5MB
                .maxResponseSize(50 * 1024 * 1024) // 50MB
                .maxConcurrentRequests(50)
                .maxConnectionsPerHost(20)
                .maxRetries(2)
                .retryBackoff(Duration.ofMillis(500))
                .build();
    }
}