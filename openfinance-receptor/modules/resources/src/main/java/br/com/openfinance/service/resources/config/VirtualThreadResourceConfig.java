package br.com.openfinance.service.resources.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Advanced Virtual Thread configuration for high-performance resource processing.
 * Optimized for Java 21 Virtual Threads and Structured Concurrency patterns
 * for Open Finance Brasil resource management operations.
 */
@Configuration
@EnableAsync
@EnableScheduling
@ConfigurationProperties(prefix = "openfinance.resources")
public class VirtualThreadResourceConfig {

    /**
     * Virtual Thread executor specifically optimized for resource discovery and cataloging.
     * Uses Virtual Threads for maximum concurrency with minimal resource overhead.
     */
    @Bean("resourceDiscoveryVirtualThreadExecutor")
    public TaskExecutor resourceDiscoveryVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-discovery-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for resource synchronization operations.
     * Optimized for I/O-bound resource sync operations with external systems.
     */
    @Bean("resourceSyncVirtualThreadExecutor")
    public TaskExecutor resourceSyncVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-sync-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for resource validation operations.
     * Uses structured concurrency for parallel validation tasks.
     */
    @Bean("resourceValidationVirtualThreadExecutor")
    public TaskExecutor resourceValidationVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-validation-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for resource monitoring and health checks.
     * Optimized for periodic monitoring of resource availability and performance.
     */
    @Bean("resourceMonitoringVirtualThreadExecutor")
    public TaskExecutor resourceMonitoringVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-monitoring-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for resource indexing and cataloging operations.
     * Designed for high-throughput resource indexing and search operations.
     */
    @Bean("resourceIndexingVirtualThreadExecutor")
    public TaskExecutor resourceIndexingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-indexing-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for resource aggregation and reporting.
     * Used for operations that aggregate resource data from multiple sources.
     */
    @Bean("resourceAggregationVirtualThreadExecutor")
    public TaskExecutor resourceAggregationVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("resource-aggregation-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Structured Concurrency executor for coordinated resource operations.
     * Used for operations that need coordinated execution and error handling.
     */
    @Bean("structuredConcurrencyResourceExecutor")
    public TaskExecutor structuredConcurrencyResourceExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("structured-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for batch resource processing.
     * Optimized for processing large batches of resources with high throughput.
     */
    @Bean("batchResourceProcessingVirtualThreadExecutor")
    public TaskExecutor batchResourceProcessingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("batch-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Platform thread executor for CPU-intensive resource operations.
     * Used for operations that don't benefit from Virtual Threads.
     */
    @Bean("cpuIntensiveResourceTaskExecutor")
    public TaskExecutor cpuIntensiveResourceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("cpu-resource-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Default task executor for general resource operations.
     * Combines Virtual Threads with appropriate configuration for mixed workloads.
     */
    @Bean("defaultResourceTaskExecutor")
    public TaskExecutor defaultResourceTaskExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("default-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Reactive processing executor for WebFlux operations.
     * Optimized for reactive resource processing flows.
     */
    @Bean("reactiveResourceProcessingVirtualThreadExecutor")
    public TaskExecutor reactiveResourceProcessingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("reactive-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Error recovery executor for handling failed resource operations.
     * Uses Virtual Threads for retry and recovery operations.
     */
    @Bean("errorRecoveryResourceVirtualThreadExecutor")
    public TaskExecutor errorRecoveryResourceVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("error-recovery-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Metrics and performance monitoring executor.
     * Dedicated executor for performance monitoring and metrics collection.
     */
    @Bean("metricsResourceVirtualThreadExecutor")
    public TaskExecutor metricsResourceVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("metrics-resource-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }
}