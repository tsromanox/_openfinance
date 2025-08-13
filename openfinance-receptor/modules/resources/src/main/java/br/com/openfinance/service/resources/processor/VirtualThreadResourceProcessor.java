package br.com.openfinance.service.resources.processor;

import br.com.openfinance.service.resources.domain.*;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Advanced resource processor using Java 21 Virtual Threads and Structured Concurrency.
 * Provides high-performance parallel processing for resource discovery, validation, and monitoring.
 */
@Slf4j
@Component
public class VirtualThreadResourceProcessor {
    
    private final TaskExecutor resourceDiscoveryExecutor;
    private final TaskExecutor resourceSyncExecutor;
    private final TaskExecutor resourceValidationExecutor;
    private final TaskExecutor structuredConcurrencyExecutor;
    private final AdaptiveResourceResourceManager resourceManager;
    private final ResourcePerformanceMonitor performanceMonitor;
    
    public VirtualThreadResourceProcessor(
            TaskExecutor resourceDiscoveryVirtualThreadExecutor,
            TaskExecutor resourceSyncVirtualThreadExecutor,
            TaskExecutor resourceValidationVirtualThreadExecutor,
            TaskExecutor structuredConcurrencyResourceExecutor,
            AdaptiveResourceResourceManager resourceManager,
            ResourcePerformanceMonitor performanceMonitor) {
        this.resourceDiscoveryExecutor = resourceDiscoveryVirtualThreadExecutor;
        this.resourceSyncExecutor = resourceSyncVirtualThreadExecutor;
        this.resourceValidationExecutor = resourceValidationVirtualThreadExecutor;
        this.structuredConcurrencyExecutor = structuredConcurrencyResourceExecutor;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
    }
    
    /**
     * Discover resources using Structured Concurrency for coordinated execution.
     */
    public CompletableFuture<BatchResourceDiscoveryResult> discoverResourcesWithStructuredConcurrency(
            List<String> discoveryEndpoints) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            AtomicInteger discoveredCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            Collection<Resource> discoveredResources = new ConcurrentLinkedQueue<>();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Create subtasks for each discovery endpoint
                List<Supplier<List<Resource>>> discoveryTasks = discoveryEndpoints.stream()
                        .map(endpoint -> scope.fork(() -> {
                            try {
                                if (!resourceManager.acquireResourceDiscoveryResources()) {
                                    log.warn("Resource limit reached for discovery endpoint: {}", endpoint);
                                    return List.<Resource>of();
                                }
                                
                                try {
                                    performanceMonitor.recordResourceOperation("DISCOVERY_START", true, 0);
                                    
                                    var resources = discoverResourcesFromEndpoint(endpoint);
                                    discoveredCount.addAndGet(resources.size());
                                    discoveredResources.addAll(resources);
                                    
                                    performanceMonitor.recordResourceOperation("DISCOVERY_COMPLETE", true, 
                                            System.currentTimeMillis() - startTime);
                                    
                                    return resources;
                                    
                                } finally {
                                    resourceManager.releaseResourceDiscoveryResources();
                                }
                                
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                                performanceMonitor.recordError("resource_discovery_error", "DISCOVERY", true);
                                log.error("Error discovering resources from endpoint {}: {}", endpoint, e.getMessage());
                                return List.<Resource>of();
                            }
                        }))
                        .map(Supplier.class::cast)
                        .toList();
                
                // Wait for all tasks to complete or fail
                scope.join();
                scope.throwIfFailed();
                
                long duration = System.currentTimeMillis() - startTime;
                
                var result = new BatchResourceDiscoveryResult(
                        discoveredCount.get(),
                        errorCount.get(),
                        new ArrayList<>(discoveredResources),
                        duration,
                        "STRUCTURED_CONCURRENCY"
                );
                
                performanceMonitor.recordBatchProcessing(discoveredCount.get(), duration);
                
                log.info("Structured Concurrency resource discovery completed: {} resources in {} ms", 
                        discoveredCount.get(), duration);
                
