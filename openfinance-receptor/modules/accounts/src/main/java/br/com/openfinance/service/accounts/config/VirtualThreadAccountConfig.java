package br.com.openfinance.service.accounts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Advanced Virtual Thread configuration for high-performance account processing.
 * Optimized for Java 21 Virtual Threads and Structured Concurrency patterns.
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "openfinance.accounts")
public class VirtualThreadAccountConfig {

    /**
     * Virtual Thread executor specifically optimized for account synchronization.
     * Uses Virtual Threads for maximum concurrency with minimal resource overhead.
     */
    @Bean("accountSyncVirtualThreadExecutor")
    public TaskExecutor accountSyncVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("account-sync-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for balance processing operations.
     * Optimized for I/O-bound balance update operations.
     */
    @Bean("balanceProcessingVirtualThreadExecutor")
    public TaskExecutor balanceProcessingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("balance-processing-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for account validation operations.
     * Uses structured concurrency for parallel validation tasks.
     */
    @Bean("accountValidationVirtualThreadExecutor")
    public TaskExecutor accountValidationVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("account-validation-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Virtual Thread executor for API call operations.
     * Optimized for external OpenFinance API communications.
     */
    @Bean("apiCallVirtualThreadExecutor")
    public TaskExecutor apiCallVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("api-call-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Batch processing executor using Virtual Threads.
     * Designed for high-throughput batch account processing operations.
     */
    @Bean("batchProcessingVirtualThreadExecutor")
    public TaskExecutor batchProcessingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("batch-processing-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Structured Concurrency executor for coordinated account operations.
     * Used for operations that need coordinated execution and error handling.
     */
    @Bean("structuredConcurrencyVirtualThreadExecutor")
    public TaskExecutor structuredConcurrencyVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("structured-concurrency-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Platform thread executor for CPU-intensive operations.
     * Used for operations that don't benefit from Virtual Threads.
     */
    @Bean("cpuIntensiveTaskExecutor")
    public TaskExecutor cpuIntensiveTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cpu-intensive-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Default task executor for general account operations.
     * Combines Virtual Threads with appropriate configuration for mixed workloads.
     */
    @Bean("defaultAccountTaskExecutor")
    public TaskExecutor defaultAccountTaskExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("default-account-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Reactive processing executor for WebFlux operations.
     * Optimized for reactive account processing flows.
     */
    @Bean("reactiveProcessingVirtualThreadExecutor")
    public TaskExecutor reactiveProcessingVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("reactive-processing-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Error recovery executor for handling failed operations.
     * Uses Virtual Threads for retry and recovery operations.
     */
    @Bean("errorRecoveryVirtualThreadExecutor")
    public TaskExecutor errorRecoveryVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("error-recovery-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }

    /**
     * Monitoring and metrics executor.
     * Dedicated executor for performance monitoring and metrics collection.
     */
    @Bean("monitoringVirtualThreadExecutor")
    public TaskExecutor monitoringVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("monitoring-", 0)
                .factory();
        
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }
}