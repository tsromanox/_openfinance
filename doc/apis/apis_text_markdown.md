# Guia Completo: API REST Open Finance Brasil com Spring Boot 3.5.6 e Java 21

O Open Finance Brasil exige arquitetura robusta, escalável e em conformidade regulatória. Este guia técnico fornece configurações completas, código de produção e melhores práticas para implementar a API de Consents usando tecnologias modernas como Virtual Threads, arquitetura hexagonal e cache distribuído.

## Stack tecnológica moderna alcança 500+ requisições simultâneas

A combinação de **Spring Boot 3.5.6 com Virtual Threads do Java 21** entrega escalabilidade comparável a sistemas reativos, mas mantendo código imperativo simples. Virtual Threads permitem 1KB de overhead por thread (vs. 1MB das threads tradicionais), viabilizando milhares de conexões concorrentes. MongoDB com índices otimizados garante latência de 1-5ms em consultas por ID. Hazelcast distribui cache entre instâncias para reduzir carga no banco. Resilience4j implementa rate limiting conforme padrões Open Finance (300 TPS base, escalando 150 TPS por demanda).

### Conformidade regulatória do Open Finance Brasil

A especificação oficial Consents API v2.2.0 exige **mTLS obrigatório** com certificados ICP-Brasil, OAuth 2.0 FAPI-Advanced com scopes dinâmicos (`consent:urn:bancoex:{consentId}`), e rate limiting rigoroso. O Banco Central define limites de **300 TPS (transações por segundo) mínimo**, escalando +150 TPS quando 90% da capacidade é atingida por 3 quinzenas consecutivas. Violações retornam HTTP 529. Rate limiting por TPM (transações por minuto) varia de 1.000 a 10.000+ TPM baseado em quantidade de consentimentos ativos (QCA). APIs de Consents estão **isentas de rate limiting**, mas precisam resiliência para dependências. Status HTTP 429 indica TPM excedido.

## Estrutura de projeto hexagonal com três módulos

### Organização de diretórios Maven multi-módulo

```
open-finance-consents-api/
├── pom.xml (parent)
├── domain/
│   ├── pom.xml (SEM dependências Spring)
│   └── src/main/java/com/bank/consent/domain/
│       ├── model/
│       │   ├── Consent.java
│       │   ├── ConsentId.java (Value Object)
│       │   ├── ConsentStatus.java (Enum)
│       │   └── ConsentPermission.java
│       └── service/
│           └── ConsentValidationService.java (regras de negócio puras)
│
├── application/
│   ├── pom.xml (depende de domain, SEM Spring)
│   └── src/main/java/com/bank/consent/application/
│       ├── port/
│       │   ├── in/
│       │   │   ├── GetConsentUseCase.java
│       │   │   ├── CreateConsentUseCase.java
│       │   │   └── command/
│       │   │       └── CreateConsentCommand.java
│       │   └── out/
│       │       ├── LoadConsentPort.java
│       │       ├── SaveConsentPort.java
│       │       └── NotifyConsentPort.java
│       └── service/
│           ├── GetConsentService.java (implementa GetConsentUseCase)
│           └── CreateConsentService.java
│
└── infrastructure/
    ├── pom.xml (depende de domain + application, TODAS deps Spring)
    └── src/main/
        ├── java/com/bank/consent/infrastructure/
        │   ├── adapter/
        │   │   ├── in/
        │   │   │   └── rest/
        │   │   │       ├── ConsentController.java (MVC)
        │   │   │       ├── ConsentWebFluxController.java (opcional)
        │   │   │       ├── dto/
        │   │   │       │   ├── ConsentRequest.java
        │   │   │       │   └── ConsentResponse.java
        │   │   │       └── mapper/
        │   │   │           └── ConsentWebMapper.java
        │   │   └── out/
        │   │       ├── persistence/
        │   │       │   ├── ConsentPersistenceAdapter.java
        │   │       │   ├── entity/
        │   │       │   │   └── ConsentEntity.java (@Document)
        │   │       │   ├── repository/
        │   │       │   │   └── ConsentMongoRepository.java
        │   │       │   └── mapper/
        │   │       │       └── ConsentPersistenceMapper.java
        │   │       └── cache/
        │   │           └── ConsentCacheAdapter.java
        │   └── config/
        │       ├── ApplicationConfig.java
        │       ├── MongoConfig.java
        │       ├── HazelcastConfig.java
        │       ├── Resilience4jConfig.java
        │       ├── ObservabilityConfig.java
        │       └── OpenApiConfig.java
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            ├── hazelcast.yaml
            ├── elasticapm.properties
            └── api/
                └── open-finance-consents-v2.2.0.yml
```

