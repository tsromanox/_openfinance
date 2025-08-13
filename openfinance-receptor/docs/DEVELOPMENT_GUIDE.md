# OpenFinance Receptor - Development Guide

## Quick Start

### Prerequisites

- **Java 21**: Required for Virtual Threads and preview features
- **Maven 3.9+**: For building and dependency management
- **PostgreSQL 16+**: Primary database (Docker available)
- **Docker & Docker Compose**: For local infrastructure
- **IDE Support**: IntelliJ IDEA 2024+ or VSCode with Java extensions

### Environment Setup

#### 1. Java 21 Installation
```bash
# Using SDKMAN (recommended)
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.1-oracle
sdk use java 21.0.1-oracle

# Verify installation
java --version
java -XX:+UnlockExperimentalVMOptions -XX:+EnableVirtualThreads --version
```

#### 2. Clone and Build
```bash
git clone <repository-url>
cd openfinance-receptor

# Build all modules (requires Java 21)
mvn clean install -DskipTests

# Build with tests (slower but comprehensive)
mvn clean install
```

#### 3. Database Setup
```bash
# Using Docker (recommended for development)
docker run --name openfinance-postgres \
  -e POSTGRES_DB=openfinance_receptor_dev \
  -e POSTGRES_USER=openfinance_dev \
  -e POSTGRES_PASSWORD=openfinance_dev \
  -p 5432:5432 -d postgres:16

# Or use docker-compose
docker-compose -f docker/docker-compose.dev.yml up -d postgres
```

#### 4. Run Application
```bash
# Development mode (default)
cd bootstrap
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring.profiles.active=development

# With JVM debugging
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Verification

Access the following endpoints to verify the setup:

- **Health Check**: http://localhost:8080/actuator/health
- **Resource Health**: http://localhost:8080/api/v1/resources/health  
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Metrics**: http://localhost:8080/actuator/prometheus

## Module Development

### Core Domain Development

#### Adding New Domain Entities

1. **Create Entity Class**:
```java
// core-domain/src/main/java/br/com/openfinance/domain/newentity/NewEntity.java
@Builder
@Getter
public class NewEntity {
    private final UUID id;
    private final String name;
    private final LocalDateTime createdAt;
    
    // Business logic methods
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }
    
    public NewEntity withName(String newName) {
        return this.toBuilder().name(newName).build();
    }
}
```

2. **Add Domain Events**:
```java
// core-domain/src/main/java/br/com/openfinance/domain/event/NewEntityCreatedEvent.java
public record NewEntityCreatedEvent(
    UUID eventId,
    UUID entityId,
    String entityName,
    LocalDateTime occurredAt
) implements DomainEvent {
    
    @Override
    public String getEventType() {
        return "NewEntityCreated";
    }
}
```

3. **Create Tests**:
```java
// core-domain/src/test/java/br/com/openfinance/domain/newentity/NewEntityTest.java
class NewEntityTest {
    
    @Test
    void shouldCreateValidEntity() {
        NewEntity entity = NewEntity.builder()
            .id(UUID.randomUUID())
            .name("Test Entity")
            .createdAt(LocalDateTime.now())
            .build();
            
        assertTrue(entity.isValid());
    }
}
```

### Core Application Development

#### Adding New Use Cases

1. **Define Use Case Interface**:
```java
// core-application/src/main/java/br/com/openfinance/application/port/input/NewEntityUseCase.java
public interface NewEntityUseCase {
    NewEntity createEntity(CreateEntityCommand command);
    NewEntity getEntity(UUID entityId);
    void deleteEntity(UUID entityId);
    
    record CreateEntityCommand(String name) {}
}
```

2. **Create Application Service**:
```java
// core-application/src/main/java/br/com/openfinance/application/service/NewEntityService.java
@Service
@Transactional
public class NewEntityService implements NewEntityUseCase {
    
    private final NewEntityRepository repository;
    
    @Override
    public NewEntity createEntity(CreateEntityCommand command) {
        NewEntity entity = NewEntity.builder()
            .id(UUID.randomUUID())
            .name(command.name())
            .createdAt(LocalDateTime.now())
            .build();
            
        return repository.save(entity);
    }
}
```

3. **Define Repository Port**:
```java
// core-application/src/main/java/br/com/openfinance/application/port/output/NewEntityRepository.java
public interface NewEntityRepository {
    NewEntity save(NewEntity entity);
    Optional<NewEntity> findById(UUID id);
    void deleteById(UUID id);
}
```

### Service Module Development

#### Adding High-Performance Services

1. **Create Service Configuration**:
```java
// service-newentity/src/main/java/br/com/openfinance/service/newentity/config/VirtualThreadNewEntityConfig.java
@Configuration
public class VirtualThreadNewEntityConfig {
    
    @Bean("newEntityVirtualThreadExecutor")
    public TaskExecutor newEntityVirtualThreadExecutor() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("newentity-", 0)
            .factory();
        return new TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(virtualThreadFactory));
    }
}
```

2. **Implement Virtual Thread Service**:
```java
// service-newentity/src/main/java/br/com/openfinance/service/newentity/VirtualThreadNewEntityService.java
@Service
public class VirtualThreadNewEntityService {
    
