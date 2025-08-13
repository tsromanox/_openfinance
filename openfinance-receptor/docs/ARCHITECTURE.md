# OpenFinance Receptor - Architecture Documentation

## Overview

OpenFinance Receptor is a high-performance, scalable platform for processing Open Finance Brasil resources built with Java 21 Virtual Threads and Structured Concurrency. The system follows hexagonal (ports and adapters) architecture principles with clean separation between domain, application, and infrastructure layers.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              OpenFinance Receptor                               │
│                          Java 21 Virtual Threads Platform                      │
└─────────────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                 Bootstrap                                       │
│              Application Entry Point & Configuration Assembly                  │
└─────────────────────────────────────────────────────────────────────────────────┘
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  ┌─────────────┐
│  Service Layer   │  │  Service Layer   │  │  Service Layer   │  │ Controllers │
│                  │  │                  │  │                  │  │             │
│ service-accounts │  │ service-consents │  │ service-resources│  │ REST APIs   │
│                  │  │                  │  │                  │  │             │
└──────────────────┘  └──────────────────┘  └──────────────────┘  └─────────────┘
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Core Application                                   │
│                     Use Cases, Ports & Application Services                    │
└─────────────────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               Core Domain                                       │
│                    Business Entities, Value Objects & Rules                    │
└─────────────────────────────────────────────────────────────────────────────────┘
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Infrastructure  │  │ Infrastructure  │  │ Infrastructure  │  │ Infrastructure  │
│                 │  │                 │  │                 │  │                 │
│   Persistence   │  │     Client      │  │   Scheduler     │  │   Monitoring    │
│                 │  │                 │  │                 │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Module Structure

### Core Modules

#### 1. **core-domain**
- **Purpose**: Contains business entities, value objects, and domain rules
- **Dependencies**: None (pure domain logic)
- **Key Components**:
  - Domain entities (Account, Consent, Resource)
  - Value objects (AccountNumber, AgencyNumber, Balance)
  - Enumerations (ConsentStatus, ResourceType, ResourceStatus)
  - Domain events and exceptions

#### 2. **core-application**
- **Purpose**: Application orchestration layer with use cases and ports
- **Dependencies**: core-domain
- **Key Components**:
  - Use case interfaces (AccountUseCase, ConsentUseCase, ResourceUseCase)
  - Output ports (repositories, external services)
  - Application services (orchestration logic)
  - DTOs and application exceptions

### Service Modules

#### 3. **service-resources**
- **Purpose**: High-performance resource processing with Virtual Threads
- **Dependencies**: core-domain
- **Key Components**:
  - Virtual Thread optimized processors
  - Structured Concurrency coordination
  - Adaptive resource management
  - Performance monitoring and metrics
  - Comprehensive configuration profiles

#### 4. **service-accounts** & **service-consents**
- **Purpose**: Specialized processing for accounts and consents
- **Dependencies**: core-domain, core-application
- **Key Components**:
  - Domain-specific business logic
  - Integration adapters
  - Batch processing capabilities

### Infrastructure Modules

#### 5. **infrastructure-persistence**
- **Purpose**: Data persistence layer with JPA/Hibernate
- **Dependencies**: core-domain, core-application
- **Key Components**:
  - JPA entities and repositories
  - Database migrations (Flyway)
  - Connection pooling (HikariCP)
  - Transaction management

#### 6. **infrastructure-client**
- **Purpose**: External API integration
- **Dependencies**: core-application
- **Key Components**:
  - OpenAPI client generation
  - HTTP client configuration
  - Circuit breakers (Resilience4j)
  - Rate limiting

#### 7. **infrastructure-scheduler**
- **Purpose**: Background job processing
- **Dependencies**: core-application
- **Key Components**:
  - Quartz scheduler integration
  - Job queues and processing
  - Retry mechanisms
  - Monitoring hooks

### Assembly Module

#### 8. **bootstrap**
- **Purpose**: Application entry point and configuration assembly
- **Dependencies**: All modules
- **Key Components**:
  - Spring Boot configuration
  - Dependency injection setup
  - Virtual Thread configuration
  - REST controllers
  - Health checks and monitoring

## Design Principles

### Hexagonal Architecture

The system follows hexagonal architecture principles:

```
                     ┌─────────────────────────────────┐
                     │                                 │
                     │           Domain                │
                     │      (Business Logic)           │
                     │                                 │
                     └─────────────────────────────────┘
                                      │
            ┌─────────────────────────────────────────────────────────┐
            │                                                         │
            │                   Application                           │
            │              (Use Cases & Ports)                        │
            │                                                         │
            └─────────────────────────────────────────────────────────┘
                  │                                            │
         Input Ports                                    Output Ports
              │                                                │
┌─────────────────────────┐                    ┌─────────────────────────┐
│                         │                    │                         │
│      Controllers        │                    │     Infrastructure      │
│    (REST, GraphQL)      │                    │  (Database, External    │
│                         │                    │   APIs, Messaging)      │
└─────────────────────────┘                    └─────────────────────────┘
```

### Key Benefits

1. **Independence**: Domain logic is independent of infrastructure
2. **Testability**: Easy unit testing through port interfaces
3. **Flexibility**: Infrastructure can be swapped without affecting business logic
4. **Maintainability**: Clear separation of concerns

### Dependency Rules

- **Domain Layer**: No dependencies on other layers
- **Application Layer**: Only depends on Domain
- **Infrastructure Layer**: Depends on Domain and Application
- **Bootstrap Layer**: Assembles all components

## Virtual Threads Integration

