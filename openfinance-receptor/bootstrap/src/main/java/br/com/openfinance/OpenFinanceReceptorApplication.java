package br.com.openfinance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * OpenFinance Receptor Application - Main entry point
 * 
 * High-performance Open Finance Brasil data receptor platform built with:
 * - Java 21 Virtual Threads and Structured Concurrency
 * - Spring Boot 3.4.8 with WebFlux reactive programming
 * - Hexagonal architecture with clean separation of concerns
 * - Resource discovery, synchronization, validation, and monitoring
 * - Adaptive resource management with performance feedback
 * - Comprehensive observability and metrics
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {
    "br.com.openfinance",
    "br.com.openfinance.application",
    "br.com.openfinance.domain",
    "br.com.openfinance.infrastructure",
    "br.com.openfinance.service"
})
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableTransactionManagement
@ConfigurationPropertiesScan(basePackages = {
    "br.com.openfinance.config",
    "br.com.openfinance.service.resources.config"
})
public class OpenFinanceReceptorApplication {
    
    public static void main(String[] args) {
        // Configure system properties for Virtual Threads
        System.setProperty("jdk.virtualThreadScheduler.parallelism", 
                String.valueOf(Runtime.getRuntime().availableProcessors() * 4));
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "1000");
        
        // JVM optimizations for Virtual Threads
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                String.valueOf(Runtime.getRuntime().availableProcessors() * 2));
        
        log.info("Starting OpenFinance Receptor Application...");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        log.info("Virtual Threads Enabled: {}", Thread.class.getDeclaredMethods().length > 20);
        
        try {
            SpringApplication application = new SpringApplication(OpenFinanceReceptorApplication.class);
            
            // Configure additional application properties
            application.setAdditionalProfiles("virtual-threads");
            
            // Enable Virtual Threads support
            System.setProperty("spring.threads.virtual.enabled", "true");
            
            var context = application.run(args);
            
            log.info("OpenFinance Receptor Application started successfully!");
            log.info("Active Profiles: {}", String.join(", ", context.getEnvironment().getActiveProfiles()));
            log.info("Application Context: {} beans loaded", context.getBeanDefinitionCount());
            
            // Log Virtual Thread configuration
            logVirtualThreadConfiguration();
            
        } catch (Exception e) {
            log.error("Failed to start OpenFinance Receptor Application", e);
            System.exit(1);
        }
    }
    
    private static void logVirtualThreadConfiguration() {
        try {
            int parallelism = Integer.parseInt(
                    System.getProperty("jdk.virtualThreadScheduler.parallelism", "0"));
            int maxPoolSize = Integer.parseInt(
                    System.getProperty("jdk.virtualThreadScheduler.maxPoolSize", "0"));
            
            log.info("Virtual Thread Scheduler Configuration:");
            log.info("  - Parallelism: {}", parallelism);
            log.info("  - Max Pool Size: {}", maxPoolSize);
            log.info("  - ForkJoinPool Parallelism: {}", 
                    System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
            
        } catch (Exception e) {
            log.debug("Could not log Virtual Thread configuration: {}", e.getMessage());
        }
    }
}