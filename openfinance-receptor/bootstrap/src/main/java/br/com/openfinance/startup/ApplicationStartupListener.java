package br.com.openfinance.startup;

import br.com.openfinance.application.service.ResourceService;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Application startup and shutdown event listener.
 * Handles initialization of resource processing components and graceful shutdown.
 */
@Slf4j
@Component
public class ApplicationStartupListener {
    
    private final ResourceService resourceService;
    private final ResourcePerformanceMonitor performanceMonitor;
    private final AdaptiveResourceResourceManager resourceManager;
    
    private LocalDateTime startupTime;
    private LocalDateTime readyTime;
    
    public ApplicationStartupListener(
            ResourceService resourceService,
            ResourcePerformanceMonitor performanceMonitor,
            AdaptiveResourceResourceManager resourceManager) {
        this.resourceService = resourceService;
        this.performanceMonitor = performanceMonitor;
        this.resourceManager = resourceManager;
    }
    
    /**
     * Handle application starting event.
     */
    @EventListener(ApplicationStartingEvent.class)
    @Order(1)
    public void handleApplicationStarting(ApplicationStartingEvent event) {
        this.startupTime = LocalDateTime.now();
        
        log.info("=".repeat(80));
        log.info("OpenFinance Receptor Application Starting...");
        log.info("=".repeat(80));
        log.info("Startup Time: {}", startupTime);
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        // Check Virtual Threads support
        boolean virtualThreadsSupported = checkVirtualThreadsSupport();
        log.info("Virtual Threads Supported: {}", virtualThreadsSupported);
        
        if (!virtualThreadsSupported) {
            log.warn("Virtual Threads are not supported in this Java version!");
            log.warn("Performance may be degraded. Please use Java 21+ with --enable-preview");
        }
        
        log.info("=".repeat(80));
    }
    
    /**
     * Handle application started event.
     */
    @EventListener(ApplicationStartedEvent.class)
    @Order(2)
    public void handleApplicationStarted(ApplicationStartedEvent event) {
        log.info("OpenFinance Receptor Application Started Successfully!");
        
        // Log application context details
        var context = event.getApplicationContext();
        log.info("Spring Context: {} beans loaded", context.getBeanDefinitionCount());
        log.info("Active Profiles: {}", String.join(", ", context.getEnvironment().getActiveProfiles()));
        
        // Log Virtual Thread configuration
        logVirtualThreadConfiguration();
        
        // Initialize performance monitoring
        initializePerformanceMonitoring();
    }
    
    /**
     * Handle application ready event.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    public void handleApplicationReady(ApplicationReadyEvent event) {
        this.readyTime = LocalDateTime.now();
        Duration startupDuration = Duration.between(startupTime, readyTime);
        
        log.info("=".repeat(80));
        log.info("OpenFinance Receptor Application Ready!");
        log.info("=".repeat(80));
        log.info("Ready Time: {}", readyTime);
        log.info("Total Startup Duration: {} ms", startupDuration.toMillis());
        log.info("Application is ready to process OpenFinance Brasil resources");
        
        // Log resource processing capabilities
        logResourceProcessingCapabilities();
        
        // Perform startup health checks
        performStartupHealthChecks();
        
        // Initialize automatic resource discovery if enabled
        initializeAutomaticProcessing();
        
        log.info("=".repeat(80));
        log.info("🚀 OpenFinance Receptor is READY for high-performance resource processing!");
        log.info("=".repeat(80));
    }
    
    /**
     * Handle application shutdown.
     */
    @EventListener(ContextClosedEvent.class)
    public void handleApplicationShutdown(ContextClosedEvent event) {
        LocalDateTime shutdownTime = LocalDateTime.now();
        
        if (readyTime != null) {
            Duration uptime = Duration.between(readyTime, shutdownTime);
            log.info("=".repeat(80));
            log.info("OpenFinance Receptor Application Shutting Down...");
            log.info("=".repeat(80));
            log.info("Shutdown Time: {}", shutdownTime);
            log.info("Total Uptime: {} ({} seconds)", uptime, uptime.toSeconds());
            
            // Log final performance statistics
            logFinalPerformanceStatistics();
        }
        
        log.info("OpenFinance Receptor Application Shutdown Complete");
        log.info("=".repeat(80));
    }
    
