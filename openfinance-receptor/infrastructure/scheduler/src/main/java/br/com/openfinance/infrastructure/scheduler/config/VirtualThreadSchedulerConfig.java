package br.com.openfinance.infrastructure.scheduler.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.TaskExecutorAdapter;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

/**
 * Advanced scheduler configuration optimized for Virtual Threads and high concurrency.
 * Provides multiple execution strategies for different workload types.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class VirtualThreadSchedulerConfig {

    @Value("${openfinance.scheduler.virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${openfinance.scheduler.virtual-threads.max-pool-size:1000}")
    private int maxVirtualThreadPoolSize;

    @Value("${openfinance.scheduler.batch.size:100}")
    private int defaultBatchSize;

    @Value("${openfinance.scheduler.batch.parallel-factor:10}")
    private int parallelFactor;

    private final MeterRegistry meterRegistry;

    public VirtualThreadSchedulerConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Primary task executor using Virtual Threads for maximum concurrency.
     */
    @Bean("virtualThreadTaskExecutor")
    @Primary
    public TaskExecutor virtualThreadTaskExecutor() {
        if (virtualThreadsEnabled) {
            log.info("Configuring Virtual Thread TaskExecutor with unlimited concurrency");
            
            return new TaskExecutorAdapter(
                Executors.newVirtualThreadPerTaskExecutor()
            );
        } else {
            log.warn("Virtual Threads disabled, falling back to platform threads");
            return new TaskExecutorAdapter(
                Executors.newCachedThreadPool(createNamedThreadFactory("openfinance-scheduler"))
            );
        }
    }

    /**
     * Structured Concurrency executor for batch processing operations.
     */
    @Bean("structuredConcurrencyExecutor")
    public TaskExecutor structuredConcurrencyExecutor() {
        log.info("Configuring Structured Concurrency executor for batch operations");
        
        return new TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * Scheduled executor service for periodic tasks using Virtual Threads.
     */
    @Bean("virtualThreadScheduledExecutor")
    public ScheduledExecutorService virtualThreadScheduledExecutor() {
        if (virtualThreadsEnabled) {
            log.info("Creating Virtual Thread ScheduledExecutorService");
            
            // Virtual Threads don't have a built-in scheduled executor, 
            // so we create a platform thread scheduler that submits tasks to virtual threads
            return Executors.newScheduledThreadPool(
                parallelFactor, 
                createNamedThreadFactory("vt-scheduler")
            );
        } else {
            return Executors.newScheduledThreadPool(
                parallelFactor, 
                createNamedThreadFactory("platform-scheduler")
            );
        }
    }

    /**
     * High-performance task executor for I/O bound operations.
     */
    @Bean("ioOptimizedExecutor")
    public TaskExecutor ioOptimizedTaskExecutor() {
        log.info("Configuring I/O optimized executor with Virtual Threads");
        
        return new TaskExecutorAdapter(
            virtualThreadsEnabled 
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(Math.min(500, maxVirtualThreadPoolSize))
        );
    }

    /**
     * CPU-intensive task executor (uses platform threads for better performance).
     */
    @Bean("cpuOptimizedExecutor")
    public TaskExecutor cpuOptimizedTaskExecutor() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int poolSize = Math.max(cpuCores, parallelFactor);
        
        log.info("Configuring CPU optimized executor with {} platform threads", poolSize);
        
        return new TaskExecutorAdapter(
            Executors.newFixedThreadPool(poolSize, createNamedThreadFactory("cpu-optimized"))
        );
    }

    /**
     * Quartz scheduler factory bean for complex scheduling scenarios.
     */
    @Bean
    public SchedulerFactoryBean quartzScheduler() throws SchedulerException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setTaskExecutor(virtualThreadTaskExecutor());
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);
        
        // Configure Quartz to use our task executor
        factory.setQuartzProperties(quartzProperties());
        
        log.info("Configuring Quartz Scheduler with Virtual Thread support");
        return factory;
    }

    /**
     * Quartz scheduler bean for programmatic job scheduling.
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean quartzScheduler) throws SchedulerException {
        Scheduler scheduler = quartzScheduler.getScheduler();
        
        // Start the scheduler
        scheduler.start();
        
        log.info("Quartz Scheduler started successfully");
        return scheduler;
    }

    /**
     * Job detail for batch processing operations.
     */
    @Bean
    public JobDetail batchProcessingJobDetail() {
        return newJob(VirtualThreadBatchProcessingJob.class)
                .withIdentity("batchProcessingJob", "openfinance")
                .withDescription("Virtual Thread batch processing job")
                .storeDurably()
                .build();
    }

    /**
     * Trigger for batch processing operations.
     */
    @Bean
    public Trigger batchProcessingTrigger(JobDetail batchProcessingJobDetail) {
        return newTrigger()
                .forJob(batchProcessingJobDetail)
                .withIdentity("batchProcessingTrigger", "openfinance")
                .withDescription("Trigger for batch processing every 30 seconds")
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(30)
                        .repeatForever())
                .build();
    }

    /**
     * Quartz properties for optimal performance.
     */
    private java.util.Properties quartzProperties() {
        java.util.Properties props = new java.util.Properties();
        
        // Thread pool configuration
        props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.put("org.quartz.threadPool.threadCount", String.valueOf(parallelFactor));
        props.put("org.quartz.threadPool.threadPriority", "5");
        
        // Job store configuration (RAM-based for this example)
        props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        
        // Scheduler configuration
        props.put("org.quartz.scheduler.instanceName", "OpenFinanceVirtualThreadScheduler");
        props.put("org.quartz.scheduler.instanceId", "AUTO");
        props.put("org.quartz.scheduler.rmi.export", "false");
        props.put("org.quartz.scheduler.rmi.proxy", "false");
        props.put("org.quartz.scheduler.wrapJobExecutionInUserTransaction", "false");
        
        // Performance optimizations
        props.put("org.quartz.scheduler.skipUpdateCheck", "true");
        props.put("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", String.valueOf(defaultBatchSize));
        props.put("org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow", "5000");
        
        return props;
    }

    /**
     * Create named thread factory for better monitoring.
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
     * Configuration properties record.
     */
    public record SchedulerConfig(
            boolean virtualThreadsEnabled,
            int maxVirtualThreadPoolSize,
            int defaultBatchSize,
            int parallelFactor
    ) {}

    @Bean
    public SchedulerConfig schedulerConfig() {
        return new SchedulerConfig(
                virtualThreadsEnabled,
                maxVirtualThreadPoolSize,
                defaultBatchSize,
                parallelFactor
        );
    }
}