    private final TaskExecutor newEntityExecutor;
    
    public CompletableFuture<List<NewEntity>> processEntitiesAsync(List<String> entityIds) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                List<StructuredTaskScope.Subtask<NewEntity>> subtasks = 
                    entityIds.stream()
                        .map(id -> scope.fork(() -> processEntity(id)))
                        .toList();
                        
                scope.join();
                scope.throwIfFailed();
                
                return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }
        }, newEntityExecutor);
    }
}
```

### Bootstrap Integration

#### Adding New Components

1. **Create Configuration Class**:
```java
// bootstrap/src/main/java/br/com/openfinance/config/NewEntityConfiguration.java
@Configuration
@EnableConfigurationProperties
public class NewEntityConfiguration {
    
    @Bean
    public NewEntityService newEntityService(NewEntityRepository repository) {
        return new NewEntityService(repository);
    }
    
    @Bean
    public NewEntityRepository newEntityRepository() {
        return new InMemoryNewEntityRepository();
    }
}
```

2. **Add REST Controller**:
```java
// bootstrap/src/main/java/br/com/openfinance/controller/NewEntityController.java
@RestController
@RequestMapping("/api/v1/entities")
public class NewEntityController {
    
    private final NewEntityUseCase newEntityUseCase;
    
    @PostMapping
    public ResponseEntity<NewEntity> createEntity(@RequestBody CreateEntityRequest request) {
        var command = new CreateEntityCommand(request.name());
        var entity = newEntityUseCase.createEntity(command);
        return ResponseEntity.ok(entity);
    }
}
```

## Testing Strategies

### Unit Testing

#### Domain Entity Tests
```java
@ExtendWith(MockitoExtension.class)
class AccountTest {
    
    @Test
    void shouldIndicateNeedsSyncWhenLastSyncOld() {
        Account account = Account.builder()
            .accountId("12345")
            .lastSyncAt(LocalDateTime.now().minusHours(2))
            .build();
            
        assertTrue(account.needsSync());
    }
}
```

#### Application Service Tests
```java
@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {
    
    @Mock
    private ConsentRepository consentRepository;
    
    @InjectMocks
    private ConsentService consentService;
    
    @Test
    void shouldCreateConsentSuccessfully() {
        // Given
        CreateConsentCommand command = new CreateConsentCommand(
            "org123", "customer456", Set.of("ACCOUNTS_READ"), null);
            
        when(consentRepository.save(any())).thenReturn(mockConsent);
        
        // When
        Consent result = consentService.createConsent(command);
        
        // Then
        assertThat(result).isNotNull();
        verify(consentRepository).save(any(Consent.class));
    }
}
```

### Integration Testing

#### With TestContainers
```java
@SpringBootTest
@Testcontainers
class ResourceServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")  
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private ResourceService resourceService;
    
    @Test
    void shouldDiscoverAndSaveResources() {
        List<String> endpoints = List.of("http://test-endpoint.com");
        
        List<Resource> resources = resourceService.discoverResources(endpoints).join();
        
        assertThat(resources).isNotEmpty();
    }
}
```

### Performance Testing

#### Virtual Thread Performance Tests
```java
@Test
@Timeout(30)
void shouldProcessLargeVolumeWithVirtualThreads() {
    List<String> resourceIds = IntStream.range(0, 10000)
        .mapToObj(i -> "resource-" + i)
        .toList();
        
    long startTime = System.currentTimeMillis();
    
    List<Resource> results = resourceProcessor
        .processMassiveResourceWorkload(resourceIds, 1000)
        .join();
        
    long duration = System.currentTimeMillis() - startTime;
    double throughput = results.size() * 1000.0 / duration;
    
    assertThat(results).hasSize(10000);
    assertThat(throughput).isGreaterThan(500); // > 500 ops/second
    assertThat(duration).isLessThan(20000); // < 20 seconds
}
```

#### Load Testing with JMeter
```xml
<!-- src/test/jmeter/resource-discovery-load-test.jmx -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="Resource Discovery Load Test">
      <ThreadGroup testname="Resource Discovery Threads">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">60</stringProp>
        
        <HTTPSamplerProxy testname="Discover Resources">
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/resources/discover</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
        </HTTPSamplerProxy>
      </ThreadGroup>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

## Configuration Management

### Environment-Specific Configuration

#### Development (application-development.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/openfinance_receptor_dev
    username: openfinance_dev
    password: openfinance_dev
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    br.com.openfinance: DEBUG
    org.hibernate.SQL: DEBUG

openfinance:
  resources:
    discovery:
      automatic:
        interval: 300s  # Faster for development
```

#### Production (application-production.yml)
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 100
      minimum-idle: 20

  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    br.com.openfinance: INFO
    org.springframework: WARN
    
openfinance:
  resources:
    discovery:
      automatic:
        interval: 7200s  # Standard for production
```

### Configuration Properties

