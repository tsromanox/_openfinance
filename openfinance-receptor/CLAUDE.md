# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is the **openfinance-receptor** - a high-performance, modular Open Finance Brasil data receptor platform built using hexagonal (ports and adapters) architecture with cutting-edge Java 21 Virtual Threads and Structured Concurrency optimizations. The system implements advanced receptor functionality for consuming Open Finance Brasil APIs to synchronize financial data from participating institutions with maximum performance and minimal resource consumption.

### Architecture

The project follows **hexagonal architecture** with clear separation of concerns and **Java 21 concurrency optimizations**:

- **Core Domain**: Enhanced business entities and rules with validation (`core/domain/`)
- **Application Layer**: Use cases and ports with comprehensive DTOs (`core/application/`)  
- **Infrastructure**: High-performance adapters with Virtual Threads (`infrastructure/`)
- **Modules**: Feature-specific implementations with Java 21 optimizations (`modules/`)
- **Bootstrap**: Application entry point and assembly (`bootstrap/`)

## Technology Stack

### Core Technologies
- **Java 21** with preview features and **Virtual Threads** enabled (`--enable-preview`)
- **Spring Boot 3.4.8** with **WebFlux** for reactive programming
- **PostgreSQL** with **R2DBC** and **JPA/Hibernate** dual access strategy
- **Flyway** for database migrations and schema management
- **OpenAPI 3.0** code generation for OpenFinance Brasil client models
- **MapStruct** for high-performance object mapping
- **Lombok** for reducing boilerplate code

### Performance & Concurrency
- **Virtual Threads** for massive I/O-bound concurrency (up to 10,000+ threads)
- **Structured Concurrency** for coordinated task execution and error handling
- **Project Reactor** with **WebFlux** for reactive streams processing
- **Resilience4j** for circuit breaker patterns and fault tolerance
- **HikariCP** with optimized connection pooling for database access
- **Caffeine** for high-performance in-memory caching

### Monitoring & Observability
- **Micrometer** with **Prometheus** integration for comprehensive metrics
- **Spring Boot Actuator** for health checks and operational endpoints
- **Adaptive Resource Management** with real-time performance feedback
- **Custom Performance Monitors** for domain-specific metrics collection

## Common Development Commands

### Building the Project

```bash
# Build all modules (from root)
mvn clean install

# Build specific module
cd infrastructure/client
mvn clean compile

# Build with tests
mvn clean test

# Skip tests during build
mvn clean install -DskipTests
```

### OpenAPI Code Generation

OpenAPI models are automatically generated during compilation. The infrastructure-client module uses:
- **Input spec**: `infrastructure/client/src/main/resources/openapi/consents-3.2.0.yml`
- **Generated packages**: `br.com.openfinance.client.api` and `br.com.openfinance.client.model`

```bash
# Regenerate OpenAPI models manually
cd infrastructure/client
mvn clean compile
```

### Database Operations

The system uses Flyway for database migrations:

```bash
# Run migrations (automatically executed on startup)
mvn flyway:migrate

# Create new migration
# Add SQL files to: infrastructure/persistence/src/main/resources/sql/
# Format: V{version}__{description}.sql
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
cd core/application
mvn test

# Run with TestContainers (integration tests)
mvn test -Dspring.profiles.active=test
```

## Module Structure

### Core Modules

**core/domain**: Enhanced domain entities with business logic and validation
- `domain/consent/`: Consent, ConsentStatus, Permission with business rules
- `domain/account/`: Account, Balance with value objects (AccountNumber, AgencyNumber)
- `domain/processing/`: ProcessingJob, JobStatus with lifecycle management
- `domain/events/`: Domain events (ConsentCreatedEvent, ConsentStatusChangedEvent, AccountSyncedEvent)
- `domain/exception/`: Domain-specific exceptions with proper error handling
- `domain/factory/`: ConsentFactory for controlled object creation

**core/application**: Application services and comprehensive DTOs
- `port/input/`: Use case interfaces (AccountUseCase, ConsentUseCase)
- `port/output/`: Repository and external service interfaces with enhanced contracts
- `service/`: Business logic implementations with comprehensive error handling
- `dto/`: Data transfer objects (ConsentRequest, ConsentResponse, AccountsResponse, BalanceResponse)
- `exception/`: Application-specific exceptions with detailed error information

