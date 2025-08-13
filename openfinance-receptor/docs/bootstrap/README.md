# Bootstrap Module

## Overview

The `bootstrap` module is the application assembly and entry point for the OpenFinance Receptor platform. It configures dependency injection, Virtual Thread support, security, web layers, and provides comprehensive health monitoring. This module brings together all other modules and external frameworks into a cohesive, production-ready application.

## Architecture

```
bootstrap/
├── src/main/java/br/com/openfinance/
│   ├── OpenFinanceReceptorApplication.java    # Main application class
│   ├── config/                                # Configuration classes
│   │   ├── VirtualThreadConfiguration.java    # Virtual Thread setup
│   │   ├── ResourceConfiguration.java         # Resource service configuration
│   │   ├── InfrastructureConfiguration.java   # Infrastructure layer config
│   │   ├── WebConfiguration.java              # Web layer configuration
│   │   └── SecurityConfiguration.java         # Security configuration
│   ├── controller/                            # REST API controllers
│   │   └── ResourceController.java            # Main resource endpoints
│   ├── health/                                # Health indicators
│   │   └── ResourceHealthIndicator.java       # Custom health checks
│   └── startup/                               # Application lifecycle
│       └── ApplicationStartupListener.java    # Startup/shutdown events
├── src/main/resources/
│   └── application.yml                        # Multi-profile configuration
└── pom.xml                                    # Maven configuration
```

## Application Entry Point

### OpenFinanceReceptorApplication
**Location**: `br.com.openfinance.OpenFinanceReceptorApplication`

The main Spring Boot application class with Virtual Thread optimizations and comprehensive startup logging.

```java
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
@ConfigurationPropertiesScan
public class OpenFinanceReceptorApplication {
    
    public static void main(String[] args) {
        // Configure system properties for Virtual Threads
        System.setProperty("jdk.virtualThreadScheduler.parallelism", 
                String.valueOf(Runtime.getRuntime().availableProcessors() * 4));
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "1000");
        
        // JVM optimizations for Virtual Threads
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
                String.valueOf(Runtime.getRuntime().availableProcessors() * 2));
        
        // Enhanced logging and Virtual Thread validation
        logSystemConfiguration();
        
        SpringApplication application = new SpringApplication(OpenFinanceReceptorApplication.class);
        application.setAdditionalProfiles("virtual-threads");
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        var context = application.run(args);
        logApplicationStartup(context);
    }
}
```

**Key Features**:
- **Virtual Thread Configuration**: System-level Virtual Thread parameters
- **Component Scanning**: Comprehensive scanning of all modules
- **Profile Management**: Automatic Virtual Thread profile activation
- **Startup Validation**: Comprehensive system validation and logging
- **Context Information**: Detailed application context reporting

### Maven Configuration (pom.xml)

Comprehensive Maven setup with Java 21, Virtual Threads, and multi-profile support.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.8</version>
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    
    <!-- Framework versions -->
    <spring-boot.version>3.4.8</spring-boot.version>
    <spring-cloud.version>2025.0.0</spring-cloud.version>
    <postgresql.version>42.7.4</postgresql.version>
    <resilience4j.version>2.3.0</resilience4j.version>
    <testcontainers.version>1.20.4</testcontainers.version>
</properties>
```

**Key Dependencies**:
- **All Internal Modules**: core-domain, core-application, service-resources, infrastructure-*
- **Spring Boot Starters**: web, webflux, data-jpa, actuator, security, validation
- **Database Support**: PostgreSQL, Flyway migrations, HikariCP connection pooling
- **Observability**: Micrometer, Prometheus, OpenTelemetry
- **Resilience**: Resilience4j circuit breakers, retry, rate limiting
- **Testing**: JUnit 5, TestContainers, Reactive testing
- **Documentation**: SpringDoc OpenAPI

**Build Configuration**:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>
            --enable-preview
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util.concurrent=ALL-UNNAMED
            -XX:+UseZGC
            -XX:+EnableVirtualThreads
            -Xmx4g
            -Xms1g
        </jvmArguments>
    </configuration>
</plugin>
```

**Multi-Profile Support**:
- **development**: 4GB RAM, debug logging, relaxed security
- **production**: 8GB RAM, optimized logging, full security
- **performance**: 12GB RAM, performance testing configuration

## Configuration Classes

### 1. Virtual Thread Configuration

#### VirtualThreadConfiguration
**Location**: `br.com.openfinance.config.VirtualThreadConfiguration`

Comprehensive Virtual Thread setup with specialized executors and fallback platform threads.