### Dependências Maven do parent POM

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
    </parent>
    
    <groupId>com.bank.openfinance</groupId>
    <artifactId>consents-api-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>domain</module>
        <module>application</module>
        <module>infrastructure</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <openapi-generator.version>7.10.0</openapi-generator.version>
        <hazelcast.version>5.5.0</hazelcast.version>
        <resilience4j.version>2.2.0</resilience4j.version>
        <elastic-apm.version>1.52.1</elastic-apm.version>
        <springdoc.version>2.8.5</springdoc.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- OpenAPI Generator Runtime -->
            <dependency>
                <groupId>org.openapitools</groupId>
                <artifactId>jackson-databind-nullable</artifactId>
                <version>0.2.6</version>
            </dependency>
            
            <!-- Hazelcast -->
            <dependency>
                <groupId>com.hazelcast</groupId>
                <artifactId>hazelcast-spring</artifactId>
                <version>${hazelcast.version}</version>
            </dependency>
            
            <!-- Resilience4j -->
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-spring-boot3</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            
            <!-- Elastic APM -->
            <dependency>
                <groupId>co.elastic.apm</groupId>
                <artifactId>apm-agent-attach</artifactId>
                <version>${elastic-apm.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Infrastructure module dependencies completas

```xml
<!-- infrastructure/pom.xml -->
<dependencies>
    <!-- Internal modules -->
    <dependency>
        <groupId>com.bank.openfinance</groupId>
        <artifactId>domain</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.bank.openfinance</groupId>
        <artifactId>application</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Spring Boot Web (MVC com Virtual Threads) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- Hazelcast Cache -->
    <dependency>
        <groupId>com.hazelcast</groupId>
        <artifactId>hazelcast</artifactId>
        <version>${hazelcast.version}</version>
    </dependency>
    <dependency>
        <groupId>com.hazelcast</groupId>
        <artifactId>hazelcast-spring</artifactId>
    </dependency>
    
    <!-- Resilience4j -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    
    <!-- Actuator + Micrometer -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Elastic APM -->
    <dependency>
        <groupId>co.elastic.apm</groupId>
        <artifactId>apm-agent-attach</artifactId>
    </dependency>
    
    <!-- OpenAPI Generator -->
    <dependency>
        <groupId>org.openapitools</groupId>
        <artifactId>jackson-databind-nullable</artifactId>
    </dependency>
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
        <version>2.2.22</version>
    </dependency>
    
    <!-- SpringDoc OpenAPI UI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>
    
    <!-- Lombok (opcional mas recomendado) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- OpenAPI Generator Plugin -->
        <plugin>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-maven-plugin</artifactId>
            <version>${openapi-generator.version}</version>
            <executions>
                <execution>
                    <goals><goal>generate</goal></goals>
                    <configuration>
                        <inputSpec>${project.basedir}/src/main/resources/api/open-finance-consents-v2.2.0.yml</inputSpec>
                        <generatorName>spring</generatorName>
                        <library>spring-boot</library>
                        <apiPackage>com.bank.consent.api</apiPackage>
                        <modelPackage>com.bank.consent.api.model</modelPackage>
                        <configOptions>
                            <useSpringBoot3>true</useSpringBoot3>
                            <interfaceOnly>true</interfaceOnly>
                            <useBeanValidation>true</useBeanValidation>
                            <useResponseEntity>true</useResponseEntity>
                            <documentationProvider>springdoc</documentationProvider>
                            <dateLibrary>java8</dateLibrary>
                        </configOptions>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        
        <!-- Build Helper (adiciona código gerado) -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals><goal>add-source</goal></goals>
                    <configuration>
                        <sources>
                            <source>${project.build.directory}/generated-sources/openapi/src/main/java</source>
                        </sources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

## Configuração completa com Virtual Threads e MongoDB

### Application.yml production-ready para alta concorrência

```yaml
spring:
  application:
    name: open-finance-consents-api
  
  # Virtual Threads (Java 21) - Chave para 500+ requisições
  threads:
    virtual:
      enabled: true
  
  main:
    keep-alive: true  # Previne saída da JVM com virtual threads
  
  # MongoDB - Configuração para high availability
  data:
    mongodb:
      # Replica Set com 3 membros para HA
      uri: mongodb://mongouser:${MONGO_PASSWORD}@mongo1:27017,mongo2:27017,mongo3:27017/consents?replicaSet=rs0&retryWrites=true&w=majority&readPreference=secondaryPreferred&maxPoolSize=200&minPoolSize=50&maxIdleTimeMS=60000&maxLifeTimeMS=300000&waitQueueTimeoutMS=15000&serverSelectionTimeoutMS=30000&connectTimeoutMS=10000&socketTimeoutMS=30000
      
      database: consents
      auto-index-creation: false  # Criar índices manualmente em produção
      uuid-representation: standard
  
  # Spring Cache com Hazelcast
  cache:
    type: hazelcast
    cache-names:
      - consents
      - consents-by-customer

# Server Configuration - Tomcat otimizado para Virtual Threads
server:
  port: 8080
  tomcat:
    threads:
      max: 10000  # Limite alto para Virtual Threads
      min-spare: 50
    connection-timeout: 20s
    max-connections: 10000
    accept-count: 1000
    mbeanregistry:
      enabled: true  # Para monitoramento JMX
  shutdown: graceful
  compression:
    enabled: true

# Hazelcast Distributed Cache Configuration
hazelcast:
  network:
    join:
      multicast:
        enabled: true
    port:
      auto-increment: true
      port: 5701
  map:
    consents:
      time-to-live-seconds: 600  # 10 minutos
      max-idle-seconds: 300
      eviction:
        eviction-policy: LRU
        max-size-policy: PER_NODE
        size: 10000
    consents-by-customer:
      time-to-live-seconds: 300
      max-idle-seconds: 180

# Resilience4j - Rate Limiting e Retry
resilience4j:
  ratelimiter:
    instances:
      # Rate limit por IP/cliente conforme Open Finance
      consentApiLimiter:
        limitForPeriod: 2500  # TPM base para alta frequência
        limitRefreshPeriod: 1m
        timeoutDuration: 100ms
      
      mongodbLimiter:
        limitForPeriod: 1000
        limitRefreshPeriod: 1m
        timeoutDuration: 500ms
  
  retry:
    instances:
      mongoRetry:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - com.mongodb.MongoException
          - java.io.IOException
      
      externalServiceRetry:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
  
  circuitbreaker:
    instances:
      mongoCircuitBreaker:
        slidingWindowSize: 100
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 10

# Management e Observability
management:
  server:
    port: 8081  # Porta separada para actuator
  
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus,hazelcast,caches
  
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true  # Kubernetes probes
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    distribution:
      percentiles-histogram:
        http.server.requests: true
        consent.processing.time: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s
    
    tags:
      application: ${spring.application.name}
      environment: production
      region: us-east-1
  
  tracing:
    enabled: true
    sampling:
      probability: 1.0

# SpringDoc OpenAPI Configuration
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    display-request-duration: true

# Logging
logging:
  level:
    root: INFO
    com.bank.consent: DEBUG
    org.springframework.data.mongodb: INFO
    com.hazelcast: WARN
    io.github.resilience4j: DEBUG
```

### Arquivo Hazelcast configuration (hazelcast.yaml)

```yaml
hazelcast:
  cluster-name: consent-api-cluster
  
  network:
    port:
      auto-increment: true
      port: 5701
    join:
      multicast:
        enabled: true
      tcp-ip:
        enabled: false
  
  map:
    consents:
      in-memory-format: BINARY
      backup-count: 1
      async-backup-count: 1
      time-to-live-seconds: 600
      max-idle-seconds: 300
      
      eviction:
        eviction-policy: LRU
        max-size-policy: PER_NODE
        size: 10000
      
      statistics-enabled: true
      
    consents-by-customer:
      in-memory-format: BINARY
      backup-count: 1
      time-to-live-seconds: 300
      max-idle-seconds: 180
      eviction:
        eviction-policy: LRU
        max-size-policy: PER_NODE
        size: 5000
  
  metrics:
    enabled: true
    management-center:
      enabled: false
```

### Elastic APM Configuration (elasticapm.properties)

```properties
# Core Configuration
service_name=open-finance-consents-api
service_version=1.0.0
environment=production

# APM Server
server_url=http://apm-server:8200
secret_token=${ELASTIC_APM_SECRET_TOKEN}

# Application Packages
application_packages=com.bank.consent

# Sampling
transaction_sample_rate=1.0
metrics_interval=30s

# Data Capture
capture_body=all
capture_headers=true
sanitize_field_names=password,token,secret,authorization,cpf,cnpj

# Performance Profiling
profiling_inferred_spans_enabled=true
profiling_inferred_spans_sampling_interval=50ms
profiling_inferred_spans_min_duration=500ms

# Transaction Configuration
transaction_max_spans=500
span_min_duration=5ms
```

## Implementação do domínio e casos de uso

### Domain model - entidade Consent pura

```java
// domain/src/main/java/com/bank/consent/domain/model/Consent.java
package com.bank.consent.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class Consent {
    private final ConsentId id;
    private final String consentId;  // ID externo para API
    private final String customerId;
    private ConsentStatus status;
    private final List<ConsentPermission> permissions;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    private Consent(Builder builder) {
        this.id = builder.id;
        this.consentId = builder.consentId;
        this.customerId = builder.customerId;
        this.status = builder.status;
        this.permissions = builder.permissions;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.expiresAt = builder.expiresAt;
    }
    
    // Business logic
    public void approve() {
        if (this.status != ConsentStatus.AWAITING_AUTHORISATION) {
            throw new IllegalStateException("Cannot approve consent in status: " + status);
        }
        this.status = ConsentStatus.AUTHORISED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void reject() {
        if (this.status == ConsentStatus.REJECTED || this.status == ConsentStatus.CONSUMED) {
            throw new IllegalStateException("Cannot reject consent in status: " + status);
        }
        this.status = ConsentStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return status == ConsentStatus.AUTHORISED && !isExpired();
    }
    
    // Getters
    public ConsentId getId() { return id; }
    public String getConsentId() { return consentId; }
    public String getCustomerId() { return customerId; }
    public ConsentStatus getStatus() { return status; }
    public List<ConsentPermission> getPermissions() { return permissions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ConsentId id;
        private String consentId;
        private String customerId;
        private ConsentStatus status = ConsentStatus.AWAITING_AUTHORISATION;
        private List<ConsentPermission> permissions;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private LocalDateTime expiresAt;
        
        public Builder id(ConsentId id) { this.id = id; return this; }
        public Builder consentId(String consentId) { this.consentId = consentId; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder status(ConsentStatus status) { this.status = status; return this; }
        public Builder permissions(List<ConsentPermission> permissions) { this.permissions = permissions; return this; }
        public Builder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        
        public Consent build() {
            return new Consent(this);
        }
    }
}
```

```java
// domain/src/main/java/com/bank/consent/domain/model/ConsentStatus.java
package com.bank.consent.domain.model;

public enum ConsentStatus {
    AWAITING_AUTHORISATION,
    AUTHORISED,
    REJECTED,
    CONSUMED
}
```

### Application layer - ports e use cases

```java
// application/src/main/java/com/bank/consent/application/port/in/GetConsentUseCase.java
package com.bank.consent.application.port.in;

import com.bank.consent.domain.model.Consent;
import java.util.Optional;

public interface GetConsentUseCase {
    Optional<Consent> execute(String consentId);
}
```

```java
// application/src/main/java/com/bank/consent/application/port/out/LoadConsentPort.java
package com.bank.consent.application.port.out;

import com.bank.consent.domain.model.Consent;
import java.util.Optional;

public interface LoadConsentPort {
    Optional<Consent> findByConsentId(String consentId);
}
```

```java
// application/src/main/java/com/bank/consent/application/service/GetConsentService.java
package com.bank.consent.application.service;

import com.bank.consent.application.port.in.GetConsentUseCase;
import com.bank.consent.application.port.out.LoadConsentPort;
import com.bank.consent.domain.model.Consent;

import java.util.Optional;

// Custom annotation (não @Service para manter sem Spring no application)
public class GetConsentService implements GetConsentUseCase {
    
    private final LoadConsentPort loadConsentPort;
    
    public GetConsentService(LoadConsentPort loadConsentPort) {
        this.loadConsentPort = loadConsentPort;
    }
    
    @Override
    public Optional<Consent> execute(String consentId) {
        return loadConsentPort.findByConsentId(consentId)
            .filter(consent -> !consent.isExpired());
    }
}
```

## MongoDB adapter com cache Hazelcast integrado

### MongoDB entity e repository Spring Data

```java
// infrastructure/.../persistence/entity/ConsentEntity.java
package com.bank.consent.infrastructure.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "consents")
@CompoundIndex(name = "consent_customer_idx", def = "{'consentId': 1, 'customerId': 1}")
@CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}")
public class ConsentEntity {
    
    @Id
    private String id;
    
    @Indexed(unique = true, name = "consent_id_idx")
    private String consentId;
    
    @Indexed(name = "customer_id_idx")
    private String customerId;
    
    private String status;
    
    private List<String> permissions;
    
    @Indexed(name = "created_at_idx")
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Indexed(name = "expires_at_idx", expireAfterSeconds = 0)
    private LocalDateTime expiresAt;
    
    // Getters, setters, constructor
    public ConsentEntity() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
```

```java
// infrastructure/.../persistence/repository/ConsentMongoRepository.java
package com.bank.consent.infrastructure.adapter.out.persistence.repository;

import com.bank.consent.infrastructure.adapter.out.persistence.entity.ConsentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface ConsentMongoRepository extends MongoRepository<ConsentEntity, String> {
    
    @Query("{'consentId': ?0}")
    Optional<ConsentEntity> findByConsentId(String consentId);
    
    @Query("{'customerId': ?0}")
    List<ConsentEntity> findByCustomerId(String customerId);
}
```

### Persistence adapter com cache Hazelcast

```java
// infrastructure/.../persistence/ConsentPersistenceAdapter.java
package com.bank.consent.infrastructure.adapter.out.persistence;

import com.bank.consent.application.port.out.LoadConsentPort;
import com.bank.consent.application.port.out.SaveConsentPort;
import com.bank.consent.domain.model.Consent;
import com.bank.consent.infrastructure.adapter.out.persistence.entity.ConsentEntity;
import com.bank.consent.infrastructure.adapter.out.persistence.mapper.ConsentPersistenceMapper;
import com.bank.consent.infrastructure.adapter.out.persistence.repository.ConsentMongoRepository;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConsentPersistenceAdapter implements LoadConsentPort, SaveConsentPort {
    
    private final ConsentMongoRepository repository;
    private final ConsentPersistenceMapper mapper;
    private final Timer queryTimer;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    
    public ConsentPersistenceAdapter(
            ConsentMongoRepository repository,
            ConsentPersistenceMapper mapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.mapper = mapper;
        this.queryTimer = meterRegistry.timer("consent.mongodb.query.time");
        this.cacheHitCounter = meterRegistry.counter("consent.cache.hits");
        this.cacheMissCounter = meterRegistry.counter("consent.cache.misses");
    }
    
    @Override
    @Cacheable(value = "consents", key = "#consentId")
    @RateLimiter(name = "mongodbLimiter")
    @Retry(name = "mongoRetry")
    public Optional<Consent> findByConsentId(String consentId) {
        return queryTimer.record(() -> {
            Optional<ConsentEntity> entity = repository.findByConsentId(consentId);
            
            if (entity.isPresent()) {
                cacheHitCounter.increment();
                return entity.map(mapper::toDomain);
            } else {
                cacheMissCounter.increment();
                return Optional.empty();
            }
        });
    }
    
    @Override
    @CacheEvict(value = "consents", key = "#consent.consentId")
    @Retry(name = "mongoRetry")
    public Consent save(Consent consent) {
        return queryTimer.record(() -> {
            ConsentEntity entity = mapper.toEntity(consent);
            ConsentEntity saved = repository.save(entity);
            return mapper.toDomain(saved);
        });
    }
}
```

```java
// infrastructure/.../persistence/mapper/ConsentPersistenceMapper.java
package com.bank.consent.infrastructure.adapter.out.persistence.mapper;

import com.bank.consent.domain.model.*;
import com.bank.consent.infrastructure.adapter.out.persistence.entity.ConsentEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ConsentPersistenceMapper {
    
    public Consent toDomain(ConsentEntity entity) {
        return Consent.builder()
            .id(new ConsentId(entity.getId()))
            .consentId(entity.getConsentId())
            .customerId(entity.getCustomerId())
            .status(ConsentStatus.valueOf(entity.getStatus()))
            .permissions(entity.getPermissions().stream()
                .map(ConsentPermission::fromString)
                .collect(Collectors.toList()))
            .expiresAt(entity.getExpiresAt())
            .build();
    }
    
    public ConsentEntity toEntity(Consent domain) {
        ConsentEntity entity = new ConsentEntity();
        entity.setId(domain.getId() != null ? domain.getId().getValue() : null);
        entity.setConsentId(domain.getConsentId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setStatus(domain.getStatus().name());
        entity.setPermissions(domain.getPermissions().stream()
            .map(ConsentPermission::toString)
            .collect(Collectors.toList()));
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        return entity;
    }
}
```

## REST controller com rate limiting e métricas

### Controller implementando OpenAPI interface gerada

```java
// infrastructure/.../adapter/in/rest/ConsentController.java
package com.bank.consent.infrastructure.adapter.in.rest;

import com.bank.consent.api.ConsentsApi;  // Interface gerada pelo OpenAPI Generator
import com.bank.consent.api.model.ConsentResponse;  // DTO gerado
import com.bank.consent.application.port.in.GetConsentUseCase;
import com.bank.consent.domain.model.Consent;
import com.bank.consent.infrastructure.adapter.in.rest.mapper.ConsentWebMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Timed("consent.api")
public class ConsentController implements ConsentsApi {
    
    private final GetConsentUseCase getConsentUseCase;
    private final ConsentWebMapper mapper;
    private final Counter requestCounter;
    private final Counter notFoundCounter;
    
    public ConsentController(
            GetConsentUseCase getConsentUseCase,
            ConsentWebMapper mapper,
            MeterRegistry meterRegistry) {
        this.getConsentUseCase = getConsentUseCase;
        this.mapper = mapper;
        this.requestCounter = meterRegistry.counter("consent.api.requests.total");
        this.notFoundCounter = meterRegistry.counter("consent.api.notfound.total");
    }
    
    @Override
    @RateLimiter(name = "consentApiLimiter", fallbackMethod = "rateLimitFallback")
    @Timed(value = "consent.get", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public ResponseEntity<ConsentResponse> getConsentByConsentId(String consentId) {
        requestCounter.increment();
        
        return getConsentUseCase.execute(consentId)
            .map(consent -> {
                ConsentResponse response = mapper.toResponse(consent);
                return ResponseEntity.ok()
                    .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                    .body(response);
            })
            .orElseGet(() -> {
                notFoundCounter.increment();
                return ResponseEntity.notFound().build();
            });
    }
    
    // Fallback method para rate limiting
    public ResponseEntity<ConsentResponse> rateLimitFallback(String consentId, Throwable ex) {
        return ResponseEntity.status(429)
            .header("X-RateLimit-Retry-After", "60")
            .header("Retry-After", "60")
            .build();
    }
}
```

## Configuração Spring com todas integrações

### Configuration classes para cada componente

```java
// infrastructure/.../config/MongoConfig.java
package com.bank.consent.infrastructure.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoRepositories(basePackages = "com.bank.consent.infrastructure.adapter.out.persistence.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {
    
    @Value("${spring.data.mongodb.uri}")
    private String connectionString;
    
    @Value("${spring.data.mongodb.database}")
    private String database;
    
    @Override
    protected String getDatabaseName() {
        return database;
    }
    
    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        builder
            .applyConnectionString(new ConnectionString(connectionString))
            .applyToConnectionPoolSettings(settings -> settings
                .minSize(50)
                .maxSize(200)
                .maxWaitTime(15000, TimeUnit.MILLISECONDS)
                .maxConnectionLifeTime(300000, TimeUnit.MILLISECONDS)
                .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)
            )
            .readPreference(ReadPreference.secondaryPreferred())
            .writeConcern(WriteConcern.MAJORITY);
    }
}
```

```java
// infrastructure/.../config/HazelcastConfig.java
package com.bank.consent.infrastructure.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class HazelcastConfig {
    
    @Bean
    public Config hazelcastConfig() {
        Config config = new Config();
        config.setClusterName("consent-api-cluster");
        
        // Network configuration
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701).setPortAutoIncrement(true);
        
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(true);
        
        // Map configuration for consents cache
        MapConfig consentsMapConfig = new MapConfig()
            .setName("consents")
            .setTimeToLiveSeconds(600)
            .setMaxIdleSeconds(300)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(10000))
            .setBackupCount(1)
            .setStatisticsEnabled(true);
        
        config.addMapConfig(consentsMapConfig);
        
        // Map configuration for customer consents
        MapConfig customerConsentsMapConfig = new MapConfig()
            .setName("consents-by-customer")
            .setTimeToLiveSeconds(300)
            .setMaxIdleSeconds(180)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(5000))
            .setBackupCount(1);
        
        config.addMapConfig(customerConsentsMapConfig);
        
        return config;
    }
    
    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        return Hazelcast.newHazelcastInstance(config);
    }
    
    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}
