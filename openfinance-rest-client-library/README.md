# OpenFinance REST Client Library

A high-performance, fault-tolerant REST client library for Java 21 applications, designed to handle up to 50,000 concurrent requests. The library provides both reactive (WebClient) and imperative (RestClient + Virtual Threads) implementations with comprehensive event consumption capabilities from database and Kafka sources.

## 🚀 Features

### Dual Implementation Strategy
- **Reactive**: Non-blocking I/O with WebClient and reactive streams (Mono/Flux)
- **Imperative**: Virtual Threads with RestClient for traditional blocking I/O patterns

### Event-Driven Architecture
- **Database Events**: Poll database tables for new events (JDBC/R2DBC)
- **Kafka Events**: Consume events from Kafka topics with automatic offset management

### High Performance & Scalability
- Support for up to **50,000 concurrent requests**
- Virtual Threads for massive parallelism
- Optimized connection pooling
- Structured concurrency support

### Resilience Patterns
- Circuit Breaker with configurable failure thresholds
- Retry mechanism with exponential backoff
- Rate limiting to prevent API overload
- Bulkhead pattern for resource isolation

### Enterprise Ready
- OpenAPI code generation from YAML specifications
- Comprehensive metrics and monitoring
- Spring Boot auto-configuration
- Actuator health checks integration

## 📋 Requirements

- **Java 21** (Virtual Threads support required)
- **Spring Boot 3.4.8+**
- **Maven 3.8+**

## 🛠 Installation

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>br.com.openfinance</groupId>
    <artifactId>rest-client-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## ⚡ Quick Start

### 1. Basic Configuration

```yaml
# application.yml
openfinance:
  rest-client:
    base-url: https://api.openfinance.org.br
    strategy: reactive  # or imperative
    max-connections: 1000
    connection-timeout: 10s
    read-timeout: 30s
    
    # Resilience configuration
    resilience:
      circuit-breaker-enabled: true
      failure-rate-threshold: 60.0
      retry-enabled: true
      max-retry-attempts: 3
      
    # Virtual Threads (for imperative strategy)
    virtual-threads:
      enabled: true
      max-virtual-threads: 50000
      structured-concurrency: true
```

### 2. Using the Client

```java
@Service
public class AccountService {
    
    @Autowired
    private ApiClient<?> apiClient;
    
    // Reactive approach
    public Mono<AccountDto> getAccount(String accountId) {
        return apiClient.getReactive("/accounts/" + accountId, AccountDto.class);
    }
    
    // Imperative approach
    public CompletableFuture<AccountDto> getAccountAsync(String accountId) {
        return apiClient.getImperative("/accounts/" + accountId, AccountDto.class);
    }
    
    // Batch processing
    public Flux<AccountDto> getMultipleAccounts(List<String> accountIds) {
        List<String> endpoints = accountIds.stream()
            .map(id -> "/accounts/" + id)
            .toList();
            
        return apiClient.getBatchReactive(Flux.fromIterable(endpoints), AccountDto.class);
    }
}
```

## 🎯 Implementation Strategies

### Reactive Implementation (WebClient)

Best for:
- Applications already using Spring WebFlux
- Non-blocking I/O requirements
- Backpressure handling
- Memory-efficient high-load scenarios

```yaml
openfinance:
  rest-client:
    strategy: reactive
```

### Imperative Implementation (RestClient + Virtual Threads)

Best for:
- Traditional Spring MVC applications
- Familiar blocking programming model
- Massive parallelism requirements
- CPU-intensive processing

```yaml
openfinance:
  rest-client:
    strategy: imperative
    virtual-threads:
      enabled: true
      max-virtual-threads: 50000
```

## 📊 Event Consumption

### Database Events

Configure database polling for events:

```yaml
openfinance:
  rest-client:
    events:
      enabled: true
      source: database
      polling-interval: 5s
      batch-size: 100
      database:
        table-name: api_events
        reactive: true  # Use R2DBC, false for JDBC
```

Required database schema:

```sql
CREATE TABLE api_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id UUID,
    version VARCHAR(20),
    retry_count INTEGER DEFAULT 0,
    processed BOOLEAN DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    target_endpoint VARCHAR(200),
    http_method VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE
);
```

### Kafka Events

Configure Kafka event consumption:

```yaml
openfinance:
  rest-client:
    events:
      enabled: true
      source: kafka
      kafka:
        topic: api-events
        group-id: rest-client-consumer
        bootstrap-servers: localhost:9092
        reactive: true  # Use reactor-kafka
```

Event consumption usage:

```java
@Service
public class EventProcessor {
    
    @Autowired
    private EventConsumer<AccountEventDto> eventConsumer;
    
    @Autowired
    private ApiClient<?> apiClient;
    
    @EventListener
    public void startEventProcessing() {
        eventConsumer.consume()
            .flatMap(this::processEvent)
            .subscribe();
    }
    
    private Mono<Void> processEvent(EventEnvelope<AccountEventDto> event) {
        return apiClient.postReactive(
                event.getTargetEndpoint(), 
                event.getPayload(), 
                Void.class)
            .doOnSuccess(result -> 
                log.info("Processed event: {}", event.getEventId()))
            .then();
    }
}
```

