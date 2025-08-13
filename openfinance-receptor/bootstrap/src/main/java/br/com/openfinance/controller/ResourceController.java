package br.com.openfinance.controller;

import br.com.openfinance.application.dto.ResourceResponse;
import br.com.openfinance.application.service.ResourceService;
import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main REST controller for OpenFinance Receptor resource operations.
 * Provides high-level API endpoints for resource management with Virtual Threads.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    
    private final ResourceService resourceService;
    private final ResourcePerformanceMonitor performanceMonitor;
    private final AdaptiveResourceResourceManager resourceManager;
    
    public ResourceController(
            ResourceService resourceService,
            ResourcePerformanceMonitor performanceMonitor,
            AdaptiveResourceResourceManager resourceManager) {
        this.resourceService = resourceService;
        this.performanceMonitor = performanceMonitor;
        this.resourceManager = resourceManager;
    }
    
    /**
     * Discover resources from Open Finance Brasil directories.
     */
    @PostMapping("/discover")
    @Timed(name = "resource.discover", description = "Resource discovery operation")
    public CompletableFuture<ResponseEntity<ResourceResponse.ResourceDiscoveryResponse>> discoverResources(
            @RequestBody List<String> discoveryEndpoints) {
        
        log.info("Resource discovery requested from {} endpoints", discoveryEndpoints.size());
        
        return resourceService.discoverResources(discoveryEndpoints)
                .thenApply(resources -> {
                    var response = new ResourceResponse.ResourceDiscoveryResponse(
                            resources.size(),
                            0, // Duration would be calculated
                            "VIRTUAL_THREADS",
                            true,
                            "Resource discovery completed successfully",
                            resources.stream()
                                    .map(resource -> new ResourceResponse.ResourceInfo(
                                            resource.getResourceId(),
                                            resource.getOrganizationId(),
                                            resource.getOrganizationName(),
                                            resource.getCnpj(),
                                            resource.getType(),
                                            resource.getStatus(),
                                            resource.getDiscoveredAt(),
                                            resource.getLastSyncedAt(),
                                            resource.getLastValidatedAt(),
                                            resource.getLastMonitoredAt()
                                    ))
                                    .toList()
                    );
                    
                    log.info("Resource discovery completed: {} resources discovered", resources.size());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource discovery failed: {}", e.getMessage());
                    
                    var response = new ResourceResponse.ResourceDiscoveryResponse(
                            0, 0, "FAILED", false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Synchronize resources with external APIs.
     */
    @PostMapping("/sync")
    @Timed(name = "resource.sync", description = "Resource synchronization operation")
    public CompletableFuture<ResponseEntity<ResourceResponse.ResourceSyncResponse>> syncResources(
            @RequestBody List<String> resourceIds) {
        
        log.info("Resource synchronization requested for {} resources", resourceIds.size());
        
        return resourceService.synchronizeResources(resourceIds)
                .thenApply(result -> {
                    var response = new ResourceResponse.ResourceSyncResponse(
                            result.syncedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            result.strategy(),
                            result.batchSize(),
                            result.concurrencyLevel(),
                            true,
                            "Resource synchronization completed successfully",
                            result.errors().stream().map(error -> error.errorMessage()).toList()
                    );
                    
                    log.info("Resource synchronization completed: {} synced, {} errors", 
                            result.syncedCount(), result.errorCount());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource synchronization failed: {}", e.getMessage());
                    
                    var response = new ResourceResponse.ResourceSyncResponse(
                            0, 0, 0, "FAILED", 0, 0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Validate resources.
     */
    @PostMapping("/validate")
    @Timed(name = "resource.validate", description = "Resource validation operation")
    public CompletableFuture<ResponseEntity<ResourceResponse.ResourceValidationResponse>> validateResources(
            @RequestBody List<String> resourceIds) {
        
        log.info("Resource validation requested for {} resources", resourceIds.size());
        
        return resourceService.validateResources(resourceIds)
                .thenApply(result -> {
                    var response = new ResourceResponse.ResourceValidationResponse(
                            result.validatedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            true,
                            "Resource validation completed successfully",
                            result.validationResults().stream()
                                    .map(validation -> new ResourceResponse.ValidationResult(
                                            validation.resourceId(),
                                            validation.isValid(),
                                            validation.validationMessage(),
                                            validation.errorType(),
                                            java.time.LocalDateTime.now()
                                    ))
                                    .toList()
                    );
                    
                    log.info("Resource validation completed: {} validated, {} errors", 
                            result.validatedCount(), result.errorCount());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource validation failed: {}", e.getMessage());
                    
                    var response = new ResourceResponse.ResourceValidationResponse(
                            0, 0, 0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Monitor resource health.
     */
    @PostMapping("/monitor")
    @Timed(name = "resource.monitor", description = "Resource health monitoring operation")
    public CompletableFuture<ResponseEntity<ResourceResponse.ResourceHealthResponse>> monitorResources(
            @RequestBody List<String> resourceIds) {
        
        log.info("Resource health monitoring requested for {} resources", resourceIds.size());
        
        return resourceService.monitorResourceHealth(resourceIds)
                .thenApply(result -> {
                    var response = new ResourceResponse.ResourceHealthResponse(
                            result.monitoredCount(),
                            result.errorCount(),
                            result.durationMs(),
                            true,
                            "Resource health monitoring completed successfully",
                            result.healthResults().stream()
                                    .map(health -> new ResourceResponse.HealthResult(
                                            health.resourceId(),
                                            health.isHealthy(),
                                            health.healthScore(),
                                            health.responseTime(),
                                            0.0, // uptime would be calculated
                                            0.0, // error rate would be calculated
                                            health.statusMessage(),
                                            java.time.LocalDateTime.now()
                                    ))
                                    .toList()
                    );
                    
                    log.info("Resource health monitoring completed: {} monitored, {} errors", 
                            result.monitoredCount(), result.errorCount());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource health monitoring failed: {}", e.getMessage());
                    
                    var response = new ResourceResponse.ResourceHealthResponse(
                            0, 0, 0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Get resource by ID.
     */
    @GetMapping("/{resourceId}")
    @Timed(name = "resource.get", description = "Get resource by ID")
    public ResponseEntity<ResourceResponse.ResourceInfo> getResource(@PathVariable String resourceId) {
        try {
            log.debug("Getting resource: {}", resourceId);
            
            var resource = resourceService.getResource(resourceId);
            
            var response = new ResourceResponse.ResourceInfo(
                    resource.getResourceId(),
                    resource.getOrganizationId(),
                    resource.getOrganizationName(),
                    resource.getCnpj(),
                    resource.getType(),
                    resource.getStatus(),
                    resource.getDiscoveredAt(),
                    resource.getLastSyncedAt(),
                    resource.getLastValidatedAt(),
                    resource.getLastMonitoredAt()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting resource {}: {}", resourceId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Search resources by type and status.
     */
    @GetMapping("/search")
    @Timed(name = "resource.search", description = "Search resources")
    public ResponseEntity<List<ResourceResponse.ResourceInfo>> searchResources(
            @RequestParam ResourceType type,
            @RequestParam(required = false) ResourceStatus status) {
        
        log.debug("Searching resources by type {} and status {}", type, status);
        
        ResourceStatus searchStatus = status != null ? status : ResourceStatus.ACTIVE;
        
        var resources = resourceService.getResourcesByType(type, searchStatus);
        
        var response = resources.stream()
                .map(resource -> new ResourceResponse.ResourceInfo(
                        resource.getResourceId(),
                        resource.getOrganizationId(),
                        resource.getOrganizationName(),
                        resource.getCnpj(),
                        resource.getType(),
                        resource.getStatus(),
                        resource.getDiscoveredAt(),
                        resource.getLastSyncedAt(),
                        resource.getLastValidatedAt(),
                        resource.getLastMonitoredAt()
                ))
                .toList();
        
        log.debug("Resource search completed: {} resources found", response.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get performance metrics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<ResourceResponse.ResourcePerformanceResponse> getPerformanceMetrics() {
        var report = performanceMonitor.getPerformanceReport();
        var recommendations = performanceMonitor.getRecommendations();
        
        var response = new ResourceResponse.ResourcePerformanceResponse(
                report.totalResourcesDiscovered(),
                report.totalResourcesSynced(),
                report.totalResourcesValidated(),
                report.totalResourcesMonitored(),
                report.totalBatchesProcessed(),
                report.totalErrors(),
                report.currentThroughput(),
                report.processingEfficiency(),
                report.activeVirtualThreads(),
                report.concurrentResourceOperations(),
                report.errorRate(),
                recommendations.optimizationSuggestions()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get resource utilization.
     */
    @GetMapping("/utilization")
    public ResponseEntity<ResourceResponse.ResourceUtilizationResponse> getResourceUtilization() {
        var utilization = resourceManager.getResourceUtilization();
        
        var response = new ResourceResponse.ResourceUtilizationResponse(
                utilization.activeResourceDiscoveryTasks(),
                utilization.activeResourceSyncTasks(),
                utilization.activeResourceValidationTasks(),
                utilization.activeResourceMonitoringTasks(),
                utilization.activeApiCalls(),
                utilization.activeBatchProcessingTasks(),
                utilization.currentCpuUsage(),
                utilization.currentMemoryUsage(),
                resourceManager.getDynamicBatchSize(),
                resourceManager.getDynamicConcurrencyLevel(),
                "ADAPTIVE"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        var utilization = resourceManager.getResourceUtilization();
        var report = performanceMonitor.getPerformanceReport();
        
        boolean healthy = utilization.currentCpuUsage() < 0.90 
                && utilization.currentMemoryUsage() < 0.90
                && report.errorRate() < 0.20;
        
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "cpuUsage", String.format("%.2f%%", utilization.currentCpuUsage() * 100),
                "memoryUsage", String.format("%.2f%%", utilization.currentMemoryUsage() * 100),
                "errorRate", String.format("%.2f%%", report.errorRate() * 100),
                "throughput", String.format("%.2f ops/sec", report.currentThroughput()),
                "activeVirtualThreads", report.activeVirtualThreads(),
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}