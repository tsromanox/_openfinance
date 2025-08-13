package br.com.openfinance.resources.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.TaskExecutorAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class VirtualThreadsConfig {

    @Value("${openfinance.resources.virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${openfinance.resources.http.connection.pool.max:200}")
    private int maxConnections;

    @Value("${openfinance.resources.http.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${openfinance.resources.http.read.timeout:60000}")
    private int readTimeout;

    @Bean("virtualThreadTaskExecutor")
    public TaskExecutor virtualThreadTaskExecutor() {
        if (virtualThreadsEnabled) {
            log.info("Configuring Virtual Thread TaskExecutor");
            return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        } else {
            log.info("Virtual Threads disabled, using default TaskExecutor");
            return new TaskExecutorAdapter(Executors.newCachedThreadPool());
        }
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        log.info("Configuring RestTemplate with Virtual Thread support");
        
        // Configure connection pool for Virtual Threads
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections / 4);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(connectionTimeout);
        requestFactory.setConnectionRequestTimeout(connectionTimeout);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        
        log.info("RestTemplate configured with connection pool: max={}, timeout={}ms", 
                maxConnections, connectionTimeout);
        
        return restTemplate;
    }

    @Bean("batchProcessingTaskExecutor")
    public TaskExecutor batchProcessingTaskExecutor() {
        log.info("Configuring batch processing TaskExecutor with Virtual Threads");
        
        if (virtualThreadsEnabled) {
            // For batch processing, we use Virtual Threads for maximum concurrency
            return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        } else {
            // Fallback to fixed thread pool
            return new TaskExecutorAdapter(Executors.newFixedThreadPool(50));
        }
    }

    @Bean("schedulerTaskExecutor") 
    public TaskExecutor schedulerTaskExecutor() {
        log.info("Configuring scheduler TaskExecutor");
        
        if (virtualThreadsEnabled) {
            return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        } else {
            return new TaskExecutorAdapter(Executors.newScheduledThreadPool(10));
        }
    }
}