```

```java
// infrastructure/.../config/ObservabilityConfig.java
package com.bank.consent.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
    
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }
    
    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }
    
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
    
    @Bean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }
    
    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics() {
        return new FileDescriptorMetrics();
    }
}
```

```java
// infrastructure/.../config/ApplicationConfig.java
package com.bank.consent.infrastructure.config;

import com.bank.consent.application.port.in.GetConsentUseCase;
import com.bank.consent.application.port.out.LoadConsentPort;
import com.bank.consent.application.service.GetConsentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    
    @Bean
    public GetConsentUseCase getConsentUseCase(LoadConsentPort loadConsentPort) {
        return new GetConsentService(loadConsentPort);
    }
}
```

### Main Application com Elastic APM attach

```java
// infrastructure/.../ConsentApiApplication.java
package com.bank.consent.infrastructure;

import co.elastic.apm.attach.ElasticApmAttacher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.bank.consent")
@EnableMongoRepositories(basePackages = "com.bank.consent.infrastructure.adapter.out.persistence.repository")
@EntityScan(basePackages = "com.bank.consent.infrastructure.adapter.out.persistence.entity")
public class ConsentApiApplication {
    
    public static void main(String[] args) {
        // Attach Elastic APM agent programmatically
        ElasticApmAttacher.attach();
        
        SpringApplication.run(ConsentApiApplication.class, args);
    }
}
```

## Dockerfile otimizado para Java 21 com Virtual Threads

```dockerfile
# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/

