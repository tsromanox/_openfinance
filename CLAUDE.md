# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a comprehensive Open Finance Brasil microservices system built with modern Java technologies. The system is designed for high performance and scalability, capable of processing millions of financial transactions and account data synchronizations.

### Main Components

- **openfinance-accounts-service**: Account data processing and synchronization service
- **openfinance-consents-service**: Consent management and lifecycle service  
- **openfinance-core**: Shared library with common configurations and utilities
- **openfinance-system-receptor**: Alternative system implementation with Virtual Threads
- **openfinance-platform-receptor**: Modular platform architecture (hexagonal/ports-adapters)
- **infra/**: Docker Compose setup for local development environment

## Architecture and Technology Stack

### Core Technologies
- **Java 21** with preview features and Virtual Threads enabled
- **Spring Boot 3.4.8** with WebFlux for reactive programming
- **Azure Cosmos DB** for primary data storage with multi-region replication
- **Redis** for distributed caching (13-hour TTL for account data)
- **Apache Kafka** for event-driven messaging and async processing
- **PostgreSQL** as alternative database (used in system-receptor)

### Key Architectural Patterns
- **Hexagonal Architecture** (Ports & Adapters) - clear separation of domain, application, and infrastructure layers
- **Event-Driven Architecture** - Kafka-based async communication between services
- **Reactive Programming** - Non-blocking I/O with Mono/Flux throughout the stack
- **Circuit Breaker Pattern** - Resilience4j for fault tolerance
- **CQRS** - Separate read/write models for high-performance scenarios
- **Virtual Threads** - Java 21 structured concurrency for massive parallelism

## Common Development Commands

### Building and Running

```bash
# Build core library (required first)
cd openfinance-core
mvn clean install

# Build and run accounts service
cd openfinance-accounts-service
mvn clean compile
mvn spring-boot:run -Dspring.profiles.active=local

# Build and run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Run all tests
mvn test

# Run tests with coverage (JaCoCo configured)
mvn test jacoco:report

# Generate OpenAPI models (automatically run during compile)
mvn compile
```

### Infrastructure Management

```bash
# Start all infrastructure services (from /infra directory)
make up
# or
docker-compose up -d

# View running services
docker-compose ps

# View logs
make logs
# or 
docker-compose logs -f

# Clean up everything
make clean

# Access service UIs:
# - Kafka UI: http://localhost:8090
# - Grafana: http://localhost:3000 (admin/admin)  
# - Cosmos DB Emulator: https://localhost:8081/_explorer/index.html
# - Prometheus: http://localhost:9090
```

### Database Operations

```bash
# PostgreSQL CLI
make postgres-cli

# Redis CLI  
make redis-cli

# View Kafka topics
make kafka-topics
```

## Key Development Patterns

### Service Implementation Structure
Services follow hexagonal architecture with clear layers:

```
service/
├── domain/
│   ├── entity/          # Domain entities
│   ├── port/           # Input/output ports (interfaces)
│   ├── usecase/        # Business logic implementations  
│   └── exception/      # Domain exceptions
├── application/
│   ├── service/        # Application orchestration
│   ├── scheduler/      # Batch job scheduling
│   ├── dto/           # Data transfer objects
│   └── event/         # Event handlers
└── infrastructure/
    ├── adapter/       # External system adapters
    ├── config/        # Technical configuration
    ├── repository/    # Data access implementations
    └── messaging/     # Kafka producers/consumers
```

### Virtual Threads Usage
All I/O bound operations use virtual threads for maximum concurrency:

```java
@Bean
public TaskExecutor virtualThreadTaskExecutor() {
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
}
```

Location: `VirtualThreadsConfig.java` in accounts service

### Reactive Patterns
Services use reactive streams consistently:

```java
public Mono<Account> findAccount(String accountId) {
    return accountRepository.findById(accountId)
        .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)));
}
```

### Event Publishing
Domain events are published via Kafka using the core library:

```java
@Component
public class EventPublisher {
    public void publishEvent(DomainEvent event) {
        kafkaEventPublisher.publish(event).subscribe();
    }
}
```

## Configuration Profiles

### Local Development (`local` profile)
- Cosmos DB Emulator (port 8081)
- Local Redis (port 6379)
- Local Kafka (port 9092)
- Virtual threads enabled
- Debug logging enabled

### Development (`dev` profile)  
- External Azure resources
- Reduced logging
- Performance monitoring enabled

### Production (`prod` profile)
- Multi-region Cosmos DB
- Redis Cluster
- Kafka cluster
- Enhanced security
- Full observability stack

## Testing Strategy

### Unit Tests
```bash
mvn test -Dtest=AccountServiceTest
```

### Integration Tests with TestContainers
```bash
mvn test -Dtest=*IntegrationTest
```

### Performance Tests with Virtual Threads
```bash
mvn test -Dtest=VirtualThreadPerformanceBenchmark
```

Location: `VirtualThreadPerformanceBenchmark.java` - Tests processing 1000+ accounts concurrently

## OpenAPI Code Generation

The system uses OpenAPI-first development with automatic code generation:

- **Specifications**: Located in `src/main/resources/openapi/`
- **Accounts API**: `accounts-2.4.2.yml`  
- **Consents API**: `consents-3.2.0.yml`
- **Generated Models**: Created during Maven compile phase
- **Configuration**: Spring Boot 3 reactive models with Lombok annotations

Generation occurs automatically during `mvn compile`.

## Data Processing Patterns

### Batch Processing Architecture
The system processes millions of accounts using:

1. **Quartz Scheduler** - Triggers batch jobs every 12 hours
2. **Kafka Topics** - Distributes work across consumer groups
3. **Virtual Thread Pools** - Processes 100-1000 accounts concurrently
4. **Circuit Breakers** - Handles external API failures gracefully
5. **Redis Cache** - 13-hour TTL prevents unnecessary API calls

### Event Flow
```
Scheduler → Kafka → VirtualThread Workers → External APIs → Database → Cache
```

## Performance Characteristics

### Target Metrics (from README)
- **Volume**: 1M clients × 5 accounts = 5M accounts total
- **Throughput**: 10M operations/day (116 ops/s average, 1000 ops/s peak)
- **Latency**: P99 < 500ms
- **Cache Hit Rate**: > 80%
- **Parallel Workers**: 100-1000 virtual threads

### Optimization Features
- **Structured Concurrency** with `StructuredTaskScope`
- **Semaphore Throttling** to prevent API overload
- **Batch Processing** in chunks of 1000 records
- **Connection Pooling** optimized for virtual threads
- **Multi-level Caching** (Redis + local Caffeine)

## Monitoring and Observability

### Metrics Collection
- **Micrometer** with Prometheus export
- **Custom Metrics** via `@Monitored` annotation
- **Virtual Thread Metrics** for concurrency monitoring
- **Business Metrics** for account processing rates

### Health Checks
- Cosmos DB connectivity
- Kafka broker health  
- Redis cluster status
- External API availability

### Tracing
- Correlation IDs for request tracking
- Structured logging with context
- Performance monitoring with AOP

## Security Implementation

### OAuth 2.0 Configuration
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          openfinance:
            authorization-grant-type: client_credentials
            scope: accounts
```

### Features
- Client credentials flow for M2M authentication
- Circuit breaker integration for auth failures
- Token caching and automatic refresh

## Troubleshooting

### Common Issues

**Virtual Thread Pool Exhaustion**
- Check `MAX_VIRTUAL_THREADS` configuration
- Monitor semaphore permits in `VirtualThreadAccountBatchProcessor`
- Review thread dumps via actuator endpoint

**Cosmos DB Connection Issues**  
- Verify `COSMOS_ENDPOINT` and `COSMOS_KEY` environment variables
- Check Cosmos DB health indicator
- Review connection mode settings (DIRECT vs GATEWAY)

**Kafka Consumer Lag**
- Monitor lag via Kafka UI (port 8090)
- Check consumer group health
- Review batch processing timeouts

### Useful Commands
```bash
# Check virtual thread usage
curl localhost:8081/actuator/threaddump

# Health check all components  
curl localhost:8081/actuator/health

# Prometheus metrics
curl localhost:8081/actuator/prometheus | grep virtual_threads
```

## Development Workflow

### New Feature Development
1. Start infrastructure: `make up`
2. Build core library: `cd openfinance-core && mvn clean install`
3. Implement domain logic in appropriate layer
4. Add integration tests with TestContainers
5. Test with local profile: `mvn spring-boot:run -Dspring.profiles.active=local`
6. Run performance tests if applicable

### Code Generation
OpenAPI models are automatically generated during compilation. Manual regeneration:
```bash
mvn clean compile
```

### Testing with Real Dependencies
Use TestContainers for integration testing with real PostgreSQL, Kafka, and Redis instances.

## Important Notes

- **Java 21 Required**: Virtual threads and preview features are essential
- **Build Order**: Always build `openfinance-core` first
- **Profile Management**: Use appropriate profile for your environment
- **Resource Requirements**: Ensure sufficient memory for virtual thread pools
- **Cosmos DB**: Use emulator for local development, real instance for other environments