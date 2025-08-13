package br.com.openfinance.health;

import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for resource processing operations.
 * Provides detailed health information about Virtual Thread usage,
 * resource processing performance, and system utilization.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "openfinance.resources.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceHealthIndicator implements HealthIndicator {
    
    private final ResourcePerformanceMonitor performanceMonitor;
    private final AdaptiveResourceResourceManager resourceManager;
    
    public ResourceHealthIndicator(
            ResourcePerformanceMonitor performanceMonitor,
            AdaptiveResourceResourceManager resourceManager) {
        this.performanceMonitor = performanceMonitor;
        this.resourceManager = resourceManager;
    }
    
    @Override
    public Health health() {
        try {
            var performanceReport = performanceMonitor.getPerformanceReport();
            var resourceUtilization = resourceManager.getResourceUtilization();
            
            // Determine overall health status
            boolean healthy = isSystemHealthy(performanceReport, resourceUtilization);
            
            var healthBuilder = healthy ? Health.up() : Health.down();
            
            // Add performance metrics
            healthBuilder
                .withDetail("performance", createPerformanceDetails(performanceReport))
                .withDetail("utilization", createUtilizationDetails(resourceUtilization))
                .withDetail("virtualThreads", createVirtualThreadDetails(performanceReport, resourceUtilization))
                .withDetail("adaptiveSettings", createAdaptiveSettingsDetails())
                .withDetail("timestamps", createTimestampDetails());
            
            log.debug("Resource health check completed - Status: {}", healthy ? "UP" : "DOWN");
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            log.error("Resource health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }
    
    private boolean isSystemHealthy(
            ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport,
            AdaptiveResourceResourceManager.ResourceUtilization resourceUtilization) {
        
        // Check CPU usage
        if (resourceUtilization.currentCpuUsage() > 0.95) {
            log.warn("High CPU usage detected: {:.2f}%", resourceUtilization.currentCpuUsage() * 100);
            return false;
        }
        
        // Check memory usage
        if (resourceUtilization.currentMemoryUsage() > 0.95) {
            log.warn("High memory usage detected: {:.2f}%", resourceUtilization.currentMemoryUsage() * 100);
            return false;
        }
        
        // Check error rate
        if (performanceReport.errorRate() > 0.25) {
            log.warn("High error rate detected: {:.2f}%", performanceReport.errorRate() * 100);
            return false;
        }
        
        // Check processing efficiency
        if (performanceReport.processingEfficiency() < 0.60) {
            log.warn("Low processing efficiency detected: {:.2f}%", performanceReport.processingEfficiency() * 100);
            return false;
        }
        
        return true;
    }
    
    private Object createPerformanceDetails(ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport) {
        return java.util.Map.of(
            "totalResourcesDiscovered", performanceReport.totalResourcesDiscovered(),
            "totalResourcesSynced", performanceReport.totalResourcesSynced(),
            "totalResourcesValidated", performanceReport.totalResourcesValidated(),
            "totalResourcesMonitored", performanceReport.totalResourcesMonitored(),
            "totalBatchesProcessed", performanceReport.totalBatchesProcessed(),
            "totalErrors", performanceReport.totalErrors(),
            "currentThroughput", String.format("%.2f ops/sec", performanceReport.currentThroughput()),
            "processingEfficiency", String.format("%.2f%%", performanceReport.processingEfficiency() * 100),
            "errorRate", String.format("%.2f%%", performanceReport.errorRate() * 100),
            "averageDiscoveryTime", String.format("%.2f ms", performanceReport.averageDiscoveryTime()),
            "averageSyncTime", String.format("%.2f ms", performanceReport.averageSyncTime()),
            "averageValidationTime", String.format("%.2f ms", performanceReport.averageValidationTime()),
            "averageMonitoringTime", String.format("%.2f ms", performanceReport.averageMonitoringTime())
        );
    }
    
    private Object createUtilizationDetails(AdaptiveResourceResourceManager.ResourceUtilization resourceUtilization) {
        return java.util.Map.of(
            "activeResourceDiscoveryTasks", resourceUtilization.activeResourceDiscoveryTasks(),
            "activeResourceSyncTasks", resourceUtilization.activeResourceSyncTasks(),
            "activeResourceValidationTasks", resourceUtilization.activeResourceValidationTasks(),
            "activeResourceMonitoringTasks", resourceUtilization.activeResourceMonitoringTasks(),
            "activeApiCalls", resourceUtilization.activeApiCalls(),
            "activeBatchProcessingTasks", resourceUtilization.activeBatchProcessingTasks(),
            "currentCpuUsage", String.format("%.2f%%", resourceUtilization.currentCpuUsage() * 100),
            "currentMemoryUsage", String.format("%.2f%%", resourceUtilization.currentMemoryUsage() * 100),
            "availableProcessors", resourceUtilization.availableProcessors(),
            "availablePermits", java.util.Map.of(
                "discovery", resourceUtilization.availableResourceDiscoveryPermits(),
                "sync", resourceUtilization.availableResourceSyncPermits(),
                "validation", resourceUtilization.availableResourceValidationPermits(),
                "monitoring", resourceUtilization.availableResourceMonitoringPermits(),
                "apiCalls", resourceUtilization.availableApiCallPermits(),
                "batchProcessing", resourceUtilization.availableBatchProcessingPermits()
            )
        );
    }
    
    private Object createVirtualThreadDetails(
            ResourcePerformanceMonitor.ResourcePerformanceReport performanceReport,
            AdaptiveResourceResourceManager.ResourceUtilization resourceUtilization) {
        
        int totalActiveTasks = resourceUtilization.activeResourceDiscoveryTasks() +
                              resourceUtilization.activeResourceSyncTasks() +
                              resourceUtilization.activeResourceValidationTasks() +
                              resourceUtilization.activeResourceMonitoringTasks() +
                              resourceUtilization.activeApiCalls() +
                              resourceUtilization.activeBatchProcessingTasks();
        
        return java.util.Map.of(
            "activeVirtualThreads", performanceReport.activeVirtualThreads(),
            "concurrentResourceOperations", performanceReport.concurrentResourceOperations(),
            "totalActiveTasks", totalActiveTasks,
            "virtualThreadEfficiency", totalActiveTasks > 0 ? 
                String.format("%.2f%%", (double) performanceReport.concurrentResourceOperations() / totalActiveTasks * 100) : 
                "N/A",
            "threadsPerProcessor", String.format("%.2f", 
                (double) performanceReport.activeVirtualThreads() / resourceUtilization.availableProcessors())
        );
    }
    
    private Object createAdaptiveSettingsDetails() {
        return java.util.Map.of(
            "dynamicBatchSize", resourceManager.getDynamicBatchSize(),
            "dynamicConcurrencyLevel", resourceManager.getDynamicConcurrencyLevel(),
            "concurrencyByOperation", java.util.Map.of(
                "discovery", resourceManager.getDynamicDiscoveryConcurrency(),
                "sync", resourceManager.getDynamicSyncConcurrency(),
                "validation", resourceManager.getDynamicValidationConcurrency(),
                "monitoring", resourceManager.getDynamicMonitoringConcurrency(),
                "apiCalls", resourceManager.getDynamicApiCallConcurrency()
            ),
            "adaptationInterval", String.format("%d ms", resourceManager.getAdaptationInterval())
        );
    }
    
    private Object createTimestampDetails() {
        return java.util.Map.of(
            "lastHealthCheck", java.time.LocalDateTime.now().toString(),
            "uptime", java.time.Duration.ofMillis(
                java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime()
            ).toString()
        );
    }
}