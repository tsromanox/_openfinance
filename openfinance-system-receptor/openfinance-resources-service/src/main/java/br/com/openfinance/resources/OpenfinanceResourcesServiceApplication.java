package br.com.openfinance.resources;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = {
    "br.com.openfinance.resources",
    "br.com.openfinance.core"
})
@Slf4j
public class OpenfinanceResourcesServiceApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads preview feature
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "10000");
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "100000");
        
        log.info("Starting OpenFinance Resources Service with Virtual Threads support");
        
        ConfigurableApplicationContext context = SpringApplication.run(OpenfinanceResourcesServiceApplication.class, args);
        
        // Log Virtual Threads information
        Thread currentThread = Thread.currentThread();
        log.info("Application started on thread: {} (Virtual: {})", 
                currentThread.getName(), 
                currentThread.isVirtual());
        
        // Log active profiles
        String[] activeProfiles = context.getEnvironment().getActiveProfiles();
        log.info("Active profiles: {}", String.join(", ", activeProfiles));
        
        log.info("OpenFinance Resources Service started successfully");
    }
}