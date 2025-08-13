# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This repository contains the **openfinance-system-receptor** - a high-performance Open Finance Brasil microservices system built with cutting-edge Java 21 Virtual Threads technology. The system is designed for massive scale processing of financial accounts, consents, and transactions.

### Main Components

- **openfinance-accounts-service**: Account data processing service with Virtual Threads support
- **openfinance-consents-service**: Consent management and lifecycle service  
- **openfinance-core-module**: Shared library with common configurations, security, and utilities

## Architecture and Technology Stack

### Core Technologies
- **Java 21** with preview features and **Virtual Threads** enabled for massive concurrency
- **Spring Boot 3.5.4** with enhanced Virtual Thread support
- **PostgreSQL** as primary database with optimized connection pooling
- **Redis** for distributed caching with intelligent TTL
- **Apache Kafka** for event-driven messaging and async processing
- **Docker** for containerization with Virtual Thread optimizations

### Key Architectural Patterns
- **Hexagonal Architecture** (Ports & Adapters) - clear separation of domain, application, and infrastructure layers
- **Virtual Thread Concurrency** - Java 21 structured concurrency for processing 10M+ operations
- **Event-Driven Architecture** - Kafka-based async communication between services
- **Circuit Breaker Pattern** - Resilience4j for fault tolerance
- **Reactive Programming** - Non-blocking I/O with Mono/Flux throughout the stack

## Common Development Commands

### Building and Running

```bash
# Build core module first (required dependency)
cd openfinance-core-module
mvn clean install

# Build and run accounts service
cd openfinance-accounts-service
mvn clean compile
mvn spring-boot:run -Dspring.profiles.active=local

# Enable Virtual Threads with preview features
mvn spring-boot:run -Dspring.profiles.active=local -Djava.tool.options="--enable-preview"

# Run all tests
mvn test

# Run Virtual Thread performance benchmarks
mvn test -Dtest=VirtualThreadPerformanceBenchmark

# Generate OpenAPI models (automatically run during compile)
mvn compile
```

### Virtual Thread Infrastructure Management

```bash
# Start infrastructure with Virtual Thread optimizations (from accounts-service/infra)
docker-compose -f docker-compose-vthreads.yml up -d

# View Virtual Thread metrics
curl localhost:8081/actuator/metrics/jvm.threads.virtual

# Check Virtual Thread usage
curl localhost:8081/actuator/threaddump

# Monitor batch processing performance
curl localhost:8081/actuator/prometheus | grep virtual_threads
```

### Testing Commands

```bash
# Unit tests
mvn test

# Integration tests with TestContainers
mvn test -Dtest=*IntegrationTest

# Virtual Thread batch processing tests
mvn test -Dtest=VirtualThreadAccountBatchProcessorTest

# Performance benchmarks
mvn test -Dtest=VirtualThreadPerformanceBenchmark
```

## Key Development Patterns

### Virtual Thread Service Implementation
All I/O bound operations use Virtual Threads for maximum concurrency:

```java
// Located in VirtualThreadsConfig.java
@Bean
public TaskExecutor virtualThreadTaskExecutor() {
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
}
```

### High-Performance Batch Processing
The system processes millions of accounts using Virtual Threads:

```java
// VirtualThreadAccountBatchProcessor.java - processes 10M+ accounts
public void processAccountUpdatesWithVirtualThreads() {
    try (var scope = StructuredTaskScope.ShutdownOnFailure()) {
        // Process 10,000+ concurrent virtual threads
        // Location: src/main/java/br/com/openfinance/accounts/application/services/
    }
}
```

### Service Structure (Hexagonal Architecture)
```
service/
├── domain/
│   ├── model/          # Domain entities (Account, Transaction, etc.)
│   ├── ports/         # Input/output interfaces
│   └── services/      # Domain business logic
├── application/
│   ├── services/      # Virtual Thread batch processors
│   ├── dto/          # Data transfer objects
│   └── mapper/       # MapStruct mappers
└── infrastructure/
    ├── adapters/     # HTTP clients, persistence adapters
    ├── config/       # Virtual Thread configurations
    └── monitoring/   # Virtual Thread metrics
```

## Configuration Profiles

### Local Development (`local` profile)
- Local PostgreSQL (port 5432)
- Local Redis (port 6379)
- Local Kafka (port 9092)
- Virtual threads enabled with debug logging
- Max Virtual Threads: 10,000

### Docker Profile (`docker` profile)
- Optimized for containerized environments
- Virtual Thread JVM parameters configured
- Enhanced connection pooling for containers
- Resource limits: 4GB memory, 4 CPUs

### Production Profile (`prod` profile)
- Optimized Virtual Thread settings
- Enhanced monitoring and observability
- Production-grade connection pools
- Full security features activated

## Virtual Thread Performance Characteristics

### Target Metrics
- **Volume**: Process 10M+ accounts efficiently
- **Concurrency**: 10,000+ virtual threads simultaneously
- **Memory**: Low memory footprint per virtual thread (~1KB)
- **Latency**: P99 < 500ms for batch operations
- **Throughput**: 10,000+ accounts/second processing rate