# Download dependencies
RUN apk add --no-cache maven && mvn dependency:go-offline

# Copy source
COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Download Elastic APM Agent
ADD https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=co.elastic.apm&a=elastic-apm-agent&v=LATEST \
    /app/elastic-apm-agent.jar

# Copy application
COPY --from=builder /app/infrastructure/target/infrastructure-1.0.0.jar /app/app.jar

# Environment variables
ENV JAVA_OPTS="-XX:+UseZGC \
    -XX:+UseStringDeduplication \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom"

ENV ELASTIC_APM_SERVICE_NAME="open-finance-consents-api"
ENV ELASTIC_APM_ENVIRONMENT="production"
ENV ELASTIC_APM_LOG_LEVEL="INFO"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Expose ports
EXPOSE 8080 8081

# Run
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
```

## Docker Compose completo com MongoDB, Hazelcast e Elastic Stack

```yaml
version: '3.8'

services:
  # Consent API Application
  consent-api:
    build: .
    container_name: consent-api
    ports:
      - "8080:8080"
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MONGO_PASSWORD=${MONGO_PASSWORD:-secret}
      - ELASTIC_APM_SERVER_URL=http://apm-server:8200
      - ELASTIC_APM_SECRET_TOKEN=${APM_SECRET_TOKEN}
    depends_on:
      - mongo1
      - mongo2
      - mongo3
      - apm-server
    networks:
      - consent-network
    restart: unless-stopped
  
  # MongoDB Replica Set
  mongo1:
    image: mongo:7.0
    container_name: mongo1
    command: ["--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: mongouser
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-secret}
    volumes:
      - mongo1-data:/data/db
    networks:
      - consent-network
  
  mongo2:
    image: mongo:7.0
    container_name: mongo2
    command: ["--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "27018:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: mongouser
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-secret}
    volumes:
      - mongo2-data:/data/db
    networks:
      - consent-network
  
  mongo3:
    image: mongo:7.0
    container_name: mongo3
    command: ["--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "27019:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: mongouser
      MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:-secret}
    volumes:
      - mongo3-data:/data/db
    networks:
      - consent-network
  
  # MongoDB Replica Set Initialization
  mongo-init:
    image: mongo:7.0
    container_name: mongo-init
    depends_on:
      - mongo1
      - mongo2
      - mongo3
    networks:
      - consent-network
    entrypoint: |
      bash -c "
      sleep 10
      mongosh --host mongo1:27017 -u mongouser -p ${MONGO_PASSWORD:-secret} --eval '
      rs.initiate({
        _id: \"rs0\",
        members: [
          {_id: 0, host: \"mongo1:27017\"},
          {_id: 1, host: \"mongo2:27017\"},
          {_id: 2, host: \"mongo3:27017\"}
        ]
      })'
      "
  
  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.16.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - consent-network
  
  # APM Server
  apm-server:
    image: docker.elastic.co/apm/apm-server:8.16.0
    container_name: apm-server
    ports:
      - "8200:8200"
    environment:
      - output.elasticsearch.hosts=["elasticsearch:9200"]
      - apm-server.rum.enabled=true
    depends_on:
      - elasticsearch
    networks:
      - consent-network
  
  # Kibana
  kibana:
    image: docker.elastic.co/kibana/kibana:8.16.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - consent-network

volumes:
  mongo1-data:
  mongo2-data:
  mongo3-data:
  elasticsearch-data:

networks:
  consent-network:
    driver: bridge
```

## Endpoints reativos vs imperativos - decisão técnica

### Recomendação final: NÃO misturar WebFlux e MVC

**Spring Boot não suporta executar WebFlux e Web MVC simultaneamente** na mesma aplicação. Quando ambos starters estão presentes, Spring Boot **auto-configura apenas MVC**, ignorando WebFlux. Esta é uma decisão de design intencional da equipe Spring porque os frameworks têm modelos de runtime incompatíveis (Servlet bloqueante vs. Netty não-bloqueante).

**Abordagem recomendada para este projeto**: Use **Spring MVC + Virtual Threads (Java 21)**. Esta combinação oferece:
- Escalabilidade próxima a sistemas reativos (testes mostram ~45% de vitória em latência P99)
- Código imperativo simples e fácil de debugar
- Compatibilidade total com Spring Data MongoDB (JPA/JDBC bloqueantes)
- Throughput de 100% baseline vs 65% WebFlux em cenários I/O-bound
- Apenas uma linha de configuração: `spring.threads.virtual.enabled=true`

**Quando Virtual Threads não são suficientes**: Se realmente precisar de **streaming com backpressure** ou **composição assíncrona complexa**, crie um **microserviço separado** com WebFlux. Use API Gateway (Spring Cloud Gateway) como entrada única. Nunca tente forçar coexistência no mesmo processo.

## Checklist de produção e próximos passos

### Configurações críticas antes do deploy

**Segurança Open Finance Brasil**:
- [ ] Implementar mTLS com certificados ICP-Brasil (servidor + cliente)
- [ ] Configurar OAuth 2.0 FAPI-Advanced com PAR (Pushed Authorization Requests)
- [ ] Validar scopes dinâmicos formato `consent:urn:{institution}:{consentId}`
- [ ] Implementar validação JWS PS256 para assinaturas
- [ ] Configurar JWE RSA-OAEP com A256GCM para payloads
- [ ] Adicionar sanitização de campos sensíveis (CPF, CNPJ, tokens)

**Performance e resiliência**:
- [ ] Testar carga com 500+ requisições simultâneas (usar JMeter/Gatling)
- [ ] Verificar Virtual Threads com `-Djdk.tracePinnedThreads=full` em dev
- [ ] Monitorar thread pinning em produção via JFR
- [ ] Validar rate limiting TPM conforme quantidade de consentimentos ativos
- [ ] Configurar circuit breaker para falhas MongoDB (50% em 10 chamadas)
- [ ] Testar retry com backoff exponencial (3 tentativas, 2x multiplicador)

**Observabilidade**:
- [ ] Validar métricas no Prometheus (`/actuator/prometheus`)
- [ ] Criar dashboards Kibana para latência P50/P95/P99
- [ ] Configurar alertas para taxa de erro \u003e 1%
- [ ] Monitorar cache hit ratio Hazelcast (alvo \u003e 80%)
- [ ] Verificar pool MongoDB (conexões ativas vs. disponíveis)
- [ ] Adicionar distributed tracing entre microserviços

**Infraestrutura**:
- [ ] Criar índices MongoDB manualmente antes do deploy
- [ ] Configurar backup automático MongoDB (snapshot diário)
- [ ] Implementar rotação de logs (max 7 dias)
- [ ] Adicionar Kubernetes probes (liveness/readiness)
- [ ] Configurar auto-scaling baseado em CPU \u003e 70%
- [ ] Testar failover do replica set MongoDB

### Possíveis armadilhas e como evitá-las

**Virtual Threads**: Blocos `synchronized` com I/O causam thread pinning. **Solução**: Use `ReentrantLock` em vez de synchronized. Bibliotecas antigas (Apache HttpClient \u003c5.4) têm synchronized methods - atualize dependências.

**MongoDB**: Queries sem índice causam table scan em milhões de registros. **Solução**: Sempre crie índice em `consentId` (unique) e compound index para queries frequentes. Use `.explain()` para verificar uso de índices.

**Hazelcast**: Serialização incorreta gera erros em cluster distribuído. **Solução**: Certifique-se que DTOs são `Serializable` ou use serialização customizada Hazelcast. Configure `in-memory-format: BINARY` para economizar RAM.

**Rate Limiting**: Contador pode resetar em deploy, permitindo burst temporário. **Solução**: Use Hazelcast para compartilhar estado de rate limit entre instâncias. Configure `limitRefreshPeriod` alinhado com janela Open Finance (1 minuto).

**Elastic APM**: Overhead pode chegar a 5-10% CPU em sampling 100%. **Solução**: Use `transaction_sample_rate: 0.1` (10%) em produção para APIs de alto volume. Sempre sample transações com erro (automático no agent).

**Cache**: TTL muito alto causa dados inconsistentes após update. **Solução**: Configure TTL=600s (10min) para consents, com `@CacheEvict` em operações de escrita. Use cache warming para dados críticos no startup.

### Conformidade regulatória contínua

O Banco Central exige evidências de **scaling baseado em TPS** por 12 meses. Implemente logging estruturado com timestamp de cada requisição. Calcule TPS por segundo e identifique quando \u003e90% da capacidade por 10% dos segundos em 3 quinzenas. Documente incrementos de +150 TPS e tempo de adaptação (\u003c2 meses). 

APIs de Consents são **isentas de rate limiting**, mas dados de auditoria devem rastrear lifecycle completo (criação, autorização, revogação, expiração). Mantenha logs por **5 anos** conforme LGPD. Implemente rotação com archival em S3/Glacier.

Certificados ICP-Brasil expiram anualmente. Configure alertas 60 dias antes da expiração. Processo de renovação envolve Autoridade Certificadora credenciada pelo ITI. Teste procedimento de atualização de certificados sem downtime (rolling restart).

---

**Fontes consultadas**: Open Finance Brasil Developer Portal (openfinancebrasil.atlassian.net), Spring Framework Docs 3.5.6, MongoDB Java Driver 4.11, Hazelcast 5.5 Reference, Resilience4j 2.2 Docs, Elastic APM Java Agent 1.52, JEP 444 Virtual Threads, OpenAPI Generator 7.10, Manual de APIs Open Finance v4.0, BCB Instrução Normativa 134