### Infrastructure Modules (Java 21 Optimized)

**infrastructure/persistence**: Dual-strategy data access with Virtual Threads
- **JPA/Hibernate entities** with performance annotations and optimized indexing
- **R2DBC repositories** for reactive database access
- **Virtual Thread repositories** using Structured Concurrency for batch operations
- **HikariCP configuration** optimized for Virtual Thread workloads
- **Flyway migrations** with performance-optimized schema design
- **Database performance monitoring** with custom metrics

**infrastructure/client**: High-performance OpenFinance Brasil API client
- **Auto-generated models** from OpenAPI 3.0 specifications
- **Reactive WebClient** with optimized connection pooling
- **Virtual Thread OpenFinanceClient** for massive parallel API calls
- **OAuth2 service** with token caching and parallel authentication
- **Circuit breaker patterns** with Resilience4j integration
- **Parallel batch processors** for high-throughput operations
- **Performance monitoring** with detailed API call metrics

**infrastructure/scheduler**: Advanced processing coordination with Virtual Threads
- **Virtual Thread processing service** using Structured Concurrency
- **Adaptive resource manager** with dynamic parameter adjustment
- **Performance monitor** with comprehensive metrics collection
- **Background job processing** optimized for Virtual Thread scalability
- **Scheduling controllers** with reactive endpoints and health checks

### Service Modules (Java 21 Optimized)

**modules/consents**: Virtual Thread optimized consent processing
- **VirtualThreadConsentService** extending ConsentUseCase with parallel capabilities
- **VirtualThreadConsentProcessor** using Structured Concurrency for batch operations
- **ParallelConsentValidator** with concurrent validation using Virtual Threads
- **AdaptiveConsentResourceManager** for dynamic resource optimization
- **ConsentPerformanceMonitor** with consent-specific metrics
- **VirtualThreadConsentController** with comprehensive REST API and reactive endpoints
- **Integration tests** demonstrating Virtual Thread performance and scalability

**modules/accounts**: Virtual Thread optimized account processing
- **VirtualThreadAccountService** extending AccountUseCase with advanced parallel processing
- **VirtualThreadAccountProcessor** using Structured Concurrency for coordinated execution
- **AdaptiveAccountResourceManager** for intelligent resource allocation
- **AccountPerformanceMonitor** with detailed account processing metrics
- **VirtualThreadAccountController** with complete REST API and server-sent events
- **Reactive processing** using WebFlux patterns with Virtual Thread integration
- **Integration tests** demonstrating massive account workload processing

### Module Dependencies

Enhanced dependency rules with performance considerations:
- **Domain** → No dependencies (pure business logic)
- **Application** → Domain only (use case orchestration)
- **Infrastructure** → Domain + Application (technical implementations with Virtual Threads)
- **Service Modules** → Core + Infrastructure (feature-specific optimizations)
- **Bootstrap** → All modules (application assembly)

## Key Development Patterns

### Enhanced Domain Entity Pattern
Domain entities use immutable builder pattern with business logic and validation:

```java
var consent = Consent.builder()
    .organizationId("org123")
    .customerId("customer456")
    .permissions(Set.of(Permission.ACCOUNTS_READ, Permission.ACCOUNTS_BALANCES_READ))
    .status(ConsentStatus.AWAITING_AUTHORISATION)
    .expirationDateTime(LocalDateTime.now().plusMonths(6))
    .build();

// Enhanced with business methods
if (consent.canAccessAccountData()) {
    consent.authorize();  // Business logic encapsulated
}
```

### Virtual Thread Port-Adapter Pattern
Application layer defines ports, infrastructure provides Virtual Thread optimized adapters:

