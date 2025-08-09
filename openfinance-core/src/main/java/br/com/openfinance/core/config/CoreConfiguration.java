// CoreConfiguration.java
package br.com.openfinance.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * Core configuration for OpenFinance services
 * Provides common beans used across all microservices
 */
@Configuration
public class CoreConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 time module
        mapper.registerModule(new JavaTimeModule());

        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean(name = "virtualThreadExecutor")
    @ConditionalOnMissingBean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        // Java 21 Virtual Threads
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "customForkJoinPool")
    @ConditionalOnMissingBean(name = "customForkJoinPool")
    public ForkJoinPool customForkJoinPool() {
        int parallelism = Runtime.getRuntime().availableProcessors() * 4;
        return new ForkJoinPool(
                parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true
        );
    }
}
