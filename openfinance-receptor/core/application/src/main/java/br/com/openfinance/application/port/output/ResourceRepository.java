package br.com.openfinance.application.port.output;

import br.com.openfinance.service.resources.domain.Resource;
import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for resource persistence operations.
 * Defines the contract for storing and retrieving Open Finance Brasil resources.
 */
public interface ResourceRepository {
    
    /**
     * Save a resource.
     */
    Resource save(Resource resource);
    
    /**
     * Save multiple resources in batch.
     */
    List<Resource> saveAll(List<Resource> resources);
    
    /**
     * Find resource by ID.
     */
    Optional<Resource> findById(String resourceId);
    
    /**
     * Find resources by organization ID.
     */
    List<Resource> findByOrganizationId(String organizationId);
    
    /**
     * Find resources by type and status.
     */
    List<Resource> findByTypeAndStatus(ResourceType type, ResourceStatus status);
    
    /**
     * Find resources by status.
     */
    List<Resource> findByStatus(ResourceStatus status);
    
    /**
     * Find resources by type.
     */
    List<Resource> findByType(ResourceType type);
    
    /**
     * Find all resources.
     */
    List<Resource> findAll();
    
    /**
     * Find resources that need synchronization (last sync older than threshold).
     */
    List<Resource> findResourcesNeedingSync(LocalDateTime threshold);
    
    /**
     * Find resources that need validation (last validation older than threshold).
     */
    List<Resource> findResourcesNeedingValidation(LocalDateTime threshold);
    
    /**
     * Find resources that need health monitoring (last check older than threshold).
     */
    List<Resource> findResourcesNeedingMonitoring(LocalDateTime threshold);
    
    /**
     * Find resources discovered after a specific date.
     */
    List<Resource> findResourcesDiscoveredAfter(LocalDateTime date);
    
    /**
     * Find resources by organization CNPJ.
     */
    List<Resource> findByOrganizationCnpj(String cnpj);
    
    /**
     * Count resources by status.
     */
    long countByStatus(ResourceStatus status);
    
    /**
     * Count resources by type.
     */
    long countByType(ResourceType type);
    
    /**
     * Delete resource by ID.
     */
    void deleteById(String resourceId);
    
    /**
     * Check if resource exists by ID.
     */
    boolean existsById(String resourceId);
    
    /**
     * Update resource status.
     */
    void updateStatus(String resourceId, ResourceStatus status);
    
    /**
     * Update last sync timestamp.
     */
    void updateLastSyncAt(String resourceId, LocalDateTime lastSyncAt);
    
    /**
     * Update last validation timestamp.
     */
    void updateLastValidatedAt(String resourceId, LocalDateTime lastValidatedAt);
    
    /**
     * Update last monitoring timestamp.
     */
    void updateLastMonitoredAt(String resourceId, LocalDateTime lastMonitoredAt);
}