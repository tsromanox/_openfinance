package br.com.openfinance.service.resources.controller;

import br.com.openfinance.service.resources.VirtualThreadResourceService;
import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Virtual Thread optimized resource processing operations.
 * Provides comprehensive resource management APIs with high-performance processing
 * for Open Finance Brasil resources.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resources")
public class VirtualThreadResourceController {
    
    private final VirtualThreadResourceService resourceService;
    private final ResourcePerformanceMonitor performanceMonitor;
    private final AdaptiveResourceResourceManager resourceManager;
    
    public VirtualThreadResourceController(
            VirtualThreadResourceService resourceService,
            ResourcePerformanceMonitor performanceMonitor,
            AdaptiveResourceResourceManager resourceManager) {
        this.resourceService = resourceService;
        this.performanceMonitor = performanceMonitor;
        this.resourceManager = resourceManager;
    }
    
    /**
     * Discover resources from multiple endpoints.
     */
    @PostMapping("/discover")
    @Timed(name = "resources.discover", description = "Time taken to discover resources")
    public CompletableFuture<ResponseEntity<ResourceDiscoveryResponse>> discoverResources(
            @RequestBody ResourceDiscoveryRequest request) {
        
        log.info("Starting resource discovery from {} endpoints", request.discoveryEndpoints().size());
        
        return resourceService.discoverResourcesAsync(request.discoveryEndpoints())
                .thenApply(resources -> {
                    var response = new ResourceDiscoveryResponse(
                            resources.size(),
                            true,
                            "Resource discovery completed successfully",
                            resources.stream().map(resource -> new ResourceSummary(
                                    resource.getResourceId(),
                                    resource.getOrganizationName(),
                                    resource.getType(),
                                    resource.getStatus(),
                                    resource.getDiscoveredAt()
                            )).toList()
                    );
                    
                    log.info("Resource discovery completed: {} resources discovered", resources.size());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource discovery failed: {}", e.getMessage());
                    
                    var response = new ResourceDiscoveryResponse(
                            0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Synchronize resources with adaptive batching.
     */
    @PostMapping("/sync")
    @Timed(name = "resources.sync", description = "Time taken to synchronize resources")
    public CompletableFuture<ResponseEntity<ResourceSyncResponse>> syncResources(
            @RequestBody ResourceSyncRequest request) {
        
        log.info("Starting resource synchronization for {} resources", request.resourceIds().size());
        
        return resourceService.syncResourcesAsync(request.resourceIds())
                .thenApply(result -> {
                    var response = new ResourceSyncResponse(
                            result.syncedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            result.strategy(),
                            result.batchSize(),
                            result.concurrencyLevel(),
                            true,
                            "Resource synchronization completed successfully"
                    );
                    
                    log.info("Resource sync completed: {} synced, {} errors in {}ms", 
                            result.syncedCount(), result.errorCount(), result.durationMs());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource sync failed: {}", e.getMessage());
                    
                    var response = new ResourceSyncResponse(
                            0, 0, 0, "FAILED", 0, 0, false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Validate resources in parallel.
     */
    @PostMapping("/validate")
    @Timed(name = "resources.validate", description = "Time taken to validate resources")
    public CompletableFuture<ResponseEntity<ResourceValidationResponse>> validateResources(
            @RequestBody ResourceValidationRequest request) {
        
        log.info("Starting resource validation for {} resources", request.resourceIds().size());
        
        return resourceService.validateResourcesAsync(request.resourceIds())
                .thenApply(result -> {
                    var response = new ResourceValidationResponse(
                            result.validatedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            true,
                            "Resource validation completed successfully",
                            result.validationResults()
                    );
                    
                    log.info("Resource validation completed: {} validated, {} errors in {}ms", 
                            result.validatedCount(), result.errorCount(), result.durationMs());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource validation failed: {}", e.getMessage());
                    
                    var response = new ResourceValidationResponse(
                            0, 0, 0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Monitor resource health in parallel.
     */
    @PostMapping("/health/monitor")
    @Timed(name = "resources.health.monitor", description = "Time taken to monitor resource health")
    public CompletableFuture<ResponseEntity<ResourceHealthMonitoringResponse>> monitorResourceHealth(
            @RequestBody ResourceHealthMonitoringRequest request) {
        
        log.info("Starting resource health monitoring for {} resources", request.resourceIds().size());
        
        return resourceService.monitorResourceHealthAsync(request.resourceIds())
                .thenApply(result -> {
                    var response = new ResourceHealthMonitoringResponse(
                            result.monitoredCount(),
                            result.errorCount(),
                            result.durationMs(),
                            true,
                            "Resource health monitoring completed successfully",
                            result.healthResults().stream().map(health -> new HealthSummary(
                                    health.getStatus().getDisplayName(),
                                    health.getHealthScore(),
                                    health.getAverageResponseTime(),
                                    health.getUptime(),
                                    health.getErrorRate(),
                                    health.getLastCheckAt()
                            )).toList()
                    );
                    
                    log.info("Resource health monitoring completed: {} monitored, {} errors in {}ms", 
                            result.monitoredCount(), result.errorCount(), result.durationMs());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource health monitoring failed: {}", e.getMessage());
                    
                    var response = new ResourceHealthMonitoringResponse(
                            0, 0, 0, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Process resources with massive parallel processing.
     */
    @PostMapping("/process/massive")
    @Timed(name = "resources.process.massive", description = "Time taken for massive resource processing")
    public CompletableFuture<ResponseEntity<MassiveProcessingResponse>> processMassiveWorkload(
            @RequestBody MassiveProcessingRequest request) {
        
        log.info("Starting massive resource processing: {} resources with {} concurrency", 
                request.resourceIds().size(), request.targetConcurrency());
        
        return resourceService.processMassiveResourceWorkload(request.resourceIds(), request.targetConcurrency())
                .thenApply(result -> {
                    var response = new MassiveProcessingResponse(
                            result.processedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            result.strategy(),
                            request.targetConcurrency(),
                            calculateThroughput(result.processedCount(), result.durationMs()),
                            true,
                            "Massive processing completed successfully"
                    );
                    
                    log.info("Massive processing completed: {} processed, {} errors in {}ms " +
                            "({:.2f} ops/second)", 
                            result.processedCount(), result.errorCount(), result.durationMs(),
                            calculateThroughput(result.processedCount(), result.durationMs()));
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Massive processing failed: {}", e.getMessage());
                    
                    var response = new MassiveProcessingResponse(
                            0, 0, 0, "FAILED", 0, 0.0, false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Process resources reactively with server-sent events.
     */
    @GetMapping(value = "/process/reactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResourceProcessingEvent> processResourcesReactively(
            @RequestParam List<String> resourceIds) {
        
        log.info("Starting reactive resource processing for {} resources", resourceIds.size());
        
        return resourceService.processResourcesReactively(resourceIds)
                .map(result -> new ResourceProcessingEvent(
                        result.resourceId(),
                        result.success(),
                        result.durationMs(),
                        result.errorMessage(),
                        result.message()
                ))
                .delayElements(Duration.ofMillis(100)) // Add small delay for demonstration
                .doOnNext(event -> log.debug("Reactive processing event: {}", event))
                .doOnComplete(() -> log.info("Reactive processing completed"))
                .doOnError(error -> log.error("Reactive processing error: {}", error.getMessage()));
    }
    
    /**
     * Get resource by ID.
     */
    @GetMapping("/{resourceId}")
    @Timed(name = "resources.get", description = "Time taken to get resource")
    public CompletableFuture<ResponseEntity<ResourceDetailsResponse>> getResource(@PathVariable String resourceId) {
        log.info("Getting resource: {}", resourceId);
        
        return resourceService.getResourceAsync(resourceId)
                .thenApply(resource -> {
                    var response = new ResourceDetailsResponse(
                            resource.getResourceId(),
                            resource.getOrganizationId(),
                            resource.getOrganizationName(),
                            resource.getCnpj(),
                            resource.getType(),
                            resource.getStatus(),
                            resource.getDiscoveredAt(),
                            resource.getLastSyncedAt(),
                            resource.getLastValidatedAt(),
                            true,
                            "Resource retrieved successfully"
                    );
                    
                    log.info("Successfully retrieved resource: {}", resourceId);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Error getting resource {}: {}", resourceId, e.getMessage());
                    
                    var response = new ResourceDetailsResponse(
                            resourceId, null, null, null, null, null, null, null, null,
                            false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Update resource status.
     */
    @PutMapping("/{resourceId}/status")
    @Timed(name = "resources.update.status", description = "Time taken to update resource status")
    public CompletableFuture<ResponseEntity<ResourceStatusUpdateResponse>> updateResourceStatus(
            @PathVariable String resourceId, @RequestBody ResourceStatusUpdateRequest request) {
        
        log.info("Updating resource {} status to {}", resourceId, request.newStatus());
        
        return resourceService.updateResourceStatusAsync(resourceId, request.newStatus())
                .thenApply(resource -> {
                    var response = new ResourceStatusUpdateResponse(
                            resourceId,
                            request.newStatus(),
                            true,
                            "Resource status updated successfully"
                    );
                    
                    log.info("Successfully updated resource {} status to {}", resourceId, request.newStatus());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Error updating resource {} status: {}", resourceId, e.getMessage());
                    
                    var response = new ResourceStatusUpdateResponse(
                            resourceId,
                            request.newStatus(),
                            false,
                            e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Get resources by type and status.
     */
    @GetMapping("/search")
    @Timed(name = "resources.search", description = "Time taken to search resources")
    public CompletableFuture<ResponseEntity<ResourceSearchResponse>> searchResources(
            @RequestParam ResourceType type,
            @RequestParam(required = false) ResourceStatus status) {
        
        log.info("Searching resources by type {} and status {}", type, status);
        
        ResourceStatus searchStatus = status != null ? status : ResourceStatus.ACTIVE;
        
        return resourceService.getResourcesByTypeAsync(type, searchStatus)
                .thenApply(resources -> {
                    var response = new ResourceSearchResponse(
                            resources.size(),
                            type,
                            searchStatus,
                            true,
                            "Resource search completed successfully",
                            resources.stream().map(resource -> new ResourceSummary(
                                    resource.getResourceId(),
                                    resource.getOrganizationName(),
                                    resource.getType(),
                                    resource.getStatus(),
                                    resource.getDiscoveredAt()
                            )).toList()
                    );
                    
                    log.info("Resource search completed: {} resources found", resources.size());
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Resource search failed: {}", e.getMessage());
                    
                    var response = new ResourceSearchResponse(
                            0, type, searchStatus, false, e.getMessage(), List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Get performance metrics.
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics() {
        var report = performanceMonitor.getPerformanceReport();
        var recommendations = performanceMonitor.getRecommendations();
        
        var response = new PerformanceMetricsResponse(
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
                recommendations.recommendedBatchSize(),
                recommendations.recommendedConcurrency(),
                recommendations.optimizationSuggestions()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get resource utilization.
     */
    @GetMapping("/metrics/resources")
    public ResponseEntity<ResourceUtilizationResponse> getResourceUtilization() {
        var utilization = resourceManager.getResourceUtilization();
        
        var response = new ResourceUtilizationResponse(
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
                resourceManager.getDynamicDiscoveryConcurrency(),
                resourceManager.getDynamicSyncConcurrency(),
                resourceManager.getDynamicValidationConcurrency(),
                resourceManager.getDynamicMonitoringConcurrency(),
                resourceManager.getDynamicApiCallConcurrency()
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
                "activeVirtualThreads", report.activeVirtualThreads()
        ));
    }
    
    // Helper methods
    
    private double calculateThroughput(int operations, long durationMs) {
        if (durationMs == 0) return 0.0;
        return operations * 1000.0 / durationMs;
    }
    
    // Request/Response DTOs
    
    public record ResourceDiscoveryRequest(List<String> discoveryEndpoints) {}
    
    public record ResourceSyncRequest(List<String> resourceIds) {}
    
    public record ResourceValidationRequest(List<String> resourceIds) {}
    
    public record ResourceHealthMonitoringRequest(List<String> resourceIds) {}
    
    public record MassiveProcessingRequest(List<String> resourceIds, int targetConcurrency) {}
    
    public record ResourceStatusUpdateRequest(ResourceStatus newStatus) {}
    
    public record ResourceDiscoveryResponse(
            int resourceCount,
            boolean success,
            String message,
            List<ResourceSummary> resources
    ) {}
    
    public record ResourceSyncResponse(
            int syncedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel,
            boolean success,
            String message
    ) {}
    
    public record ResourceValidationResponse(
            int validatedCount,
            int errorCount,
            long durationMs,
            boolean success,
            String message,
            List<VirtualThreadResourceService.ValidationSummary> validationResults
    ) {}
    
    public record ResourceHealthMonitoringResponse(
            int monitoredCount,
            int errorCount,
            long durationMs,
            boolean success,
            String message,
            List<HealthSummary> healthResults
    ) {}
    
    public record MassiveProcessingResponse(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int targetConcurrency,
            double throughput,
            boolean success,
            String message
    ) {}
    
    public record ResourceStatusUpdateResponse(
            String resourceId,
            ResourceStatus newStatus,
            boolean success,
            String message
    ) {}
    
    public record ResourceSearchResponse(
            int resourceCount,
            ResourceType type,
            ResourceStatus status,
            boolean success,
            String message,
            List<ResourceSummary> resources
    ) {}
    
    public record ResourceDetailsResponse(
            String resourceId,
            String organizationId,
            String organizationName,
            String cnpj,
            ResourceType type,
            ResourceStatus status,
            LocalDateTime discoveredAt,
            LocalDateTime lastSyncedAt,
            LocalDateTime lastValidatedAt,
            boolean success,
            String message
    ) {}
    
    public record ResourceProcessingEvent(
            String resourceId,
            boolean success,
            long durationMs,
            String errorMessage,
            String message
    ) {}
    
    public record ResourceSummary(
            String resourceId,
            String organizationName,
            ResourceType type,
            ResourceStatus status,
            LocalDateTime discoveredAt
    ) {}
    
    public record HealthSummary(
            String status,
            double healthScore,
            long averageResponseTime,
            double uptime,
            double errorRate,
            LocalDateTime lastCheckAt
    ) {}
    
    public record PerformanceMetricsResponse(
            long totalResourcesDiscovered,
            long totalResourcesSynced,
            long totalResourcesValidated,
            long totalResourcesMonitored,
            long totalBatchesProcessed,
            long totalErrors,
            double currentThroughput,
            double processingEfficiency,
            int activeVirtualThreads,
            int concurrentResourceOperations,
            double errorRate,
            int recommendedBatchSize,
            int recommendedConcurrency,
            String optimizationSuggestions
    ) {}
    
    public record ResourceUtilizationResponse(
            int activeResourceDiscoveryTasks,
            int activeResourceSyncTasks,
            int activeResourceValidationTasks,
            int activeResourceMonitoringTasks,
            int activeApiCalls,
            int activeBatchProcessingTasks,
            double currentCpuUsage,
            double currentMemoryUsage,
            int dynamicBatchSize,
            int dynamicConcurrencyLevel,
            int dynamicDiscoveryConcurrency,
            int dynamicSyncConcurrency,
            int dynamicValidationConcurrency,
            int dynamicMonitoringConcurrency,
            int dynamicApiCallConcurrency
    ) {}
}