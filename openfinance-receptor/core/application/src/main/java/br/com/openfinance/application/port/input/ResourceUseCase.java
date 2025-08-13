package br.com.openfinance.application.port.input;

import br.com.openfinance.service.resources.domain.Resource;
import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Use case interface for resource management operations.
 * Provides high-level business operations for Open Finance Brasil resource discovery,
 * synchronization, validation, and monitoring.
 */
public interface ResourceUseCase {
    
    /**
     * Discover resources from Open Finance Brasil directory endpoints.
     */
    CompletableFuture<List<Resource>> discoverResources(List<String> discoveryEndpoints);
    
    /**
     * Synchronize resource data from Open Finance institutions.
     */
    CompletableFuture<ResourceSyncResult> synchronizeResources(List<String> resourceIds);
    
    /**
     * Validate resource endpoints and data quality.
     */
    CompletableFuture<ResourceValidationResult> validateResources(List<String> resourceIds);
    
    /**
     * Monitor resource health and availability.
     */
    CompletableFuture<ResourceHealthResult> monitorResourceHealth(List<String> resourceIds);
    
    /**
     * Get resource by ID.
     */
    Resource getResource(String resourceId);
    
    /**
     * Update resource status.
     */
    Resource updateResourceStatus(String resourceId, ResourceStatus newStatus);
    
    /**
     * Get resources by type and status.
     */
    List<Resource> getResourcesByType(ResourceType type, ResourceStatus status);
    
    /**
     * Get all resources for a specific organization.
     */
    List<Resource> getResourcesByOrganization(String organizationId);
    
    /**
     * Get resources that need synchronization.
     */
    List<Resource> getResourcesNeedingSync();
    
    /**
     * Get resources that need validation.
     */
    List<Resource> getResourcesNeedingValidation();
    
    /**
     * Get resources that need health monitoring.
     */
    List<Resource> getResourcesNeedingMonitoring();
    
    // Result record classes
    
    record ResourceSyncResult(
            int syncedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel,
            List<SyncError> errors
    ) {}
    
    record ResourceValidationResult(
            int validatedCount,
            int errorCount,
            long durationMs,
            List<ValidationError> validationResults
    ) {}
    
    record ResourceHealthResult(
            int monitoredCount,
            int errorCount,
            long durationMs,
            List<HealthStatus> healthResults
    ) {}
    
    record SyncError(
            String resourceId,
            String errorMessage,
            String errorType
    ) {}
    
    record ValidationError(
            String resourceId,
            boolean isValid,
            String validationMessage,
            String errorType
    ) {}
    
    record HealthStatus(
            String resourceId,
            boolean isHealthy,
            double healthScore,
            long responseTime,
            String statusMessage
    ) {}
}