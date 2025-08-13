package br.com.openfinance.service.resources;

import br.com.openfinance.service.resources.domain.*;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.processor.VirtualThreadResourceProcessor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Enhanced resource service using Java 21 Virtual Threads and Structured Concurrency.
 * Provides high-performance resource discovery, synchronization, validation, and monitoring
 * for Open Finance Brasil resources.
 */
@Slf4j
@Service
public class VirtualThreadResourceService {
    
    private final VirtualThreadResourceProcessor resourceProcessor;
    private final AdaptiveResourceResourceManager resourceManager;
    private final ResourcePerformanceMonitor performanceMonitor;
    private final TaskExecutor resourceDiscoveryExecutor;
    private final TaskExecutor resourceSyncExecutor;
    private final TaskExecutor reactiveProcessingExecutor;
    
    public VirtualThreadResourceService(
            VirtualThreadResourceProcessor resourceProcessor,
            AdaptiveResourceResourceManager resourceManager,
            ResourcePerformanceMonitor performanceMonitor,
            TaskExecutor resourceDiscoveryVirtualThreadExecutor,
            TaskExecutor resourceSyncVirtualThreadExecutor,
            TaskExecutor reactiveResourceProcessingVirtualThreadExecutor) {
        this.resourceProcessor = resourceProcessor;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
        this.resourceDiscoveryExecutor = resourceDiscoveryVirtualThreadExecutor;
        this.resourceSyncExecutor = resourceSyncVirtualThreadExecutor;
        this.reactiveProcessingExecutor = reactiveResourceProcessingVirtualThreadExecutor;
    }
    