                return result;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Resource discovery interrupted", e);
            } catch (Exception e) {
                log.error("Structured Concurrency resource discovery failed", e);
                throw new RuntimeException("Resource discovery failed", e);
            }
            
        }, structuredConcurrencyExecutor);
    }
    
    /**
     * Synchronize resources in parallel batches with adaptive sizing.
     */
    public CompletableFuture<BatchResourceSyncResult> syncResourcesInAdaptiveBatches(
            List<String> resourceIds) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            AtomicInteger syncedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            Collection<Resource> allResources = new ConcurrentLinkedQueue<>();
            
            int batchSize = resourceManager.getDynamicBatchSize();
            int maxConcurrency = resourceManager.getDynamicConcurrencyLevel();
            
            log.info("Starting adaptive batch resource sync: {} resources, batch size: {}, concurrency: {}", 
                    resourceIds.size(), batchSize, maxConcurrency);
            
            // Create batches
            List<List<String>> batches = createBatches(resourceIds, batchSize);
            
            // Process batches with controlled concurrency
            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            
            for (int i = 0; i < batches.size(); i += maxConcurrency) {
                int endIndex = Math.min(i + maxConcurrency, batches.size());
                List<List<String>> currentBatchGroup = batches.subList(i, endIndex);
                
                List<CompletableFuture<Void>> groupFutures = currentBatchGroup.stream()
                        .map(batch -> CompletableFuture.runAsync(() -> {
                            processSyncBatch(batch, syncedCount, errorCount, allResources);
                        }, resourceSyncExecutor))
                        .toList();
                
                batchFutures.addAll(groupFutures);
                
                // Wait for current group to complete before starting next
                CompletableFuture.allOf(groupFutures.toArray(new CompletableFuture[0])).join();
            }
            
            // Wait for all batches to complete
            CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            
            var result = new BatchResourceSyncResult(
                    syncedCount.get(),
                    errorCount.get(),
                    new ArrayList<>(allResources),
                    duration,
                    "ADAPTIVE_BATCH"
            );
            
            performanceMonitor.recordBatchProcessing(syncedCount.get(), duration);
            
            log.info("Adaptive batch resource sync completed: {} resources in {} ms", 
                    syncedCount.get(), duration);
            
            return result;
            
        }, structuredConcurrencyExecutor);
    }
    
    /**
     * Validate resources in parallel with Virtual Threads.
     */
    public CompletableFuture<ResourceValidationResult> validateResourcesInParallel(
            List<String> resourceIds) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            AtomicInteger validatedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            Collection<ResourceValidationStatus> validationResults = new ConcurrentLinkedQueue<>();
            
            List<CompletableFuture<Void>> validationFutures = resourceIds.stream()
                    .map(resourceId -> CompletableFuture.runAsync(() -> {
                        try {
                            if (!resourceManager.acquireResourceValidationResources()) {
                                log.warn("Resource limit reached for validation: {}", resourceId);
                                return;
                            }
                            
                            try {
                                performanceMonitor.recordResourceOperation("VALIDATION_START", true, 0);
                                
                                var validationStatus = validateSingleResource(resourceId);
                                if (validationStatus != null) {
                                    validatedCount.incrementAndGet();
                                    validationResults.add(validationStatus);
                                }
                                
                                performanceMonitor.recordResourceOperation("VALIDATION_COMPLETE", true, 
                                        System.currentTimeMillis() - startTime);
                                
                            } finally {
                                resourceManager.releaseResourceValidationResources();
                            }
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            performanceMonitor.recordError("resource_validation_error", "VALIDATION", true);
                            log.error("Error validating resource {}: {}", resourceId, e.getMessage());
                        }
                    }, resourceValidationExecutor))
                    .toList();
            
            // Wait for all validations to complete
            CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            
            var result = new ResourceValidationResult(
                    validatedCount.get(),
                    errorCount.get(),
                    new ArrayList<>(validationResults),
                    duration
            );
            
            performanceMonitor.recordBatchProcessing(validatedCount.get(), duration);
            
            log.info("Parallel resource validation completed: {} resources validated in {} ms", 
                    validatedCount.get(), duration);
            
            return result;
            
        }, structuredConcurrencyExecutor);
    }
    
    /**
     * Process massive resource workloads with Virtual Thread scalability.
     */
    public CompletableFuture<BatchResourceProcessingResult> processMassiveResourceWorkload(
            List<String> resourceIds, int targetConcurrency) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            AtomicLong processedCount = new AtomicLong(0);
            AtomicLong errorCount = new AtomicLong(0);
            
            log.info("Starting massive resource workload processing: {} resources with {} concurrency", 
                    resourceIds.size(), targetConcurrency);
            
            // Use Virtual Thread scalability for massive parallel processing
            try (var scope = new StructuredTaskScope.ShutdownOnSuccess<Integer>()) {
                
                // Create worker tasks
                IntStream.range(0, targetConcurrency)
                        .forEach(workerIndex -> {
                            scope.fork(() -> {
                                int processed = 0;
                                int startIndex = workerIndex * (resourceIds.size() / targetConcurrency);
                                int endIndex = (workerIndex == targetConcurrency - 1) ? 
                                        resourceIds.size() : 
                                        (workerIndex + 1) * (resourceIds.size() / targetConcurrency);
                                
                                for (int i = startIndex; i < endIndex; i++) {
                                    try {
                                        processResourceWorkload(resourceIds.get(i));
                                        processed++;
                                        processedCount.incrementAndGet();
                                        
                                        // Adaptive throttling
                                        if (processed % 10 == 0) {
                                            performanceMonitor.recordVirtualThreadUsage(
                                                    Thread.getAllStackTraces().size());
                                        }
                                        
                                    } catch (Exception e) {
                                        errorCount.incrementAndGet();
                                        log.error("Error processing resource {}: {}", resourceIds.get(i), e.getMessage());
                                    }
                                }
                                
                                return processed;
                            });
                        });
                
                // Wait for completion
                scope.join();
                
                long duration = System.currentTimeMillis() - startTime;
                
                var result = new BatchResourceProcessingResult(
                        (int) processedCount.get(),
                        (int) errorCount.get(),
                        duration,
                        "MASSIVE_VIRTUAL_THREAD"
                );
                
                performanceMonitor.recordBatchProcessing((int) processedCount.get(), duration);
                
                log.info("Massive resource workload completed: {} processed, {} errors in {} ms", 
                        processedCount.get(), errorCount.get(), duration);
                
                return result;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Massive processing interrupted", e);
            }
            
        }, structuredConcurrencyExecutor);
    }
    
    /**
     * Monitor resource health with parallel checks.
     */
    public CompletableFuture<ResourceHealthMonitoringResult> monitorResourceHealthInParallel(
            List<String> resourceIds) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            AtomicInteger monitoredCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            Collection<ResourceHealth> healthResults = new ConcurrentLinkedQueue<>();
            
            List<CompletableFuture<Void>> monitoringFutures = resourceIds.stream()
                    .map(resourceId -> CompletableFuture.runAsync(() -> {
                        try {
                            if (!resourceManager.acquireResourceMonitoringResources()) {
                                log.warn("Resource limit reached for health monitoring: {}", resourceId);
                                return;
                            }
                            
                            try {
                                performanceMonitor.recordResourceOperation("HEALTH_CHECK_START", true, 0);
                                
                                var health = checkResourceHealth(resourceId);
                                if (health != null) {
                                    monitoredCount.incrementAndGet();
                                    healthResults.add(health);
                                }
                                
                                performanceMonitor.recordResourceOperation("HEALTH_CHECK_COMPLETE", true, 
                                        System.currentTimeMillis() - startTime);
                                
                            } finally {
                                resourceManager.releaseResourceMonitoringResources();
                            }
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            performanceMonitor.recordError("resource_health_check_error", "HEALTH_CHECK", true);
                            log.error("Error checking health for resource {}: {}", resourceId, e.getMessage());
                        }
                    }, resourceValidationExecutor))
                    .toList();
            
            // Wait for all health checks to complete
            CompletableFuture.allOf(monitoringFutures.toArray(new CompletableFuture[0])).join();
            
            long duration = System.currentTimeMillis() - startTime;
            
            var result = new ResourceHealthMonitoringResult(
                    monitoredCount.get(),
                    errorCount.get(),
                    new ArrayList<>(healthResults),
                    duration
            );
            
            performanceMonitor.recordBatchProcessing(monitoredCount.get(), duration);
            
            log.info("Parallel resource health monitoring completed: {} resources checked in {} ms", 
                    monitoredCount.get(), duration);
            
            return result;
            
        }, structuredConcurrencyExecutor);
    }
    
    // Helper methods
    
    private List<Resource> discoverResourcesFromEndpoint(String endpoint) {
        try {
            // Simulate resource discovery logic
            Thread.sleep(100); // Simulate network call
            
            // Create mock discovered resources
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
    
    private ResourceValidationStatus validateSingleResource(String resourceId) {
        try {
            // Simulate resource validation
            Thread.sleep(50);
            
            return new ResourceValidationStatus(
                resourceId,
                true,
                "Validation successful",
                LocalDateTime.now()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private ResourceHealth checkResourceHealth(String resourceId) {
        try {
            // Simulate health check
            Thread.sleep(25);
            
            return ResourceHealth.builder()
                    .status(HealthStatus.UP)
                    .healthScore(0.95)
                    .averageResponseTime(150)
                    .uptime(0.99)
                    .totalRequests(1000)
                    .successfulRequests(950)
                    .failedRequests(50)
                    .errorRate(0.05)
                    .lastCheckAt(LocalDateTime.now())
                    .build();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private void processResourceWorkload(String resourceId) {
        // Simplified processing for massive workloads
        try {
            Thread.sleep(10); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processSyncBatch(List<String> batch, AtomicInteger syncedCount, 
                                 AtomicInteger errorCount, Collection<Resource> allResources) {
        for (String resourceId : batch) {
            try {
                // Process individual resource sync
                syncedCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("Error syncing resource in batch {}: {}", resourceId, e.getMessage());
            }
        }
    }
    
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(items.subList(i, endIndex));
        }
        return batches;
    }
    
    // Result classes
    
    public record BatchResourceDiscoveryResult(
            int discoveredCount,
            int errorCount,
            List<Resource> resources,
            long durationMs,
            String strategy
    ) {}
    
    public record BatchResourceSyncResult(
            int syncedCount,
            int errorCount,
            List<Resource> resources,
            long durationMs,
            String strategy
    ) {}
    
    public record ResourceValidationResult(
            int validatedCount,
            int errorCount,
            List<ResourceValidationStatus> validationResults,
            long durationMs
    ) {}
    
    public record ResourceHealthMonitoringResult(
            int monitoredCount,
            int errorCount,
            List<ResourceHealth> healthResults,
            long durationMs
    ) {}
    
    public record BatchResourceProcessingResult(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy
    ) {}
    
    public record ResourceValidationStatus(
            String resourceId,
            boolean isValid,
            String message,
            LocalDateTime validatedAt
    ) {}
}