### Virtual Thread Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Virtual Thread Scheduler                               │
│                    (jdk.virtualThreadScheduler.parallelism)                     │
└─────────────────────────────────────────────────────────────────────────────────┘
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   Discovery     │ │      Sync       │ │   Validation    │ │   Monitoring    │
│ Virtual Threads │ │ Virtual Threads │ │ Virtual Threads │ │ Virtual Threads │
│   (400-800)     │ │   (500-1500)    │ │   (300-800)     │ │   (350-1000)    │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   API Calls     │ │     Batch       │ │   Reactive      │ │   Structured    │
│ Virtual Threads │ │   Processing    │ │   Processing    │ │  Concurrency    │
│  (2000-6000)    │ │ Virtual Threads │ │ Virtual Threads │ │   Coordination  │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
```

### Virtual Thread Benefits

1. **Massive Parallelism**: Support for millions of concurrent operations
2. **Resource Efficiency**: Minimal memory footprint per thread
3. **Blocking I/O Optimization**: Virtual threads handle blocking operations efficiently
4. **Structured Concurrency**: Coordinated parallel execution with automatic cleanup

### Performance Characteristics

- **Maximum Virtual Threads**: 25,000+ (load-test profile)
- **Concurrent Operations**: 1,000-10,000 depending on operation type
- **Throughput**: 1,000+ operations/second sustained
- **Memory Efficiency**: ~1KB per Virtual Thread vs ~8MB per Platform Thread

## Data Flow Architecture

### Resource Processing Pipeline

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Discovery     │───▶│  Synchronization│───▶│   Validation    │
│                 │    │                 │    │                 │
│ • Endpoint scan │    │ • Data retrieval│    │ • Format check  │
│ • Resource ID   │    │ • Status update │    │ • Business rules│
│ • Metadata      │    │ • Error handling│    │ • Compliance    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Persistence   │    │   Monitoring    │    │   Metrics       │
│                 │    │                 │    │                 │
│ • Database      │    │ • Health checks │    │ • Performance   │
│ • Event store   │    │ • Alerts        │    │ • Business KPIs │
│ • Audit log     │    │ • Recovery      │    │ • Dashboards    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Adaptive Resource Management

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Adaptive Resource Manager                                │
│                     (Real-time Performance Feedback)                           │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                 ┌────────────────────────────────────────────────────┐
                 │                                                    │
                 ▼                                                    ▼
┌─────────────────────────────┐                    ┌─────────────────────────────┐
│     System Monitoring       │                    │     Dynamic Adjustment      │
│                             │                    │                             │
│ • CPU Usage (40-80%)        │                    │ • Batch Size (100-6000)     │
│ • Memory Usage (50-85%)     │                    │ • Concurrency (50-3500)     │
│ • Error Rate (<25%)         │                    │ • Timeouts (10s-300s)       │
│ • Throughput (ops/sec)      │                    │ • Interval (10s-5min)       │
└─────────────────────────────┘                    └─────────────────────────────┘
```

## Integration Patterns

### Event-Driven Architecture

```
┌─────────────┐    Event Bus    ┌─────────────┐    Event Bus    ┌─────────────┐
│   Service   │────────────────▶│   Domain    │────────────────▶│   Service   │
│     A       │                 │   Events    │                 │     B       │
│             │◀────────────────│             │◀────────────────│             │
└─────────────┘                 └─────────────┘                 └─────────────┘
```

### Circuit Breaker Pattern

```
┌─────────────┐                 ┌─────────────┐                 ┌─────────────┐
│   Client    │────────────────▶│  Circuit    │────────────────▶│ External    │
│             │                 │  Breaker    │                 │ Service     │
│             │◀────────────────│             │◀────────────────│             │
└─────────────┘                 └─────────────┘                 └─────────────┘
                                      │
                                      ▼
                            States: CLOSED → OPEN → HALF_OPEN
                            Thresholds: Failure rate, Response time
```

### Repository Pattern

```
┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
│   Application   │──────────▶│   Repository    │──────────▶│  Infrastructure │
│    Service      │           │   Interface     │           │   Implementation│
│                 │           │    (Port)       │           │    (Adapter)    │
└─────────────────┘           └─────────────────┘           └─────────────────┘
```

## Deployment Architecture

### Containerization

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            Docker Container                                     │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                     OpenFinance Receptor                                  │  │
│  │                                                                           │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │  │
│  │  │ Bootstrap   │  │ Services    │  │ Application │  │ Domain      │      │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘      │  │
│  │                                                                           │  │
│  │  JVM: Java 21 + Virtual Threads + ZGC + 4-12GB RAM                       │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openfinance-receptor
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: openfinance-receptor
        image: openfinance-receptor:1.0.0
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JVM_OPTS
          value: "--enable-preview -XX:+UseZGC -XX:+EnableVirtualThreads"
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"  
            cpu: "4"
```

## Quality Attributes

### Performance
- **Throughput**: 1,000+ operations/second
- **Latency**: P99 < 500ms for resource operations
- **Concurrency**: 25,000+ Virtual Threads
- **Memory**: ~1KB per Virtual Thread

### Scalability
- **Horizontal**: Multiple application instances
- **Vertical**: Adaptive resource management
- **Load Distribution**: Multiple Virtual Thread pools

### Reliability
- **Circuit Breakers**: Automatic failure handling
- **Retry Mechanisms**: Exponential backoff
- **Health Checks**: Comprehensive monitoring
- **Graceful Degradation**: Adaptive performance adjustment

### Maintainability
- **Clean Architecture**: Clear separation of concerns
- **Comprehensive Testing**: Unit, integration, performance tests
- **Documentation**: Architecture, API, and deployment guides
- **Observability**: Detailed metrics and logging

This architecture provides a solid foundation for building high-performance, scalable applications that leverage the latest Java 21 features while maintaining clean separation of concerns and testability.