```java
@Configuration
@ConditionalOnProperty(value = "openfinance.virtual-threads.enabled", havingValue = "true")
public class VirtualThreadConfiguration implements AsyncConfigurer {
    
    /**
     * Primary Virtual Thread executor for general async operations.
     */
    @Bean
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("virtual-");
        executor.setVirtualThreads(true);
        executor.setTaskTerminationTimeout(Duration.ofSeconds(30));
        executor.setConcurrencyLimit(10000);
        return executor;
    }
    
    /**
     * Specialized executors for different operation types.
     */
    @Bean("webVirtualThreadExecutor")
    public TaskExecutor webVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("web-virtual-", 0)
                .factory();
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }
    
    @Bean("databaseVirtualThreadExecutor")
    public TaskExecutor databaseVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("database-virtual-", 0)
                .factory();
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }
    
    /**
     * Fallback platform thread executor for operations incompatible with Virtual Threads.
     */
    @Bean("platformThreadExecutor")
    public ThreadPoolTaskExecutor platformThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("platform-");
        executor.initialize();
        return executor;
    }
}
```

**Executor Types**:
- **Application Executor**: Primary async operations (10,000 concurrent limit)
- **Web Executor**: HTTP request processing
- **Database Executor**: Database I/O operations
- **HTTP Client Executor**: External API calls
- **Scheduled Executor**: Background scheduled tasks
- **Platform Executor**: Fallback for CPU-intensive operations

### 2. Resource Service Configuration

#### ResourceConfiguration
**Location**: `br.com.openfinance.config.ResourceConfiguration`

Integration configuration for resource processing components with dependency injection.

```java
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "openfinance.resources.enabled", havingValue = "true")
@Import({VirtualThreadResourceConfig.class})
public class ResourceConfiguration {
    
    @Bean
    public ResourceService resourceService(
            ResourceRepository resourceRepository,
            ResourceDiscoveryService resourceDiscoveryService,
            VirtualThreadResourceService virtualThreadResourceService) {
        
        return new ResourceService(
                resourceRepository,
                resourceDiscoveryService,
                virtualThreadResourceService
        );
    }
    
    /**
     * In-memory implementation for development and testing.
     */
    @Bean
    public ResourceRepository resourceRepository() {
        return new InMemoryResourceRepository();
    }
    
    /**
     * Resource discovery service with Open Finance Brasil integration.
     */
    @Bean
    public ResourceDiscoveryService resourceDiscoveryService() {
        return new OpenFinanceResourceDiscoveryService();
    }
}
```

### 3. Web Layer Configuration

#### WebConfiguration
**Location**: `br.com.openfinance.config.WebConfiguration`

Web layer optimization with Virtual Threads and CORS configuration.

```java
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    
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
    }
    
    /**
     * Configure async support with Virtual Threads.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(webVirtualThreadExecutor);
        configurer.setDefaultTimeout(60000); // 60 seconds
    }
    
    /**
     * Configure Tomcat to use Virtual Threads for request processing.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}
```

### 4. Security Configuration

#### SecurityConfiguration
**Location**: `br.com.openfinance.config.SecurityConfiguration`