    /**
     * Discover resources from multiple endpoints using Structured Concurrency.
     */
    public CompletableFuture<List<Resource>> discoverResourcesAsync(List<String> discoveryEndpoints) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Get resources in parallel
                List<Resource> resources = discoveryEndpoints.stream()
                        .map(endpoint -> scope.fork(() -> discoverResourcesFromEndpoint(endpoint)))
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (Exception e) {
                                log.error("Error discovering resources from endpoint: {}", e.getMessage());
                                return List.<Resource>of();
                            }
                        })
                        .flatMap(Collection::stream)
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                // Process resources using the processor
                var result = resourceProcessor.discoverResourcesWithStructuredConcurrency(discoveryEndpoints).get();
                
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordBatchProcessing(result.discoveredCount(), duration);
                
                log.info("Async resource discovery completed: {} resources from {} endpoints in {}ms", 
                        result.discoveredCount(), discoveryEndpoints.size(), duration);
                
                return result.resources();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Resource discovery interrupted", e);
            } catch (Exception e) {
                performanceMonitor.recordError("async_discovery_error", "ASYNC_DISCOVERY", true);
                throw new RuntimeException("Async resource discovery failed", e);
            }
            
        }, resourceDiscoveryExecutor);
    }
    
    /**
     * Synchronize resources in parallel with adaptive batching.
     */
    public CompletableFuture<ResourceSyncResult> syncResourcesAsync(List<String> resourceIds) {
        return resourceProcessor.syncResourcesInAdaptiveBatches(resourceIds)
                .thenApply(result -> new ResourceSyncResult(
                        result.syncedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.strategy(),
                        resourceManager.getDynamicBatchSize(),
                        resourceManager.getDynamicConcurrencyLevel()
                ));
    }
    
    /**
     * Validate resources in parallel.
     */
    public CompletableFuture<ResourceValidationBatchResult> validateResourcesAsync(List<String> resourceIds) {
        return resourceProcessor.validateResourcesInParallel(resourceIds)
                .thenApply(result -> new ResourceValidationBatchResult(
                        result.validatedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.validationResults().stream()
                                .map(status -> new ValidationSummary(
                                        status.resourceId(),
                                        status.isValid(),
                                        status.message(),
                                        status.validatedAt()))
                                .toList()
                ));
    }
    
    /**
     * Monitor resource health in parallel.
     */
    public CompletableFuture<ResourceHealthBatchResult> monitorResourceHealthAsync(List<String> resourceIds) {
        return resourceProcessor.monitorResourceHealthInParallel(resourceIds)
                .thenApply(result -> new ResourceHealthBatchResult(
                        result.monitoredCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.healthResults()
                ));
    }
    
    /**
     * Process resources reactively using WebFlux patterns.
     */
    public Flux<ResourceProcessingResult> processResourcesReactively(List<String> resourceIds) {
        return Flux.fromIterable(resourceIds)
                .parallel(resourceManager.getDynamicConcurrencyLevel())
                .runOn(Schedulers.fromExecutor(reactiveProcessingExecutor))
                .map(this::processResourceReactively)
                .sequential()
                .doOnNext(result -> performanceMonitor.recordResourceOperation("REACTIVE_PROCESS", 
                        result.success(), result.durationMs()))
                .doOnError(error -> performanceMonitor.recordError("reactive_process_error", 
                        "REACTIVE_PROCESS", true));
    }
    
    /**
     * Batch process resources with adaptive sizing.
     */
    public CompletableFuture<BatchProcessingResult> processResourcesInAdaptiveBatches(List<String> resourceIds) {
        return resourceProcessor.syncResourcesInAdaptiveBatches(resourceIds)
                .thenApply(result -> new BatchProcessingResult(
                        result.syncedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.strategy(),
                        resourceManager.getDynamicBatchSize(),
                        resourceManager.getDynamicConcurrencyLevel()
                ));
    }
    
    /**
     * Massive parallel resource processing for high-volume scenarios.
     */
    public CompletableFuture<BatchProcessingResult> processMassiveResourceWorkload(
            List<String> resourceIds, int targetConcurrency) {
        
        return resourceProcessor.processMassiveResourceWorkload(resourceIds, targetConcurrency)
                .thenApply(result -> new BatchProcessingResult(
                        result.processedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.strategy(),
                        resourceManager.getDynamicBatchSize(),
                        targetConcurrency
                ));
    }
    
    /**
     * Get resource by ID with caching.
     */
    public CompletableFuture<Resource> getResourceAsync(String resourceId) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                performanceMonitor.recordResourceOperation("GET_RESOURCE_START", true, 0);
                
                if (!resourceManager.acquireResourceDiscoveryResources()) {
                    log.warn("Resource limit reached for get resource: {}", resourceId);
                    throw new RuntimeException("Resource limit reached");
                }
                
                try {
                    // Simulate resource retrieval
                    var resource = retrieveResource(resourceId);
                    
                    long duration = System.currentTimeMillis() - startTime;
                    performanceMonitor.recordResourceOperation("GET_RESOURCE_COMPLETE", true, duration);
                    
                    log.info("Successfully retrieved resource {} in {}ms", resourceId, duration);
                    
                    return resource;
                    
                } finally {
                    resourceManager.releaseResourceDiscoveryResources();
                }
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordResourceOperation("GET_RESOURCE", false, duration);
                performanceMonitor.recordError("get_resource_error", "GET_RESOURCE", true);
                
                log.error("Error retrieving resource {}: {}", resourceId, e.getMessage());
                throw new RuntimeException("Failed to get resource", e);
            }
            
        }, resourceDiscoveryExecutor);
    }
    
    /**
     * Update resource status.
     */
    public CompletableFuture<Resource> updateResourceStatusAsync(String resourceId, ResourceStatus newStatus) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                performanceMonitor.recordResourceOperation("UPDATE_STATUS_START", true, 0);
                
                var resource = retrieveResource(resourceId);
                var updatedResource = resource.withStatus(newStatus);
                
                // Simulate saving updated resource
                saveResource(updatedResource);
                
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordResourceOperation("UPDATE_STATUS_COMPLETE", true, duration);
                
                log.info("Updated resource {} status to {} in {}ms", resourceId, newStatus, duration);
                
                return updatedResource;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordResourceOperation("UPDATE_STATUS", false, duration);
                performanceMonitor.recordError("update_status_error", "UPDATE_STATUS", true);
                
                log.error("Error updating resource {} status: {}", resourceId, e.getMessage());
                throw new RuntimeException("Failed to update resource status", e);
            }
            
        }, resourceSyncExecutor);
    }
    
    /**
     * Process resources with automatic error recovery and retry.
     */
    public CompletableFuture<List<Resource>> processResourcesWithErrorRecovery(List<String> resourceIds) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<Resource> allResources = new ConcurrentLinkedQueue<>();
            AtomicInteger retryCount = new AtomicInteger(0);
            
            while (retryCount.get() < 3) {
                try {
                    var result = discoverResourcesAsync(resourceIds).get(60, TimeUnit.SECONDS);
                    allResources.addAll(result);
                    break;
                    
                } catch (Exception e) {
                    retryCount.incrementAndGet();
                    log.warn("Resource processing failed (attempt {}): {}", retryCount.get(), e.getMessage());
                    
                    if (retryCount.get() >= 3) {
                        performanceMonitor.recordError("max_retries_exceeded", "ERROR_RECOVERY", false);
                        throw new RuntimeException("Max retries exceeded for resource processing", e);
                    }
                    
                    try {
                        Thread.sleep(1000 * retryCount.get()); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Error recovery interrupted", ie);
                    }
                }
            }
            
            return List.copyOf(allResources);
            
        }, resourceDiscoveryExecutor);
    }
    
    /**
     * Get resources by type with filtering.
     */
    public CompletableFuture<List<Resource>> getResourcesByTypeAsync(ResourceType type, ResourceStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                performanceMonitor.recordResourceOperation("GET_BY_TYPE_START", true, 0);
                
                // Simulate filtering resources by type and status
                var resources = filterResourcesByTypeAndStatus(type, status);
                
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordResourceOperation("GET_BY_TYPE_COMPLETE", true, duration);
                
                log.info("Retrieved {} resources of type {} with status {} in {}ms", 
                        resources.size(), type, status, duration);
                
                return resources;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordResourceOperation("GET_BY_TYPE", false, duration);
                performanceMonitor.recordError("get_by_type_error", "GET_BY_TYPE", true);
                
                log.error("Error getting resources by type {} and status {}: {}", type, status, e.getMessage());
                throw new RuntimeException("Failed to get resources by type", e);
            }
            
        }, resourceDiscoveryExecutor);
    }
    
    // Helper methods
    
    private ResourceProcessingResult processResourceReactively(String resourceId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate resource processing
            Thread.sleep(50);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new ResourceProcessingResult(
                    resourceId,
                    true,
                    duration,
                    null,
                    "Resource processed successfully"
            );
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new ResourceProcessingResult(
                    resourceId,
                    false,
                    duration,
                    e.getMessage(),
                    "Resource processing failed"
            );
        }
    }
    
    private List<Resource> discoverResourcesFromEndpoint(String endpoint) {
        try {
            // Simulate resource discovery logic
            Thread.sleep(100);
            
            return List.of(
                Resource.builder()
                    .resourceId("resource-" + endpoint.hashCode())
                    .organizationId("org-" + endpoint.hashCode())
                    .organizationName("Organization " + endpoint.hashCode())
                    .type(ResourceType.BANK)
                    .status(ResourceStatus.DISCOVERED)
                    .discoveredAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
    
    private Resource retrieveResource(String resourceId) {
        try {
            // Simulate database lookup
            Thread.sleep(25);
            
            return Resource.builder()
                    .resourceId(resourceId)
                    .organizationId("org-" + resourceId.hashCode())
                    .organizationName("Organization " + resourceId.hashCode())
                    .type(ResourceType.BANK)
                    .status(ResourceStatus.ACTIVE)
                    .discoveredAt(LocalDateTime.now().minusHours(1))
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .updatedAt(LocalDateTime.now())
                    .build();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Resource retrieval interrupted", e);
        }
    }
    
    private void saveResource(Resource resource) {
        try {
            // Simulate database save
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Resource save interrupted", e);
        }
    }
    
    private List<Resource> filterResourcesByTypeAndStatus(ResourceType type, ResourceStatus status) {
        try {
            // Simulate filtering logic
            Thread.sleep(75);
            
            return List.of(
                Resource.builder()
                    .resourceId("filtered-resource-1")
                    .organizationId("org-filtered-1")
                    .organizationName("Filtered Organization 1")
                    .type(type)
                    .status(status)
                    .discoveredAt(LocalDateTime.now().minusHours(2))
                    .createdAt(LocalDateTime.now().minusHours(2))
                    .updatedAt(LocalDateTime.now())
                    .build()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
    
    // Result classes
    
    public record ResourceProcessingResult(
            String resourceId,
            boolean success,
            long durationMs,
            String errorMessage,
            String message
    ) {}
    
    public record ResourceSyncResult(
            int syncedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel
    ) {}
    
    public record ResourceValidationBatchResult(
            int validatedCount,
            int errorCount,
            long durationMs,
            List<ValidationSummary> validationResults
    ) {}
    
    public record ResourceHealthBatchResult(
            int monitoredCount,
            int errorCount,
            long durationMs,
            List<ResourceHealth> healthResults
    ) {}
    
    public record BatchProcessingResult(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel
    ) {}
    
    public record ValidationSummary(
            String resourceId,
            boolean isValid,
            String message,
            LocalDateTime validatedAt
    ) {}
}