### Virtual Thread Configuration
Located in `application.yml`:
```yaml
openfinance:
  accounts:
    batch:
      virtual-threads:
        max: 10000  # Maximum concurrent virtual threads
      timeout-minutes: 120
```

## OpenAPI Code Generation

The system uses OpenAPI-first development with automatic code generation:

- **Specifications**: Located in `src/main/resources/openapi/`
- **Accounts API**: `accounts-2.4.2.yml` (Open Finance Brasil v2.4.2)
- **Consents API**: `consents-3.2.0.yml` (Open Finance Brasil v3.2.0)
- **Generated Models**: Created during Maven compile phase
- **Generation occurs automatically during `mvn compile`**

## Testing Strategy

### Virtual Thread Performance Testing
```bash
# Located in: src/test/java/br/com/openfinance/accounts/benchmark/
mvn test -Dtest=VirtualThreadPerformanceBenchmark
```

### Integration Testing with TestContainers
```bash
# Tests Virtual Thread integration with real databases
mvn test -Dtest=VirtualThreadIntegrationTest
```

### Batch Processing Tests
```bash
# Tests Virtual Thread batch processing logic
mvn test -Dtest=VirtualThreadAccountBatchProcessorTest
```

## Monitoring and Observability

### Virtual Thread Metrics
- **Micrometer** with Prometheus export for Virtual Thread metrics
- **Custom Metrics** via `VirtualThreadMetrics.java`
- **Thread Dump Analysis** available via actuator endpoints
- **Structured Concurrency Monitoring** for batch operations

### Health Checks
- Virtual Thread pool health monitoring
- Database connectivity with Virtual Thread awareness
- Kafka broker health with async Virtual Thread processing
- Redis cluster status with Virtual Thread integration

### Key Monitoring Endpoints
```bash
# Virtual Thread status
GET /actuator/metrics/jvm.threads.virtual

# Thread dump analysis
GET /actuator/threaddump

# Performance metrics
GET /actuator/prometheus
```

## Infrastructure Requirements

### Development Environment
- **Java 21** with `--enable-preview` flag
- **PostgreSQL** 16+ with connection pooling for Virtual Threads
- **Redis** 7+ for distributed caching
- **Docker** and **Docker Compose** for infrastructure

### Production Environment
- **Memory**: Minimum 4GB (Virtual Threads are memory efficient)
- **CPU**: Minimum 4 cores (Virtual Threads scale with available cores)
- **JVM Settings**: `--enable-preview` and Virtual Thread optimizations
- **Connection Pools**: Optimized for Virtual Thread concurrency patterns

## Troubleshooting

### Virtual Thread Issues
**Virtual Thread Pool Exhaustion**
- Check `MAX_VIRTUAL_THREADS` configuration in `application.yml:68`
- Monitor thread dumps via actuator endpoint
- Review `VirtualThreadMetrics.java` for pool usage

**Performance Degradation**
- Enable Virtual Thread debug logging: `java.lang.VirtualThread: DEBUG`
- Monitor Prometheus metrics for Virtual Thread utilization
- Check `VirtualThreadPerformanceBenchmark.java` for baseline performance

**Database Connection Issues with Virtual Threads**
- Verify HikariCP pool settings are optimized for Virtual Threads
- Check `maximum-pool-size: 100` in `application.yml:17`
- Review Virtual Thread to database connection ratios

## Development Workflow

### New Feature Development
1. Start infrastructure: `docker-compose -f docker-compose-vthreads.yml up -d`
2. Build core module: `cd openfinance-core-module && mvn clean install`
3. Implement domain logic following hexagonal architecture
4. Use Virtual Threads for all I/O bound operations
5. Add Virtual Thread performance tests
6. Test with local profile: `mvn spring-boot:run -Dspring.profiles.active=local`

### Virtual Thread Best Practices
- Use `StructuredTaskScope` for coordinated Virtual Thread execution
- Implement proper semaphore controls to prevent resource exhaustion
- Monitor Virtual Thread metrics during development
- Test Virtual Thread performance with realistic data volumes

## Important Files and Locations

### Virtual Thread Implementation
- `VirtualThreadsConfig.java` - Virtual Thread configuration
- `VirtualThreadAccountBatchProcessor.java` - Main batch processing with Virtual Threads
- `VirtualThreadMetrics.java` - Performance monitoring
- `VirtualThreadPerformanceBenchmark.java` - Performance testing

### Configuration Files
- `application.yml` - Main configuration with Virtual Thread settings
- `docker-compose-vthreads.yml` - Docker setup optimized for Virtual Threads
- `pom.xml` - Maven configuration with Java 21 preview features

### Key Service Classes
- `AccountApplicationService.java` - Main service orchestration
- `AccountDomainService.java` - Business logic implementation
- `TransmitterAccountClientAdapter.java` - External API integration with Virtual Threads

## Important Notes

- **Java 21 Required**: Virtual Threads and preview features are essential for system operation
- **Build Order**: Always build `openfinance-core-module` first before other services
- **Profile Management**: Use appropriate profile for your environment (`local`, `docker`, `prod`)
- **Virtual Thread Optimization**: System is specifically optimized for Virtual Thread concurrency patterns
- **Resource Monitoring**: Monitor Virtual Thread usage and performance metrics regularly