Multi-profile security with differentiated access control for development and production.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    /**
     * Development security - permissive for testing.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "development")
    public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/resources/health").permitAll()
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }
    
    /**
     * Production security - restrictive access control.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "production")
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/resources/health").permitAll()
                
                // Admin-only endpoints
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/discover").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/sync").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/process/**").hasRole("ADMIN")
                
                // Read-only access
                .requestMatchers("/api/v1/resources/search").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/resources/*").hasAnyRole("USER", "ADMIN")
                
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .build();
    }
}
```

## Health Monitoring

### ResourceHealthIndicator
**Location**: `br.com.openfinance.health.ResourceHealthIndicator`

Comprehensive health indicator providing detailed system status and performance metrics.

```java
@Component
@ConditionalOnProperty(value = "openfinance.resources.enabled", havingValue = "true")
public class ResourceHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            var performanceReport = performanceMonitor.getPerformanceReport();
            var resourceUtilization = resourceManager.getResourceUtilization();
            
            boolean healthy = isSystemHealthy(performanceReport, resourceUtilization);
            
            var healthBuilder = healthy ? Health.up() : Health.down();
            
            return healthBuilder
                .withDetail("performance", createPerformanceDetails(performanceReport))
                .withDetail("utilization", createUtilizationDetails(resourceUtilization))
                .withDetail("virtualThreads", createVirtualThreadDetails(performanceReport, resourceUtilization))
                .withDetail("adaptiveSettings", createAdaptiveSettingsDetails())
                .build();
                
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    
    private boolean isSystemHealthy(
            ResourcePerformanceReport performanceReport,
            ResourceUtilization resourceUtilization) {
        
        return resourceUtilization.currentCpuUsage() < 0.95
            && resourceUtilization.currentMemoryUsage() < 0.95
            && performanceReport.errorRate() < 0.25
            && performanceReport.processingEfficiency() > 0.60;
    }
}
```

**Health Check Criteria**:
- **CPU Usage**: < 95% for healthy status
- **Memory Usage**: < 95% for healthy status  
- **Error Rate**: < 25% for healthy status
- **Processing Efficiency**: > 60% for healthy status

## Application Lifecycle

### ApplicationStartupListener
**Location**: `br.com.openfinance.startup.ApplicationStartupListener`

Comprehensive application lifecycle management with detailed startup/shutdown logging and health checks.

```java
@Component
public class ApplicationStartupListener {
    
    @EventListener(ApplicationStartingEvent.class)
    public void handleApplicationStarting(ApplicationStartingEvent event) {
        log.info("=".repeat(80));
        log.info("OpenFinance Receptor Application Starting...");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        log.info("Virtual Threads Supported: {}", checkVirtualThreadsSupport());
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReady(ApplicationReadyEvent event) {
        Duration startupDuration = Duration.between(startupTime, LocalDateTime.now());
        
        log.info("=".repeat(80));
        log.info("OpenFinance Receptor Application Ready!");
        log.info("Total Startup Duration: {} ms", startupDuration.toMillis());
        log.info("Application is ready to process OpenFinance Brasil resources");
        
        logResourceProcessingCapabilities();
        performStartupHealthChecks();
        initializeAutomaticProcessing();
        
        log.info("🚀 OpenFinance Receptor is READY for high-performance resource processing!");
    }
    
    @EventListener(ContextClosedEvent.class)
    public void handleApplicationShutdown(ContextClosedEvent event) {
        Duration uptime = Duration.between(readyTime, LocalDateTime.now());
        
        log.info("OpenFinance Receptor Application Shutting Down...");
        log.info("Total Uptime: {} ({} seconds)", uptime, uptime.toSeconds());
        
        logFinalPerformanceStatistics();
        log.info("OpenFinance Receptor Application Shutdown Complete");
    }
}
```

**Lifecycle Phases**:
1. **Starting**: System validation, Virtual Thread checks, environment logging
2. **Started**: Spring context validation, component registration
3. **Ready**: Health checks, capability logging, automatic process initialization
4. **Shutdown**: Performance statistics, graceful component shutdown

## REST API Controllers

### ResourceController
**Location**: `br.com.openfinance.controller.ResourceController`

Main REST API controller providing high-level resource management endpoints.

```java
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    
    /**
     * Discover resources from Open Finance Brasil directories.
     */
    @PostMapping("/discover")
    @Timed(name = "resource.discover", description = "Resource discovery operation")
    public CompletableFuture<ResponseEntity<ResourceDiscoveryResponse>> discoverResources(
            @RequestBody List<String> discoveryEndpoints) {
        
        return resourceService.discoverResources(discoveryEndpoints)
                .thenApply(resources -> {
                    var response = new ResourceDiscoveryResponse(
                            resources.size(), 0, "VIRTUAL_THREADS", true,
                            "Resource discovery completed successfully",
                            mapToResourceInfo(resources)
                    );
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    var response = new ResourceDiscoveryResponse(
                            0, 0, "FAILED", false, e.getMessage(), List.of()
                    );
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Health check endpoint with detailed system information.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        var utilization = resourceManager.getResourceUtilization();
        var report = performanceMonitor.getPerformanceReport();
        
        boolean healthy = utilization.currentCpuUsage() < 0.90 
                && utilization.currentMemoryUsage() < 0.90
                && report.errorRate() < 0.20;
        
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "cpuUsage", String.format("%.2f%%", utilization.currentCpuUsage() * 100),
                "memoryUsage", String.format("%.2f%%", utilization.currentMemoryUsage() * 100),
                "errorRate", String.format("%.2f%%", report.errorRate() * 100),
                "throughput", String.format("%.2f ops/sec", report.currentThroughput()),
                "activeVirtualThreads", report.activeVirtualThreads()
        ));
    }
}
```

## Configuration Management

### application.yml
**Location**: `src/main/resources/application.yml`

Comprehensive multi-profile configuration supporting development, production, test, and performance scenarios.

```yaml
spring:
  application:
    name: openfinance-receptor
  
  profiles:
    active: development
    include:
      - virtual-threads
      - resources
  
  # Virtual Threads configuration
  threads:
    virtual:
      enabled: true
  
  # Database configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/openfinance_receptor
    username: openfinance
    password: openfinance
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000

