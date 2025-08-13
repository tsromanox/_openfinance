package br.com.openfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Virtual Thread configuration for OpenFinance Receptor Application.
 * Configures Virtual Threads for maximum performance and resource efficiency.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "openfinance.virtual-threads.enabled", havingValue = "true", matchIfMissing = true)
public class VirtualThreadConfiguration implements AsyncConfigurer {
    
    /**
     * Primary Virtual Thread executor for general async operations.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(value = "spring.threads.virtual.enabled", havingValue = "true", matchIfMissing = true)
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("virtual-");
        executor.setVirtualThreads(true);
        executor.setTaskTerminationTimeout(java.time.Duration.ofSeconds(30));
        executor.setConcurrencyLimit(10000);
        
        log.info("Configured primary Virtual Thread executor with concurrency limit: {}", 
                executor.getConcurrencyLimit());
        
        return executor;
    }
    
    /**
     * Virtual Thread executor for web request processing.
     */
    @Bean("webVirtualThreadExecutor")
    public TaskExecutor webVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("web-virtual-", 0)
                .factory();
        
        Executor executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        log.info("Configured web Virtual Thread executor");
        
        return new TaskExecutorAdapter(executor);
    }
    
    /**
     * Virtual Thread executor for database operations.
     */
    @Bean("databaseVirtualThreadExecutor")
    public TaskExecutor databaseVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("database-virtual-", 0)
                .factory();
        
        Executor executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        log.info("Configured database Virtual Thread executor");
        
        return new TaskExecutorAdapter(executor);
    }
    
    /**
     * Virtual Thread executor for HTTP client operations.
     */
    @Bean("httpClientVirtualThreadExecutor")
    public TaskExecutor httpClientVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("http-client-", 0)
                .factory();
        
        Executor executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        log.info("Configured HTTP client Virtual Thread executor");
        
        return new TaskExecutorAdapter(executor);
    }
    
    /**
     * Virtual Thread executor for scheduled tasks.
     */
    @Bean("scheduledVirtualThreadExecutor")
    public TaskExecutor scheduledVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("scheduled-", 0)
                .factory();
        
        Executor executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        log.info("Configured scheduled Virtual Thread executor");
        
        return new TaskExecutorAdapter(executor);
    }
    
    /**
     * Virtual Thread executor for background processing.
     */
    @Bean("backgroundVirtualThreadExecutor")
    public TaskExecutor backgroundVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("background-", 0)
                .factory();
        
        Executor executor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);
        
        log.info("Configured background Virtual Thread executor");
        
        return new TaskExecutorAdapter(executor);
    }
    
    /**
     * Fallback platform thread executor for operations that don't work well with Virtual Threads.
     */
    @Bean("platformThreadExecutor")
    public ThreadPoolTaskExecutor platformThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("platform-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        log.info("Configured platform thread executor - core: {}, max: {}, queue: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
    
    /**
     * Override default async executor to use Virtual Threads.
     */
    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }
    
    /**
     * Custom async uncaught exception handler.
     */
    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async method execution failed: {}.{}() with params: {}", 
                    method.getDeclaringClass().getSimpleName(), method.getName(), params, throwable);
        };
    }
    
    /**
     * Task executor customizer to ensure Virtual Threads are used where possible.
     */
    @Bean
    @ConditionalOnProperty(value = "spring.threads.virtual.enabled", havingValue = "false")
    public ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder() {
        return new ThreadPoolTaskExecutorBuilder()
                .corePoolSize(Runtime.getRuntime().availableProcessors())
                .maxPoolSize(Runtime.getRuntime().availableProcessors() * 8)
                .queueCapacity(5000)
                .keepAlive(java.time.Duration.ofSeconds(60))
                .threadNamePrefix("app-")
                .awaitTermination(true)
                .awaitTerminationPeriod(java.time.Duration.ofSeconds(30));
    }
    
    /**
     * Virtual Thread configuration info bean.
     */
    @Bean
    public VirtualThreadInfo virtualThreadInfo() {
        boolean virtualThreadsEnabled = isVirtualThreadsSupported();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024; // MB
        
        log.info("Virtual Thread Configuration Summary:");
        log.info("  - Virtual Threads Supported: {}", virtualThreadsEnabled);
        log.info("  - Available Processors: {}", availableProcessors);
        log.info("  - Max Memory: {} MB", maxMemory);
        log.info("  - Recommended Virtual Thread Pool Size: {}", availableProcessors * 1000);
        
        return new VirtualThreadInfo(virtualThreadsEnabled, availableProcessors, maxMemory);
    }
    
    private boolean isVirtualThreadsSupported() {
        try {
            Thread.ofVirtual().start(() -> {}).join();
            return true;
        } catch (Exception e) {
            log.warn("Virtual Threads not supported: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Virtual Thread information record.
     */
    public record VirtualThreadInfo(
            boolean virtualThreadsSupported,
            int availableProcessors,
            long maxMemoryMB
    ) {}
}