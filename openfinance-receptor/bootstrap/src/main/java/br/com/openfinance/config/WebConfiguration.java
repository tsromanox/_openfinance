package br.com.openfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executors;

/**
 * Web layer configuration for OpenFinance Receptor.
 * Configures Virtual Threads for web request processing and CORS settings.
 */
@Slf4j
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    
    private final TaskExecutor webVirtualThreadExecutor;
    
    public WebConfiguration(TaskExecutor webVirtualThreadExecutor) {
        this.webVirtualThreadExecutor = webVirtualThreadExecutor;
        log.info("Configuring Web layer with Virtual Thread support");
    }
    
    /**
     * Configure CORS for OpenFinance APIs.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        
        registry.addMapping("/actuator/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
        
        log.info("Configured CORS mappings for /api/** and /actuator/**");
    }
    
    /**
     * Configure async support with Virtual Threads.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(webVirtualThreadExecutor);
        configurer.setDefaultTimeout(60000); // 60 seconds
        
        log.info("Configured async support with Virtual Thread executor");
    }
    
    /**
     * Configure Tomcat to use Virtual Threads for request processing.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            log.info("Configuring Tomcat ProtocolHandler to use Virtual Threads");
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}