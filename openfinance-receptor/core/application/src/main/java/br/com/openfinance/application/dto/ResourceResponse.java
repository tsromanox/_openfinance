package br.com.openfinance.application.dto;

import br.com.openfinance.service.resources.domain.ResourceStatus;
import br.com.openfinance.service.resources.domain.ResourceType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTOs for resource-related operations.
 */
public class ResourceResponse {
    
    /**
     * Basic resource information response.
     */
    public record ResourceInfo(
            String resourceId,
            String organizationId,
            String organizationName,
            String cnpj,
            ResourceType type,
            ResourceStatus status,
            LocalDateTime discoveredAt,
            LocalDateTime lastSyncedAt,
            LocalDateTime lastValidatedAt,
            LocalDateTime lastMonitoredAt
    ) {}
    
    /**
     * Resource discovery operation response.
     */
    public record ResourceDiscoveryResponse(
            int resourceCount,
            long durationMs,
            String strategy,
            boolean success,
            String message,
            List<ResourceInfo> resources
    ) {}
    
    /**
     * Resource synchronization operation response.
     */
    public record ResourceSyncResponse(
            int syncedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel,
            boolean success,
            String message,
            List<String> errorMessages
    ) {}
    
    /**
     * Resource validation operation response.
     */
    public record ResourceValidationResponse(
            int validatedCount,
            int errorCount,
            long durationMs,
            boolean success,
            String message,
            List<ValidationResult> validationResults
    ) {}
    
    /**
     * Resource health monitoring response.
     */
    public record ResourceHealthResponse(
            int monitoredCount,
            int errorCount,
            long durationMs,
            boolean success,
            String message,
            List<HealthResult> healthResults
    ) {}
    
    /**
     * Resource processing performance metrics response.
     */
    public record ResourcePerformanceResponse(
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
            String optimizationSuggestions
    ) {}
    
    /**
     * Individual validation result.
     */
    public record ValidationResult(
            String resourceId,
            boolean isValid,
            String validationMessage,
            String errorType,
            LocalDateTime validatedAt
    ) {}
    
    /**
     * Individual health check result.
     */
    public record HealthResult(
            String resourceId,
            boolean isHealthy,
            double healthScore,
            long averageResponseTime,
            double uptime,
            double errorRate,
            String statusMessage,
            LocalDateTime lastCheckAt
    ) {}
    
    /**
     * Resource utilization information.
     */
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
            String adaptationStrategy
    ) {}
    
    /**
     * Batch processing result response.
     */
    public record BatchProcessingResponse(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel,
            double throughput,
            boolean success,
            String message
    ) {}
}