```java
// Enhanced Port (in application)
public interface ConsentRepository {
    Consent save(Consent consent);
    Optional<Consent> findById(UUID id);
    List<Consent> findByStatus(ConsentStatus status);
    List<Consent> findByOrganizationId(String organizationId);
}

// Virtual Thread Optimized Adapter (in infrastructure/persistence)
@Repository
public class VirtualThreadConsentRepository implements ConsentRepository {
    private final TaskExecutor virtualThreadExecutor;
    private final JpaConsentRepository jpaRepository;
    private final ReactiveConsentRepository reactiveRepository;
    
    // Dual-strategy implementation with Virtual Threads
    public CompletableFuture<List<Consent>> findByStatusAsync(ConsentStatus status) {
        return CompletableFuture.supplyAsync(() -> 
            jpaRepository.findByStatus(status), virtualThreadExecutor);
    }
}
```

### Virtual Thread Service Structure
Services use Structured Concurrency for coordinated operations:

```java
public class VirtualThreadConsentService implements ConsentUseCase {
    private final ConsentRepository consentRepository;
    private final OpenFinanceClient openFinanceClient;
    private final VirtualThreadConsentProcessor processor;
    private final AdaptiveConsentResourceManager resourceManager;
    private final ConsentPerformanceMonitor performanceMonitor;
    
    public CompletableFuture<List<Consent>> createConsentsAsync(List<CreateConsentCommand> commands) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                // Process consents with Structured Concurrency
                var tasks = commands.stream()
                    .map(cmd -> scope.fork(() -> createSingleConsent(cmd)))
                    .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                return tasks.stream()
                    .map(this::getResult)
                    .toList();
            }
        }, virtualThreadExecutor);
    }
}
```

### Adaptive Resource Management Pattern
Dynamic resource allocation based on system performance:

```java
@Component
public class AdaptiveConsentResourceManager {
    private final AtomicInteger dynamicBatchSize = new AtomicInteger(200);
    private final AtomicInteger dynamicConcurrencyLevel = new AtomicInteger(100);
    private final Semaphore consentProcessingSemaphore;
    
    @Scheduled(fixedDelay = 30000)
    public void adaptResourceLimits() {
        var performanceReport = performanceMonitor.getPerformanceReport();
        
        // Increase batch size if system has capacity and efficiency is good
        if (cpuUsage < 0.4 && memoryUsage < 0.5 && 
            performanceReport.processingEfficiency() > 0.85) {
            dynamicBatchSize.updateAndGet(size -> Math.min(size + 50, 1000));
        }
        
        // Adaptive concurrency adjustment
        updateSemaphoreCapacity(consentProcessingSemaphore, dynamicConcurrencyLevel.get());
    }
}
```

### Performance Monitoring Pattern
Comprehensive metrics collection with domain-specific insights:

```java
@Component
public class ConsentPerformanceMonitor {
    private final MeterRegistry meterRegistry;
    private final Timer consentProcessingTimer;
    private final Counter consentsCreatedCounter;
    
    public void recordConsentOperation(String operationType, boolean success, long durationMs) {
        consentsCreatedCounter.increment();
        consentProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        if (!success) {
            recordError("consent_operation_failure", operationType, true);
        }
    }
    
    public ConsentPerformanceRecommendations getRecommendations() {
        double efficiency = getProcessingEfficiency();
        double throughput = getCurrentThroughput();
        
        return new ConsentPerformanceRecommendations(
            calculateRecommendedBatchSize(efficiency, throughput),
            calculateRecommendedConcurrency(efficiency, throughput),
            efficiency,
            throughput,
            generateOptimizationSuggestions(efficiency, throughput)
        );
    }
}
```

## Code Generation and Compilation

