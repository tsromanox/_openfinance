package br.com.openfinance.application.service;

import br.com.openfinance.application.exception.ResourceNotFoundException;
import br.com.openfinance.application.port.input.ResourceUseCase;
import br.com.openfinance.application.port.output.ResourceDiscoveryService;
import br.com.openfinance.application.port.output.ResourceRepository;
import br.com.openfinance.service.resources.VirtualThreadResourceService;
import br.com.openfinance.service.resources.domain.Resource;
import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Application service implementing resource management use cases.
 * Orchestrates business logic for resource discovery, synchronization, 
 * validation, and monitoring operations.
 */
@Slf4j
@Service
@Transactional
public class ResourceService implements ResourceUseCase {
    
    private final ResourceRepository resourceRepository;
    private final ResourceDiscoveryService resourceDiscoveryService;
    private final VirtualThreadResourceService virtualThreadResourceService;
    
    public ResourceService(
            ResourceRepository resourceRepository,
            ResourceDiscoveryService resourceDiscoveryService,
            VirtualThreadResourceService virtualThreadResourceService) {
        this.resourceRepository = resourceRepository;
        this.resourceDiscoveryService = resourceDiscoveryService;
        this.virtualThreadResourceService = virtualThreadResourceService;
    }
    
    @Override
    public CompletableFuture<List<Resource>> discoverResources(List<String> discoveryEndpoints) {
        log.info("Starting resource discovery from {} endpoints", discoveryEndpoints.size());
        
        return virtualThreadResourceService.discoverResourcesAsync(discoveryEndpoints)
                .thenApply(resources -> {
                    // Save discovered resources
                    List<Resource> savedResources = resourceRepository.saveAll(resources);
                    
                    log.info("Successfully discovered and saved {} resources", savedResources.size());
                    return savedResources;
                })
                .exceptionally(e -> {
                    log.error("Resource discovery failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Resource discovery failed", e);
                });
    }
    
    @Override
    public CompletableFuture<ResourceSyncResult> synchronizeResources(List<String> resourceIds) {
        log.info("Starting resource synchronization for {} resources", resourceIds.size());
        
        return virtualThreadResourceService.syncResourcesAsync(resourceIds)
                .thenApply(syncResult -> {
                    // Update sync timestamps for successful resources
                    resourceIds.forEach(resourceId -> {
                        try {
                            resourceRepository.updateLastSyncAt(resourceId, LocalDateTime.now());
                        } catch (Exception e) {
                            log.warn("Failed to update sync timestamp for resource {}: {}", 
                                    resourceId, e.getMessage());
                        }
                    });
                    
                    log.info("Resource synchronization completed: {} synced, {} errors", 
                            syncResult.syncedCount(), syncResult.errorCount());
                    
                    return new ResourceSyncResult(
                            syncResult.syncedCount(),
                            syncResult.errorCount(),
                            syncResult.durationMs(),
                            syncResult.strategy(),
                            syncResult.batchSize(),
                            syncResult.concurrencyLevel(),
                            List.of() // Errors would need to be extracted from syncResult if available
                    );
                })
                .exceptionally(e -> {
                    log.error("Resource synchronization failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Resource synchronization failed", e);
                });
    }
    
    @Override
    public CompletableFuture<ResourceValidationResult> validateResources(List<String> resourceIds) {
        log.info("Starting resource validation for {} resources", resourceIds.size());
        
        return virtualThreadResourceService.validateResourcesAsync(resourceIds)
                .thenApply(validationResult -> {
                    // Update validation timestamps
                    resourceIds.forEach(resourceId -> {
                        try {
                            resourceRepository.updateLastValidatedAt(resourceId, LocalDateTime.now());
                        } catch (Exception e) {
                            log.warn("Failed to update validation timestamp for resource {}: {}", 
                                    resourceId, e.getMessage());
                        }
                    });
                    
                    log.info("Resource validation completed: {} validated, {} errors", 
                            validationResult.validatedCount(), validationResult.errorCount());
                    
                    List<ValidationError> validationErrors = validationResult.validationResults()
                            .stream()
                            .map(summary -> new ValidationError(
                                    summary.resourceId(),
                                    summary.isValid(),
                                    summary.message(),
                                    summary.isValid() ? "SUCCESS" : "VALIDATION_ERROR"
                            ))
                            .toList();
                    
                    return new ResourceValidationResult(
                            validationResult.validatedCount(),
                            validationResult.errorCount(),
                            validationResult.durationMs(),
                            validationErrors
                    );
                })
                .exceptionally(e -> {
                    log.error("Resource validation failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Resource validation failed", e);
                });
    }
    
    @Override
    public CompletableFuture<ResourceHealthResult> monitorResourceHealth(List<String> resourceIds) {
        log.info("Starting resource health monitoring for {} resources", resourceIds.size());
        
        return virtualThreadResourceService.monitorResourceHealthAsync(resourceIds)
                .thenApply(healthResult -> {
                    // Update monitoring timestamps
                    resourceIds.forEach(resourceId -> {
                        try {
                            resourceRepository.updateLastMonitoredAt(resourceId, LocalDateTime.now());
                        } catch (Exception e) {
                            log.warn("Failed to update monitoring timestamp for resource {}: {}", 
                                    resourceId, e.getMessage());
                        }
                    });
                    
                    log.info("Resource health monitoring completed: {} monitored, {} errors", 
                            healthResult.monitoredCount(), healthResult.errorCount());
                    
                    List<HealthStatus> healthStatuses = healthResult.healthResults()
                            .stream()
                            .map(health -> new HealthStatus(
                                    health.getResourceId(),
                                    health.isHealthy(),
                                    health.getHealthScore(),
                                    health.getAverageResponseTime(),
                                    health.getStatus().name()
                            ))
                            .toList();
                    
                    return new ResourceHealthResult(
                            healthResult.monitoredCount(),
                            healthResult.errorCount(),
                            healthResult.durationMs(),
                            healthStatuses
                    );
                })
                .exceptionally(e -> {
                    log.error("Resource health monitoring failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Resource health monitoring failed", e);
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Resource getResource(String resourceId) {
        log.debug("Getting resource: {}", resourceId);
        
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
    }
    
    @Override
    public Resource updateResourceStatus(String resourceId, ResourceStatus newStatus) {
        log.info("Updating resource {} status to {}", resourceId, newStatus);
        
        Resource resource = getResource(resourceId);
        Resource updatedResource = resource.withStatus(newStatus);
        
        return resourceRepository.save(updatedResource);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResourcesByType(ResourceType type, ResourceStatus status) {
        log.debug("Getting resources by type {} and status {}", type, status);
        
        return resourceRepository.findByTypeAndStatus(type, status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResourcesByOrganization(String organizationId) {
        log.debug("Getting resources for organization: {}", organizationId);
        
        return resourceRepository.findByOrganizationId(organizationId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResourcesNeedingSync() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        
        log.debug("Getting resources needing sync (threshold: {})", threshold);
        
        return resourceRepository.findResourcesNeedingSync(threshold);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResourcesNeedingValidation() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        
        log.debug("Getting resources needing validation (threshold: {})", threshold);
        
        return resourceRepository.findResourcesNeedingValidation(threshold);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Resource> getResourcesNeedingMonitoring() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        
        log.debug("Getting resources needing monitoring (threshold: {})", threshold);
        
        return resourceRepository.findResourcesNeedingMonitoring(threshold);
    }
}