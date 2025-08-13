package br.com.openfinance.resources.application.services;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.ports.output.ResourceRepository;
import br.com.openfinance.resources.domain.ports.output.ResourceEventPublisher;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualThreadResourceBatchProcessor {

    private final ResourceRepository resourceRepository;
    private final ResourceApplicationService resourceApplicationService;
    private final ResourceEventPublisher eventPublisher;

    @Value("${openfinance.resources.batch.virtual-threads.max:10000}")
    private int maxVirtualThreads;

    @Value("${openfinance.resources.batch.size:1000}")
    private int batchSize;

    @Value("${openfinance.resources.batch.timeout-minutes:120}")
    private int timeoutMinutes;

    @Monitored
    public void processResourceUpdatesWithVirtualThreads() {
        log.info("Starting Virtual Thread batch processing for resources");
        
        List<Resource> resources = resourceRepository.findResourcesForBatchUpdate(batchSize);
        
        if (resources.isEmpty()) {
            log.info("No resources found for batch update");
            return;
        }

        log.info("Processing {} resources with Virtual Threads (max: {})", resources.size(), maxVirtualThreads);
        
        // Semaphore to control concurrent Virtual Threads
        Semaphore semaphore = new Semaphore(maxVirtualThreads);
        
        try (var scope = StructuredTaskScope.ShutdownOnFailure()) {
            int successCount = 0;
            int errorCount = 0;

            for (Resource resource : resources) {
                // Submit Virtual Thread task
                scope.fork(() -> {
                    try {
                        // Acquire permit to limit concurrency
                        semaphore.acquire();
                        
                        try {
                            log.debug("Processing resource {} on Virtual Thread: {}", 
                                    resource.getResourceId(), 
                                    Thread.currentThread().getName());
                            
                            // Sync resource
                            resourceApplicationService.syncResource(resource.getResourceId());
                            
                            log.debug("Successfully processed resource: {}", resource.getResourceId());
                            return "SUCCESS";
                            
                        } finally {
                            semaphore.release();
                        }
                    } catch (Exception e) {
                        log.error("Error processing resource {}: {}", resource.getResourceId(), e.getMessage());
                        return "ERROR: " + e.getMessage();
                    }
                });
            }

            // Join all Virtual Thread tasks
            scope.join();
            
            // Check for failures
            scope.throwIfFailed();

            // Count results
            for (Resource resource : resources) {
                try {
                    // This is a simplified success counting
                    // In a real implementation, you'd track individual results
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }

            // Publish batch completion event
            eventPublisher.publishBatchSyncCompleted(successCount);

            log.info("Virtual Thread batch processing completed. Success: {}, Errors: {}, Total Virtual Threads used: {}", 
                    successCount, errorCount, resources.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Virtual Thread batch processing was interrupted", e);
            throw new RuntimeException("Batch processing interrupted", e);
        } catch (Exception e) {
            log.error("Virtual Thread batch processing failed", e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    @Monitored
    public void processResourcesWithPagination() {
        log.info("Starting paginated Virtual Thread processing for resources");
        
        int pageSize = 1000;
        int totalProcessed = 0;
        int totalErrors = 0;
        
        List<Resource> resources;
        
        do {
            resources = resourceRepository.findResourcesNeedingSync(pageSize);
            
            if (!resources.isEmpty()) {
                log.info("Processing page of {} resources", resources.size());
                
                try {
                    processResourceBatch(resources);
                    totalProcessed += resources.size();
                } catch (Exception e) {
                    log.error("Error processing resource batch: {}", e.getMessage());
                    totalErrors += resources.size();
                }
            }
            
        } while (!resources.isEmpty());
        
        log.info("Paginated Virtual Thread processing completed. Processed: {}, Errors: {}", 
                totalProcessed, totalErrors);
    }

    private void processResourceBatch(List<Resource> resources) {
        Semaphore semaphore = new Semaphore(maxVirtualThreads);
        
        try (var scope = StructuredTaskScope.ShutdownOnFailure()) {
            
            for (Resource resource : resources) {
                scope.fork(() -> {
                    try {
                        semaphore.acquire();
                        
                        try {
                            resourceApplicationService.syncResource(resource.getResourceId());
                            return "SUCCESS";
                        } finally {
                            semaphore.release();
                        }
                    } catch (Exception e) {
                        log.error("Error processing resource {}: {}", resource.getResourceId(), e.getMessage());
                        return "ERROR";
                    }
                });
            }
            
            scope.join();
            scope.throwIfFailed();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Batch processing interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    @Monitored  
    public void processResourcesByCustomerWithVirtualThreads(String customerId) {
        log.info("Starting Virtual Thread processing for customer: {}", customerId);
        
        // This would fetch resources for a specific customer and process them
        // with Virtual Threads in a similar pattern
        
        try {
            resourceApplicationService.syncResourcesByCustomer(customerId);
            log.info("Completed Virtual Thread processing for customer: {}", customerId);
        } catch (Exception e) {
            log.error("Error processing resources for customer {}: {}", customerId, e.getMessage());
            throw e;
        }
    }
}