### Maven Compiler Configuration
All modules use Java 21 with preview features:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Annotation Processing
MapStruct and Lombok are configured for all infrastructure modules:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>1.6.3</version>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
</annotationProcessorPaths>
```

## Database Schema

Key tables defined in `V1__create_base_tables.sql`:

- **processing_jobs**: Job queue with status tracking and retry logic
- **consents**: Consent lifecycle management
- **accounts**: Account data synchronized from Open Finance APIs

Optimized indexes for:
- Pending job queries: `idx_pending_jobs ON processing_jobs(created_at) WHERE status = 'PENDING'`
- Organization lookups: `idx_consents_org ON consents(organization_id)`

## Performance Characteristics & Optimization

### Target Performance Metrics
- **Volume**: Support for millions of accounts and thousands of consents
- **Throughput**: 1000+ operations/second with Virtual Thread optimization
- **Latency**: P99 < 500ms for most operations, P95 < 200ms
- **Concurrency**: 10,000+ Virtual Threads for I/O-bound operations
- **Resource Efficiency**: < 85% CPU, < 85% memory under high load

### Java 21 Concurrency Features
- **Virtual Threads**: Massive concurrency for I/O operations (up to 10,000+ threads)
- **Structured Concurrency**: Coordinated task execution with proper error handling
- **Adaptive Resource Management**: Dynamic parameter adjustment based on performance
- **Semaphore-based Throttling**: Intelligent resource limiting per operation type

### Configuration Profiles

#### Development Profile (`development`)
```yaml
openfinance:
  consents:
    batch.size: 50
    batch.max-concurrent: 20
  accounts:
    batch.size: 100
    batch.max-concurrent: 30
```

#### Production Profile (`production`)
```yaml
openfinance:
  consents:
    batch.size: 500
    batch.max-concurrent: 200
  accounts:
    batch.size: 800
    batch.max-concurrent: 400
```

#### Performance Testing Profile (`performance`)
```yaml
openfinance:
  consents:
    virtual-threads.max-pool-size: 5000
    batch.size: 1000
    batch.max-concurrent: 500
  accounts:
    virtual-threads.max-pool-size: 10000
    batch.size: 2000
    batch.max-concurrent: 1000