## 🔧 Advanced Configuration

### High Concurrency Setup

For 50k concurrent requests:

```yaml
openfinance:
  rest-client:
    max-connections: 5000
    max-connections-per-route: 1000
    connection-timeout: 5s
    read-timeout: 15s
    
    virtual-threads:
      max-virtual-threads: 50000
      
    resilience:
      rate-limiter-enabled: true
      limit-for-period: 10000
      limit-refresh-period: 1s
      bulkhead-enabled: true
      max-concurrent-calls: 5000
```

### Security Configuration

```yaml
openfinance:
  rest-client:
    security:
      oauth2-enabled: true
      token-endpoint: https://auth.openfinance.org.br/token
      client-id: ${OPENFINANCE_CLIENT_ID}
      client-secret: ${OPENFINANCE_CLIENT_SECRET}
      scope: accounts consents
      token-cache-duration: 30m
```

### OpenAPI Code Generation

Place your OpenAPI specification in `src/main/resources/openapi/`:

```yaml
# src/main/resources/openapi/accounts-api.yml
openapi: 3.0.0
info:
  title: Accounts API
  version: 2.4.2
paths:
  /accounts:
    get:
      # Your API specification
```

The Maven plugin will automatically generate interfaces and DTOs during compilation.

## 📈 Monitoring & Metrics

Built-in metrics available via Micrometer:

- `rest_client_request_duration` - Request timing
- `rest_client_requests_total` - Request counters by status
- `rest_client_health` - Client health status
- `db_events_consumed_total` - Database events processed
- `kafka_events_consumed_total` - Kafka events processed

### Actuator Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

Health check endpoint: `GET /actuator/health/restClient`

## 🧪 Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    
    @Mock
    private ApiClient<?> apiClient;
    
    @InjectMocks
    private AccountService accountService;
    
    @Test
    void shouldGetAccount() {
        // Given
        AccountDto expectedAccount = new AccountDto();
        when(apiClient.getReactive(anyString(), eq(AccountDto.class)))
            .thenReturn(Mono.just(expectedAccount));
        
        // When & Then
        StepVerifier.create(accountService.getAccount("123"))
            .expectNext(expectedAccount)
            .verifyComplete();
    }
}
```

### Integration Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class RestClientIntegrationTest {
    
    @Container
    static WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:2.35.0")
            .withMappingFromResource("account-api-stubs.json");
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withInitScript("test-schema.sql");
    
    @Autowired
    private ApiClient<?> apiClient;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("openfinance.rest-client.base-url", wireMock::getBaseUrl);
        registry.add("spring.r2dbc.url", () -> 
            "r2dbc:postgresql://" + postgres.getHost() + ":" + 
            postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
    }
    
    @Test
    void shouldProcessEventsAndCallApi() {
        // Your integration test logic
    }
}
```

## 🚀 Performance Tuning

### JVM Configuration for Virtual Threads

```bash
java -XX:+EnableDynamicAgentLoading \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseZGC \
     --enable-preview \
     -jar your-application.jar
```

### Database Connection Pool (for event consumption)

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10
      max-size: 100
      max-idle-time: 30m
      
  datasource:
    hikari:
      maximum-pool-size: 100
      minimum-idle: 10
      connection-timeout: 20000
```

### Kafka Consumer Optimization

```yaml
spring:
  kafka:
    consumer:
      properties:
        fetch.min.bytes: 50000
        fetch.max.wait.ms: 500
        max.poll.records: 1000
        session.timeout.ms: 30000
```

## 🔍 Troubleshooting

### Common Issues

**Virtual Thread Exhaustion**
- Check `maxVirtualThreads` configuration
- Monitor thread metrics: `/actuator/metrics/jvm.threads.virtual`
- Review semaphore configuration

**Database Connection Issues**
- Verify connection pool settings
- Check R2DBC/JDBC configuration
- Monitor connection pool metrics

**Kafka Consumer Lag**
- Review consumer group configuration
- Check partition assignment
- Monitor consumer lag metrics

### Debug Logging

```yaml
logging:
  level:
    br.com.openfinance.restclient: DEBUG
    io.github.resilience4j: DEBUG
    reactor.netty: DEBUG
```

## 📚 Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                 Application Layer                   │
├─────────────────────────────────────────────────────┤
│              REST Client Starter                    │
│           (Auto-Configuration)                      │
├─────────────────┬─────────────────┬─────────────────┤
│   Reactive      │      Core       │   Imperative    │
│  (WebClient)    │   (Interfaces)  │ (RestClient+VT) │
├─────────────────┴─────────────────┴─────────────────┤
│                Events Module                        │
│        (Database/Kafka Consumers)                   │
├─────────────────────────────────────────────────────┤
│              Infrastructure                         │
│  (Resilience4j, Metrics, Security)                 │
└─────────────────────────────────────────────────────┘
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Run the test suite: `mvn test`
5. Create a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For questions or issues:
- Create an issue on GitHub
- Check the [troubleshooting guide](#-troubleshooting)
- Review the [examples](examples/) directory

---

**Built with ❤️ for high-performance Java applications**