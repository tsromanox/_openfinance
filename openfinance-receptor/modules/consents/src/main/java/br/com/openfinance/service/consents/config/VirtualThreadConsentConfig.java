package br.com.openfinance.service.consents.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.TaskExecutorAdapter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Virtual Thread configuration specifically optimized for consent processing operations.
 * Provides multiple execution strategies for different consent workload types.
 */
@Slf4j
@Configuration
@EnableAsync
public class VirtualThreadConsentConfig {

    @Value("${openfinance.consents.virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${openfinance.consents.virtual-threads.max-pool-size:2000}")
    private int maxVirtualThreadPoolSize;

    @Value("${openfinance.consents.batch.size:200}")
    private int defaultBatchSize;

    @Value("${openfinance.consents.batch.parallel-factor:20}")
    private int parallelFactor;

    private final MeterRegistry meterRegistry;

    public VirtualThreadConsentConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Primary Virtual Thread executor for consent processing operations.
     */
    @Bean("consentVirtualThreadExecutor")
    @Primary
    public TaskExecutor consentVirtualThreadExecutor() {
        if (virtualThreadsEnabled) {
            log.info("Configuring Virtual Thread executor for consent processing with unlimited concurrency");
            
            return new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor()
            );
        } else {
            log.warn("Virtual Threads disabled for consents, falling back to platform threads");
            return new TaskExecutorAdapter(
                Executors.newCachedThreadPool(createNamedThreadFactory("consent-platform"))
            );
        }
    }

    /**
     * Structured Concurrency executor for batch consent operations.
     */
    @Bean("consentStructuredConcurrencyExecutor")
    public TaskExecutor consentStructuredConcurrencyExecutor() {
        log.info("Configuring Structured Concurrency executor for batch consent operations");
        
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * High-performance executor for consent validation operations.
     */
    @Bean("consentValidationExecutor")
    public TaskExecutor consentValidationExecutor() {
        log.info("Configuring Virtual Thread executor for consent validation with maximum concurrency");
        
        return new TaskExecutorAdapter(
            virtualThreadsEnabled 
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(Math.min(200, maxVirtualThreadPoolSize))
        );
    }

    /**
     * Executor optimized for consent lifecycle operations (create, update, revoke).
     */
    @Bean("consentLifecycleExecutor")
    public TaskExecutor consentLifecycleExecutor() {
        log.info("Configuring consent lifecycle executor with Virtual Threads");
        
        return new TaskExecutorAdapter(
            virtualThreadsEnabled 
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(Math.min(100, maxVirtualThreadPoolSize))
        );
    }

    /**
     * Executor for external API calls to OpenFinance institutions.
     */
    @Bean("consentApiCallExecutor")
    public TaskExecutor consentApiCallExecutor() {
        log.info("Configuring API call executor for external consent operations");
        
        return new TaskExecutorAdapter(
            virtualThreadsEnabled 
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(Math.min(500, maxVirtualThreadPoolSize))
        );
    }

    /**
     * Scheduled executor for periodic consent operations using Virtual Threads.
     */
    @Bean("consentScheduledExecutor")
    public ScheduledExecutorService consentScheduledExecutor() {
        if (virtualThreadsEnabled) {
            log.info("Creating Virtual Thread ScheduledExecutorService for consent operations");
            
            // Create a platform thread scheduler that submits tasks to virtual threads
            return Executors.newScheduledThreadPool(
                parallelFactor, 
                createNamedThreadFactory("consent-vt-scheduler")
            );
        } else {
            return Executors.newScheduledThreadPool(
                parallelFactor, 
                createNamedThreadFactory("consent-scheduler")
            );
        }
    }

    /**
     * CPU-intensive executor for consent data processing (uses platform threads).
     */
    @Bean("consentProcessingExecutor")
    public TaskExecutor consentProcessingExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(cpuCores * 2, parallelFactor);
        
        log.info("Configuring consent processing executor with {} platform threads", poolSize);
        
        return new TaskExecutorAdapter(
            Executors.newFixedThreadPool(poolSize, createNamedThreadFactory("consent-processing"))
        );
    }

    /**
     * Create named thread factory for better monitoring and debugging.
     */
    private ThreadFactory createNamedThreadFactory(String namePrefix) {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, namePrefix + "-" + counter.getAndIncrement());
                thread.setDaemon(false);
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };
    }

    /**
     * Configuration properties record for consent processing.
     */
    public record ConsentConfig(
            boolean virtualThreadsEnabled,
            int maxVirtualThreadPoolSize,
            int defaultBatchSize,
            int parallelFactor
    ) {}

    @Bean
    public ConsentConfig consentConfig() {
        return new ConsentConfig(
                virtualThreadsEnabled,
                maxVirtualThreadPoolSize,
                defaultBatchSize,
                parallelFactor
        );
    }
}