# OpenFinance specific configuration
openfinance:
  virtual-threads:
    enabled: true
    max-pool-size: 10000
    
  resources:
    enabled: true
    discovery:
      enabled: true
      automatic:
        enabled: true
        interval: 7200s  # 2 hours
    sync:
      enabled: true
      automatic:
        enabled: true
        interval: 3600s  # 1 hour

# Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: "health,metrics,prometheus,info,env,threaddump,flyway"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

**Profile Variations**:

**Development Profile**:
- PostgreSQL development database
- Debug logging enabled
- Relaxed security settings
- Faster automatic processing intervals

**Production Profile**:
- External database configuration
- Optimized logging levels
- Full security enabled
- Standard processing intervals

**Test Profile**:
- H2 in-memory database
- Automatic processing disabled
- Debug logging for tests

## Docker Support

### Container Configuration
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>openfinance-receptor:${project.version}</name>
            <env>
                <BPL_JVM_VERSION>21</BPL_JVM_VERSION>
                <BPL_JVM_THREAD_COUNT>-XX:+EnableVirtualThreads</BPL_JVM_THREAD_COUNT>
            </env>
        </image>
    </configuration>
</plugin>
```

### Deployment Commands

**Development**:
```bash
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=development
```

**Production**:
```bash
mvn clean install -Pproduction
mvn spring-boot:run -Pproduction
```

**Docker Build**:
```bash
mvn spring-boot:build-image
docker run -p 8080:8080 openfinance-receptor:1.0.0
```

## Monitoring and Observability

### Actuator Endpoints
- **Health**: `/actuator/health` - System health with custom indicators
- **Metrics**: `/actuator/metrics` - Micrometer metrics
- **Prometheus**: `/actuator/prometheus` - Prometheus format metrics
- **Info**: `/actuator/info` - Application information
- **Thread Dump**: `/actuator/threaddump` - Virtual Thread analysis

### Custom Metrics
- Resource processing throughput
- Virtual Thread utilization
- Error rates by operation type
- System resource usage
- Adaptive setting changes

### Logging Configuration
- Structured logging with correlation IDs
- Resource-specific context (resource-id, organization-id)
- Performance logging at configurable intervals
- Virtual Thread debugging support

## Testing Strategy

### Integration Testing
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "openfinance.resources.enabled=true"
})
class BootstrapIntegrationTest {
    
    @Autowired
    private ResourceController resourceController;
    
    @Autowired
    private ResourceHealthIndicator healthIndicator;
    
    @Test
    void contextLoads() {
        assertThat(resourceController).isNotNull();
        assertThat(healthIndicator).isNotNull();
    }
    
    @Test
    void healthCheckReturnsStatus() {
        var health = healthIndicator.health();
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN);
    }
}
```

### Performance Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PerformanceIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldHandleConcurrentRequests() {
        int numRequests = 100;
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();
        
        for (int i = 0; i < numRequests; i++) {
            CompletableFuture<ResponseEntity<String>> future = CompletableFuture
                .supplyAsync(() -> restTemplate.getForEntity("/api/v1/resources/health", String.class));
            futures.add(future);
        }
        
        List<ResponseEntity<String>> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        assertThat(results).hasSize(numRequests);
        assertThat(results).allMatch(response -> response.getStatusCode().is2xxSuccessful());
    }
}
```

## Best Practices

### Configuration Management
- Use profiles appropriately for different environments
- Externalize sensitive configuration
- Monitor configuration changes in production
- Use configuration validation where possible

### Security
- Apply principle of least privilege
- Use different security configurations for dev/prod
- Monitor security events and access patterns
- Regular security configuration reviews

### Performance
- Monitor Virtual Thread usage continuously
- Use appropriate executors for different workloads
- Configure timeouts and limits appropriately
- Enable detailed metrics in production

### Operations
- Implement comprehensive health checks
- Use structured logging with correlation IDs
- Monitor application lifecycle events
- Plan for graceful shutdown procedures

This bootstrap module provides a complete, production-ready foundation for the OpenFinance Receptor platform with comprehensive Virtual Thread support, security, monitoring, and operational capabilities.