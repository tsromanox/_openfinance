package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Represents an API endpoint provided by an Open Finance resource.
 */
@Value
@Builder(toBuilder = true)
public class ApiEndpoint {
    
    String endpointId;
    String apiType;
    String apiVersion;
    String url;
    String method;
    EndpointStatus status;
    
    // Endpoint capabilities and features
    Set<String> supportedOperations;
    Map<String, Object> schema;
    String documentation;
    
    // Performance and monitoring
    EndpointMetrics metrics;
    EndpointLimits limits;
    
    // Security and authentication
    Set<String> authenticationMethods;
    Set<String> scopes;
    boolean requiresConsent;
    
    // Lifecycle information
    LocalDateTime lastTestedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    /**
     * Checks if the endpoint is currently operational.
     */
    public boolean isOperational() {
        return status == EndpointStatus.ACTIVE || status == EndpointStatus.DEGRADED;
    }
    
    /**
     * Checks if the endpoint supports a specific operation.
     */
    public boolean supportsOperation(String operation) {
        return supportedOperations != null && supportedOperations.contains(operation);
    }
    
    /**
     * Checks if the endpoint supports a specific authentication method.
     */
    public boolean supportsAuthMethod(String authMethod) {
        return authenticationMethods != null && authenticationMethods.contains(authMethod);
    }
    
    /**
     * Checks if the endpoint has been tested recently.
     */
    public boolean needsTesting(long maxAgeMinutes) {
        if (lastTestedAt == null) return true;
        
        return lastTestedAt.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }
    
    /**
     * Creates a copy with updated status.
     */
    public ApiEndpoint withStatus(EndpointStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy with updated test time.
     */
    public ApiEndpoint withUpdatedTestTime() {
        return this.toBuilder()
                .lastTestedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy with updated metrics.
     */
    public ApiEndpoint withMetrics(EndpointMetrics newMetrics) {
        return this.toBuilder()
                .metrics(newMetrics)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}