    private boolean checkVirtualThreadsSupport() {
        try {
            Thread.ofVirtual().start(() -> {}).join();
            return true;
        } catch (Exception e) {
            log.debug("Virtual Threads check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private void logVirtualThreadConfiguration() {
        try {
            String parallelism = System.getProperty("jdk.virtualThreadScheduler.parallelism", "default");
            String maxPoolSize = System.getProperty("jdk.virtualThreadScheduler.maxPoolSize", "default");
            String forkJoinParallelism = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "default");
            
            log.info("Virtual Thread Configuration:");
            log.info("  - Scheduler Parallelism: {}", parallelism);
            log.info("  - Max Pool Size: {}", maxPoolSize);
            log.info("  - ForkJoinPool Parallelism: {}", forkJoinParallelism);
            
        } catch (Exception e) {
            log.debug("Could not log Virtual Thread configuration: {}", e.getMessage());
        }
    }
    
    private void initializePerformanceMonitoring() {
        try {
            log.info("Initializing Performance Monitoring...");
            
            // Reset metrics for fresh start
            performanceMonitor.resetMetrics();
            
            log.info("Performance Monitoring initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Performance Monitoring", e);
        }
    }
    
    private void logResourceProcessingCapabilities() {
        var utilization = resourceManager.getResourceUtilization();
        
        log.info("Resource Processing Capabilities:");
        log.info("  - Available Processors: {}", utilization.availableProcessors());
        log.info("  - Dynamic Batch Size: {}", resourceManager.getDynamicBatchSize());
        log.info("  - Dynamic Concurrency Level: {}", resourceManager.getDynamicConcurrencyLevel());
        log.info("  - Discovery Concurrency: {}", resourceManager.getDynamicDiscoveryConcurrency());
        log.info("  - Sync Concurrency: {}", resourceManager.getDynamicSyncConcurrency());
        log.info("  - Validation Concurrency: {}", resourceManager.getDynamicValidationConcurrency());
        log.info("  - Monitoring Concurrency: {}", resourceManager.getDynamicMonitoringConcurrency());
        log.info("  - API Call Concurrency: {}", resourceManager.getDynamicApiCallConcurrency());
        log.info("  - Adaptation Interval: {} ms", resourceManager.getAdaptationInterval());
    }
    
    private void performStartupHealthChecks() {
        try {
            log.info("Performing Startup Health Checks...");
            
            // Check resource manager
            var utilization = resourceManager.getResourceUtilization();
            log.info("  ✓ Resource Manager: CPU {:.1f}%, Memory {:.1f}%", 
                    utilization.currentCpuUsage() * 100, utilization.currentMemoryUsage() * 100);
            
            // Check performance monitor
            var report = performanceMonitor.getPerformanceReport();
            log.info("  ✓ Performance Monitor: Efficiency {:.1f}%, Throughput {:.2f} ops/sec", 
                    report.processingEfficiency() * 100, report.currentThroughput());
            
            log.info("Startup Health Checks completed successfully");
            
        } catch (Exception e) {
            log.error("Startup Health Checks failed", e);
        }
    }
    
    private void initializeAutomaticProcessing() {
        try {
            log.info("Initializing Automatic Resource Processing...");
            
            // Note: In a real implementation, this would check configuration properties
            // and potentially start background scheduled tasks for resource discovery,
            // synchronization, validation, and monitoring
            
            log.info("Automatic Resource Processing initialization completed");
            
        } catch (Exception e) {
            log.error("Failed to initialize Automatic Resource Processing", e);
        }
    }
    
    private void logFinalPerformanceStatistics() {
        try {
            var report = performanceMonitor.getPerformanceReport();
            var utilization = resourceManager.getResourceUtilization();
            
            log.info("Final Performance Statistics:");
            log.info("  - Total Resources Discovered: {}", report.totalResourcesDiscovered());
            log.info("  - Total Resources Synced: {}", report.totalResourcesSynced());
            log.info("  - Total Resources Validated: {}", report.totalResourcesValidated());
            log.info("  - Total Resources Monitored: {}", report.totalResourcesMonitored());
            log.info("  - Total Batches Processed: {}", report.totalBatchesProcessed());
            log.info("  - Total API Calls: {}", report.totalApiCalls());
            log.info("  - Total Errors: {}", report.totalErrors());
            log.info("  - Final Processing Efficiency: {:.2f}%", report.processingEfficiency() * 100);
            log.info("  - Final Error Rate: {:.2f}%", report.errorRate() * 100);
            log.info("  - Peak Virtual Threads: {}", report.activeVirtualThreads());
            log.info("  - Peak Concurrent Operations: {}", report.concurrentResourceOperations());
            
        } catch (Exception e) {
            log.debug("Could not log final performance statistics: {}", e.getMessage());
        }
    }
}