```

## Development Workflow

### New Feature Development with Virtual Threads
1. **Define enhanced domain entities** in `core/domain` with business logic
2. **Create comprehensive use case interfaces** in `core/application/port/input`
3. **Implement business logic** in `core/application/service` with proper DTOs
4. **Add Virtual Thread optimized adapters** in appropriate `infrastructure/` modules
5. **Create service module** in `modules/` with Virtual Thread processors
6. **Add performance monitoring** and adaptive resource management
7. **Wire everything together** in `bootstrap` module

### Adding Virtual Thread Optimized Processing
1. **Create VirtualThread{Feature}Config** with specialized executors
2. **Implement VirtualThread{Feature}Processor** using Structured Concurrency
3. **Add Adaptive{Feature}ResourceManager** for dynamic resource allocation
4. **Create {Feature}PerformanceMonitor** for metrics collection
5. **Implement VirtualThread{Feature}Service** extending existing use cases
6. **Add VirtualThread{Feature}Controller** with comprehensive REST API
7. **Create integration tests** demonstrating performance characteristics

### Adding New OpenAPI Integration with Performance Optimization
1. Place OpenAPI spec in `infrastructure/client/src/main/resources/openapi/`
2. Configure generation in `infrastructure/client/pom.xml`
3. **Implement Virtual Thread optimized adapter** with parallel processing
4. **Add circuit breaker and retry patterns** with Resilience4j
5. **Create reactive and blocking API variants** for different use cases
6. Create port interface in `core/application/port/output`

### Database Changes with Performance Considerations
1. Create new migration file in `infrastructure/persistence/src/main/resources/sql/`
2. Follow naming: `V{version}__{description}.sql`
3. **Add performance-optimized indexes** for Virtual Thread workloads
4. **Update JPA entities** with performance annotations
5. **Create R2DBC reactive repositories** for high-throughput scenarios
6. **Implement Virtual Thread repository wrappers** for batch operations

## Testing Strategy

### Comprehensive Testing Approach
- **Unit Tests**: Test domain logic and services in isolation with business rule validation
- **Integration Tests**: Use TestContainers with Virtual Thread performance validation
- **Performance Tests**: Virtual Thread scalability tests with various concurrency levels
- **Load Tests**: Massive workload processing with resource utilization monitoring
- **Architecture Tests**: Verify module dependencies and hexagonal constraints
- **Reactive Tests**: Validate WebFlux integration with Virtual Thread coordination

### Virtual Thread Testing Patterns
```java
@Test
void shouldDemonstrateVirtualThreadScalability() throws InterruptedException {
    int[] threadCounts = {100, 500, 1000, 2000, 5000};
    
    for (int threadCount : threadCounts) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        IntStream.range(0, threadCount)
            .forEach(i -> executor.submit(() -> {
                // Simulate I/O-bound work
                simulateOperation();
                latch.countDown();
            }));
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.printf("Virtual Threads: %d completed in %d ms (%.2f ops/sec)%n",
            threadCount, duration, threadCount * 1000.0 / duration);
        
        executor.shutdown();
    }
}
```

## Build Dependencies

Enhanced build order with performance considerations:
1. `core/domain` (pure business logic, no dependencies)
2. `core/application` (use case orchestration, depends on domain)
3. `infrastructure/*` modules (Virtual Thread implementations, depend on core)
4. `modules/*` (feature-specific optimizations, depend on infrastructure)
5. `bootstrap` (application assembly with all optimizations)

Maven handles this automatically via the parent POM module declaration.

## Important Configuration

### Essential Requirements
- **Java Version**: 21 with preview features and Virtual Threads enabled
- **JVM Args**: `--enable-preview` for Structured Concurrency
- **Database**: PostgreSQL with optimized connection pooling (HikariCP)
- **Memory**: Minimum 2GB heap for Virtual Thread workloads
- **CPU**: Multi-core recommended for maximum Virtual Thread benefit

### Performance Configuration
- **Virtual Thread Pool Sizes**: Configured per service module (consents: 5000, accounts: 10000)
- **Adaptive Thresholds**: CPU < 80%, Memory < 85% for optimal performance
- **Circuit Breakers**: Resilience4j with service-specific failure thresholds
- **Monitoring**: Micrometer with Prometheus for comprehensive metrics
- **OpenAPI Generation**: Triggered during `mvn compile` with reactive model support

## Available APIs and Endpoints

### Consent Processing APIs

#### Core Consent Operations
- `POST /api/v1/consents` - Create single consent
- `POST /api/v1/consents/batch` - Create multiple consents asynchronously
- `PUT /api/v1/consents/{id}/authorize` - Authorize consent
- `DELETE /api/v1/consents/{id}` - Revoke consent
- `GET /api/v1/consents/{id}` - Get consent details

#### Advanced Processing
- `POST /api/v1/consents/process/adaptive` - Process with adaptive batch sizing
- `POST /api/v1/consents/process/massive` - Massive workload processing
- `GET /api/v1/consents/process/reactive` - Server-sent events for reactive processing
- `POST /api/v1/consents/validate/batch` - Parallel validation

#### Monitoring & Metrics
- `GET /api/v1/consents/metrics/performance` - Performance metrics and recommendations
- `GET /api/v1/consents/metrics/resources` - Resource utilization
- `GET /api/v1/consents/health` - Health check with system status

### Account Processing APIs

#### Core Account Operations
- `POST /api/v1/accounts/sync/{consentId}` - Sync accounts for single consent
- `POST /api/v1/accounts/sync/batch` - Sync accounts for multiple consents
- `GET /api/v1/accounts/{accountId}` - Get account details
- `POST /api/v1/accounts/{accountId}/balance` - Update single account balance
- `POST /api/v1/accounts/balances/update` - Update multiple balances in parallel

#### Advanced Processing
- `POST /api/v1/accounts/process/adaptive` - Adaptive batch processing
- `POST /api/v1/accounts/process/massive` - Massive parallel processing
- `GET /api/v1/accounts/process/reactive` - Reactive processing with SSE

#### Monitoring & Metrics
- `GET /api/v1/accounts/metrics/performance` - Account processing metrics
- `GET /api/v1/accounts/metrics/resources` - Resource utilization
- `GET /api/v1/accounts/health` - Health check

### Infrastructure Monitoring APIs

#### Scheduler Management
- `GET /api/v1/scheduler/status` - Scheduler status and statistics
- `GET /api/v1/scheduler/jobs` - Active and pending jobs
- `POST /api/v1/scheduler/jobs/{id}/retry` - Retry failed job
- `GET /api/v1/scheduler/performance` - Scheduler performance metrics
- `GET /api/v1/scheduler/resources` - Resource utilization

#### System Health
- `GET /actuator/health` - Overall system health
- `GET /actuator/metrics` - All system metrics
- `GET /actuator/prometheus` - Prometheus metrics endpoint
- `GET /actuator/threaddump` - Virtual Thread dump for debugging
- `GET /actuator/info` - Application information

## Monitoring and Observability

### Prometheus Metrics

#### Consent Processing Metrics
```
openfinance_consents_processed_total - Total consents processed
openfinance_consents_created_total - Total consents created
openfinance_consents_authorized_total - Total consents authorized
openfinance_consents_revoked_total - Total consents revoked
openfinance_consents_processing_duration - Processing time distribution
openfinance_consents_virtual_threads_active - Active Virtual Threads
openfinance_consents_batch_processing_duration - Batch processing time
openfinance_consents_errors_total - Error count by type
```

#### Account Processing Metrics
```
openfinance_accounts_processed_total - Total accounts processed
openfinance_accounts_synced_total - Total accounts synchronized
openfinance_accounts_balances_updated_total - Total balance updates
openfinance_accounts_sync_duration - Account sync time distribution
openfinance_accounts_balance_update_duration - Balance update time
openfinance_accounts_virtual_threads_active - Active Virtual Threads
openfinance_accounts_api_calls_total - Total API calls made
```

#### System Resource Metrics
```
openfinance_virtual_threads_active - Total active Virtual Threads
openfinance_cpu_usage - Current CPU utilization
openfinance_memory_usage - Current memory utilization
openfinance_adaptive_batch_size - Current adaptive batch size
openfinance_adaptive_concurrency_level - Current concurrency level
```

### Performance Dashboards

The system provides comprehensive Grafana dashboards for monitoring:

#### Consent Processing Dashboard
- Consent creation and processing rates
- Virtual Thread utilization for consent operations
- Batch processing efficiency and throughput
- Error rates and failure analysis
- Adaptive resource management effectiveness

#### Account Processing Dashboard
- Account synchronization rates and success metrics
- Balance update performance and accuracy
- API call performance and circuit breaker status
- Massive workload processing capabilities
- Resource utilization trends

#### System Performance Dashboard
- Overall Virtual Thread utilization across all services
- Adaptive resource management decisions and trends
- Circuit breaker status and failure rates
- Database connection pool utilization
- JVM performance metrics optimized for Virtual Threads

### Logging and Troubleshooting

#### Structured Logging
All logs include contextual information:
```
[correlation-id] [consent-id] [account-id] [organization-id]
```

#### Virtual Thread Debugging
- Enable detailed Virtual Thread logging with `java.util.concurrent: DEBUG`
- Thread dump analysis via `/actuator/threaddump`
- Performance bottleneck identification through metrics correlation
- Adaptive resource management decision logging

## Best Practices

### Virtual Thread Optimization
1. **Use Virtual Threads for I/O-bound operations** - Database calls, API calls, file operations
2. **Avoid Virtual Threads for CPU-intensive tasks** - Use platform thread pools instead
3. **Implement proper resource limits** - Use semaphores and adaptive management
4. **Monitor Virtual Thread creation** - Avoid excessive thread creation patterns

### Structured Concurrency Best Practices
1. **Use try-with-resources** for StructuredTaskScope management
2. **Handle interruptions properly** - Implement proper cleanup in catch blocks
3. **Coordinate error handling** - Use ShutdownOnFailure for coordinated shutdown
4. **Implement timeouts** - Always set reasonable timeouts for scope operations

### Performance Optimization
1. **Enable adaptive resource management** - Let the system optimize based on performance
2. **Monitor and tune batch sizes** - Use performance recommendations
3. **Implement circuit breakers** - Protect against cascade failures
4. **Use reactive patterns** - Combine Virtual Threads with reactive streams when appropriate

### Troubleshooting Guide
1. **High CPU usage** - Check for CPU-intensive operations in Virtual Threads
2. **Memory pressure** - Review Virtual Thread creation patterns and resource limits
3. **Poor throughput** - Analyze batch sizes and concurrency levels
4. **Circuit breaker trips** - Review error rates and external service health
5. **Slow processing** - Check database connection pool settings and query performance