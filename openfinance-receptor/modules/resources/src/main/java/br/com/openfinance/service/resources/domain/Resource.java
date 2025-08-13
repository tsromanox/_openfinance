package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an Open Finance Brasil resource that provides financial data endpoints.
 * This includes banks, financial institutions, and their available APIs.
 */
@Value
@Builder(toBuilder = true)
public class Resource {
    
    String resourceId;
    String organizationId;
    String organizationName;
    String cnpj;
    ResourceType type;
    ResourceStatus status;
    
    // API endpoints information
    List<ApiEndpoint> endpoints;
    Set<String> supportedApiVersions;
    Set<ResourceCapability> capabilities;
    
    // Discovery and metadata
    String discoveryUrl;
    Map<String, Object> metadata;
    ResourceConfiguration configuration;
    
    // Monitoring and health
    ResourceHealth health;
    List<ResourceMetric> metrics;
    
    // Lifecycle information
    LocalDateTime discoveredAt;
    LocalDateTime lastSyncedAt;
    LocalDateTime lastValidatedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    /**
     * Validates if the resource is currently available and operational.
     */
    public boolean isAvailable() {
        return status == ResourceStatus.ACTIVE && 
               health != null && 
               health.isHealthy();
    }
    
    /**
     * Checks if the resource supports a specific API capability.
     */
    public boolean supportsCapability(ResourceCapability capability) {
        return capabilities != null && capabilities.contains(capability);
    }
    
    /**
     * Gets the endpoint for a specific API type.
     */
    public ApiEndpoint getEndpoint(String apiType) {
        if (endpoints == null) return null;
        
        return endpoints.stream()
                .filter(endpoint -> apiType.equals(endpoint.getApiType()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Checks if the resource needs synchronization based on last sync time.
     */
    public boolean needsSynchronization(long maxAgeMinutes) {
        if (lastSyncedAt == null) return true;
        
        return lastSyncedAt.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }
    
    /**
     * Checks if the resource needs validation based on last validation time.
     */
    public boolean needsValidation(long maxAgeMinutes) {
        if (lastValidatedAt == null) return true;
        
        return lastValidatedAt.isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes));
    }
    
    /**
     * Creates a copy of this resource with updated health information.
     */
    public Resource withUpdatedHealth(ResourceHealth newHealth) {
        return this.toBuilder()
                .health(newHealth)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy of this resource with updated sync time.
     */
    public Resource withUpdatedSyncTime() {
        return this.toBuilder()
                .lastSyncedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy of this resource with updated validation time.
     */
    public Resource withUpdatedValidationTime() {
        return this.toBuilder()
                .lastValidatedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy of this resource with updated status.
     */
    public Resource withStatus(ResourceStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}