#### Custom Configuration Classes
```java
@ConfigurationProperties(prefix = "openfinance.resources")
@Validated
public record ResourceConfigurationProperties(
    @NotNull Boolean enabled,
    @NotNull VirtualThreadsConfig virtualThreads,
    @NotNull BatchConfig batch,
    @NotNull AdaptiveConfig adaptive
) {
    
    public record VirtualThreadsConfig(
        @NotNull Boolean enabled,
        @Min(100) Integer maxPoolSize,
        @NotNull Duration connectionTimeout
    ) {}
    
    public record BatchConfig(
        @Min(10) @Max(10000) Integer size,
        @Min(1) Integer maxConcurrent,
        @Min(1) Integer parallelFactor
    ) {}
}
```

## Debugging and Troubleshooting

### Common Issues

#### Virtual Threads Not Working
```bash
# Check Java version supports Virtual Threads
java -XX:+UnlockExperimentalVMOptions -XX:+EnableVirtualThreads --version

# Enable Virtual Thread debugging
export JVM_OPTS="--enable-preview -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput"
mvn spring-boot:run
```

#### Database Connection Issues
```bash
# Check PostgreSQL connection
psql -h localhost -U openfinance_dev -d openfinance_receptor_dev

# View connection pool status
curl localhost:8080/actuator/metrics/hikaricp.connections.active
```

#### Performance Issues
```bash
# Monitor Virtual Thread usage
curl localhost:8080/actuator/threaddump | grep -i virtual

# Check resource utilization
curl localhost:8080/api/v1/resources/utilization

# Monitor performance metrics
curl localhost:8080/actuator/prometheus | grep openfinance_resources
```

### Debugging Tools

#### Thread Dump Analysis
```bash
# Generate thread dump
curl localhost:8080/actuator/threaddump > threaddump.json

# Analyze Virtual Thread usage
jstack <pid> | grep -A 5 -B 5 "VirtualThread"
```

#### Memory Analysis
```bash
# Monitor memory usage
curl localhost:8080/actuator/metrics/jvm.memory.used

# Generate heap dump for analysis
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
jmap -dump:format=b,file=heapdump.hprof <pid>
```

#### Performance Profiling
```bash
# Enable JFR profiling
java -XX:+FlightRecorder \
     -XX:StartFlightRecording=duration=60s,filename=profile.jfr \
     -jar target/openfinance-receptor.jar

# Analyze with JFR
jfr summary profile.jfr
jfr print --events jdk.VirtualThreadStart,jdk.VirtualThreadEnd profile.jfr
```

## IDE Setup

### IntelliJ IDEA Configuration

#### Project Settings
1. **File → Project Structure**
   - Project SDK: Java 21
   - Project language level: 21 - Preview features
   - Module language level: Same as project

2. **Settings → Build → Compiler → Java Compiler**
   - Project bytecode version: 21
   - Additional command line parameters: `--enable-preview`

3. **Run Configuration**
   - VM Options: `--enable-preview -XX:+EnableVirtualThreads`
   - Environment variables: `SPRING_PROFILES_ACTIVE=development`

#### Useful Plugins
- **Spring Boot Assistant**: Enhanced Spring Boot support
- **Database Navigator**: PostgreSQL integration
- **JPA Buddy**: JPA entity management
- **Docker**: Container management

### VSCode Configuration

#### .vscode/settings.json
```json
{
  "java.compile.nullAnalysis.mode": "automatic",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java21"
    }
  ],
  "java.compile.commandLine.preview.enabled": true,
  "spring-boot.ls.problem.application-properties.enabled": true,
  "java.debug.settings.vmArgs": "--enable-preview -XX:+EnableVirtualThreads"
}
```

#### .vscode/launch.json
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "OpenFinance Receptor",
      "request": "launch",
      "mainClass": "br.com.openfinance.OpenFinanceReceptorApplication",
      "projectName": "bootstrap",
      "vmArgs": "--enable-preview -XX:+EnableVirtualThreads",
      "env": {
        "SPRING_PROFILES_ACTIVE": "development"
      }
    }
  ]
}
```

## Best Practices

### Code Organization
- Follow hexagonal architecture principles
- Keep domain logic in domain entities
- Use ports and adapters for external integration
- Maintain clear module boundaries

### Virtual Thread Usage
- Use Virtual Threads for I/O-bound operations
- Avoid blocking operations in Virtual Thread pools
- Monitor Virtual Thread usage with metrics
- Use Structured Concurrency for coordinated operations

### Testing
- Write comprehensive unit tests for domain logic
- Use integration tests for cross-module functionality
- Include performance tests for critical paths
- Use TestContainers for realistic integration testing

### Performance
- Monitor application metrics continuously
- Use adaptive resource management
- Profile applications under load
- Optimize database queries and connection pooling

### Security
- Follow security best practices
- Use appropriate authentication and authorization
- Regular security dependency updates
- Monitor for security vulnerabilities

This development guide provides the foundation for effective development on the OpenFinance Receptor platform. Follow these practices to ensure high-quality, performant, and maintainable code.