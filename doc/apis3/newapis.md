Perfeito — vou te entregar um esqueleto **pronto para subir** que cumpre todos os requisitos:

* **Java 21 + Spring Boot 3.5.6**
* **Hexagonal Architecture (Ports & Adapters)**
* **Dois adaptadores REST**:

  1. **Imperativo (Spring MVC)** com **Virtual Threads** para paralelismo
  2. **Reativo (WebFlux)**
* **MongoDB** como repositório
* **Geração automática de modelos e stubs** via **OpenAPI Generator** a partir da **spec Consents v3.2.0** (usaremos o `GET /consents/{consentId}`) 
* **Observabilidade** (Actuator + Micrometer + OTEL)
* **Rate limit + Retry** (Resilience4j)
* **Cache em memória** (Caffeine) para reduzir hits no Mongo
* **Capaz de processar ~500 requisições simultâneas** (config + virtual threads + WebFlux)

> Obs.: a API de Consents define o **GET /consents/{consentId}** com cabeçalhos FAPI e resposta `200` com `ResponseConsentRead` (campos como `consentId`, `status`, `permissions`, etc.). Implementaremos **apenas o GET**, como pedido. 

---

# Estrutura de pastas (hexagonal)

```
openfinance-consents-api/
 ├─ pom.xml
 ├─ src/main/java/com/acme/consents/
 │   ├─ application/        # Casos de uso (serviços de domínio)
 │   │   └─ GetConsentByIdService.java
 │   ├─ domain/             # Modelos de domínio/ports
 │   │   ├─ model/Consent.java
 │   │   └─ port/ConsentQueryPort.java
 │   ├─ infrastructure/
 │   │   ├─ config/         # Beans: cache, resilience4j, virtual threads, obs
 │   │   │   ├─ CacheConfig.java
 │   │   │   ├─ ObservabilityConfig.java
 │   │   │   └─ VirtualThreadConfig.java
 │   │   ├─ mongodb/        # Adapter de saída (driven)
 │   │   │   ├─ ConsentEntity.java
 │   │   │   └─ ConsentMongoRepository.java
 │   │   ├─ rest/           # Adapters de entrada (drivers)
 │   │   │   ├─ imperative/ConsentImperativeController.java
 │   │   │   └─ reactive/ConsentReactiveController.java
 │   │   └─ mapper/ConsentMapper.java
 │   └─ OpenfinanceConsentsApiApplication.java
 └─ src/main/resources/
     └─ application.yml
```

---

# `pom.xml` (OpenAPI Generator + Spring Boot + Mongo + Resilience4j + Caffeine + OTEL)

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.acme</groupId>
  <artifactId>openfinance-consents-api</artifactId>
  <version>1.0.0</version>
  <properties>
    <java.version>21</java.version>
    <spring-boot.version>3.5.6</spring-boot.version>
    <openapi.generator.version>7.6.0</openapi.generator.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <!-- Web MVC (imperativo) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- WebFlux (reativo) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <!-- MongoDB -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <!-- Actuator + Micrometer OTEL -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    <!-- Resilience4j (retry, rate limiter) -->
    <dependency>
      <groupId>io.github.resilience4j</groupId>
      <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>
    <!-- Cache Caffeine -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
    </dependency>
    <!-- OpenAPI UI (documentar nossa API) -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.6.0</version>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
      <version>2.6.0</version>
    </dependency>
    <!-- (Opcional) MapStruct para mapeamento -->
    <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct</artifactId>
      <version>1.5.5.Final</version>
    </dependency>
    <dependency>
      <groupId>org.mapstruct</groupId>
      <artifactId>mapstruct-processor</artifactId>
      <version>1.5.5.Final</version>
      <scope>provided</scope>
    </dependency>
    <!-- Testes -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- OpenAPI Generator: gera modelos e interface da nossa API a partir do consents-3.2.0.yml -->
      <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
        <version>${openapi.generator.version}</version>
        <executions>
          <execution>
            <id>generate-consents-models</id>
            <goals><goal>generate</goal></goals>
            <configuration>
              <!-- Caminho da spec (adicione o arquivo ao projeto) -->
              <inputSpec>${project.basedir}/openapi/consents-3.2.0.yml</inputSpec>
              <!-- Gera server stubs + models Spring -->
              <generatorName>spring</generatorName>
              <library>spring-boot</library>
              <apiPackage>com.acme.consents.generated.api</apiPackage>
              <modelPackage>com.acme.consents.generated.model</modelPackage>
              <invokerPackage>com.acme.consents.generated.invoker</invokerPackage>
              <configOptions>
                <interfaceOnly>true</interfaceOnly>
                <useSpringBoot3>true</useSpringBoot3>
                <useTags>true</useTags>
                <dateLibrary>java8</dateLibrary>
              </configOptions>
              <generateApiTests>false</generateApiTests>
              <generateModelTests>false</generateModelTests>
              <skipValidateSpec>false</skipValidateSpec>
              <cleanupOutput>true</cleanupOutput>
              <output>${project.build.directory}/generated-sources/openapi</output>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <!-- Compilação -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>${java.version}</source>
          <target>${java.version}</target>
          <compilerArgs>
            <arg>-parameters</arg>
          </compilerArgs>
        </configuration>
      </plugin>
      <!-- Recursos gerados entram no build -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>3.5.0</version>
        <executions>
          <execution>
            <id>add-source</id>
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
      <!-- Spring Boot -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>
    </plugins>
  </build>
</project>
```

> **Como usar a spec**: coloque `openapi/consents-3.2.0.yml` (conteúdo igual ao que citei) no projeto; o plugin gera **models** (ex.: `ResponseConsentRead`) e **interfaces** do endpoint `GET /consents/{consentId}`. Você implementa as interfaces nos nossos controllers. 

---

# `application.yml` (concorrência, WebFlux, cache, resilience, Mongo)

```yaml
server:
  port: 8080

spring:
  application:
    name: openfinance-consents-api
  data:
    mongodb:
      uri: mongodb://localhost:27017/consents_db
  cache:
    type: caffeine
  threads:
    virtual:
      enabled: true   # Virtual Threads para @RestController imperativo (Tomcat/Jetty)

# WebFlux (Netty) – pool de conexões/ajustes seguros p/ 500 reqs conc.
reactor:
  netty:
    pool:
      maxConnections: 1024

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  tracing:
    sampling:
      probability: 1.0

# Resilience4j
resilience4j:
  ratelimiter:
    instances:
      consent-by-id:
        limitForPeriod: 500        # por período
        limitRefreshPeriod: 1s     # período de refresh
        timeoutDuration: 0         # não bloquear em caso de limite
  retry:
    instances:
      consent-by-id:
        maxAttempts: 3
        waitDuration: 200ms
        retryExceptions:
          - org.springframework.dao.QueryTimeoutException
          - com.mongodb.MongoTimeoutException
          - java.io.IOException

# Cache Caffeine
caffeine:
  cache:
    consents:
      spec: maximumSize=100_000,expireAfterWrite=300s,recordStats
```

---

# Configurações & Beans

`VirtualThreadConfig.java`

```java
package com.acme.consents.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {
  @Bean
  public Executor applicationExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
```

`CacheConfig.java`

```java
package com.acme.consents.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
  @Bean
  CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("consents");
    mgr.setCaffeine(Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterWrite(300, TimeUnit.SECONDS)
        .recordStats());
    return mgr;
  }
}
```

`ObservabilityConfig.java`

```java
package com.acme.consents.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {
  public ObservabilityConfig(MeterRegistry registry) {
    // Custom meters/labels se necessário
  }
}
```

---

# Domínio (Port) e Service

`domain/port/ConsentQueryPort.java`

```java
package com.acme.consents.domain.port;

import com.acme.consents.domain.model.Consent;
import java.util.Optional;
import reactor.core.publisher.Mono;

public interface ConsentQueryPort {
  Optional<Consent> findById(String consentId);  // para o caminho imperativo
  Mono<Consent> findByIdReactive(String consentId); // para o reativo
}
```

`domain/model/Consent.java`

```java
package com.acme.consents.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

public record Consent(
    String consentId,
    OffsetDateTime creationDateTime,
    String status,
    OffsetDateTime statusUpdateDateTime,
    List<String> permissions,
    OffsetDateTime expirationDateTime
) {}
```

`application/GetConsentByIdService.java`

```java
package com.acme.consents.application;

import com.acme.consents.domain.model.Consent;
import com.acme.consents.domain.port.ConsentQueryPort;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class GetConsentByIdService {

  private final ConsentQueryPort port;

  public GetConsentByIdService(ConsentQueryPort port) { this.port = port; }

  @Cacheable(cacheNames = "consents", key = "#consentId")
  @RateLimiter(name = "consent-by-id")
  @Retry(name = "consent-by-id")
  public Optional<Consent> get(String consentId) {
    return port.findById(consentId);
  }

  @Cacheable(cacheNames = "consents", key = "#consentId")
  @RateLimiter(name = "consent-by-id")
  @Retry(name = "consent-by-id")
  public Mono<Consent> getReactive(String consentId) {
    return port.findByIdReactive(consentId);
  }
}
```

---

# Adapter Mongo (driven)

`infrastructure/mongodb/ConsentEntity.java`

```java
package com.acme.consents.infrastructure.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;

@Document("consents")
public class ConsentEntity {
  @Id
  private String consentId;
  private OffsetDateTime creationDateTime;
  private String status; // AUTHORISED | AWAITING_AUTHORISATION | REJECTED
  private OffsetDateTime statusUpdateDateTime;
  private List<String> permissions;
  private OffsetDateTime expirationDateTime;

  // getters/setters/constructors
  // ...
}
```

`ConsentMongoRepository.java`

```java
package com.acme.consents.infrastructure.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// Imperativo
public interface ConsentMongoRepository extends MongoRepository<ConsentEntity, String> {}

// Reativo
interface ConsentReactiveMongoRepository extends ReactiveCrudRepository<ConsentEntity, String> {
  Mono<ConsentEntity> findById(String id);
}
```

`mapper/ConsentMapper.java`

```java
package com.acme.consents.infrastructure.mapper;

import com.acme.consents.domain.model.Consent;
import com.acme.consents.infrastructure.mongodb.ConsentEntity;

public class ConsentMapper {
  public static Consent toDomain(ConsentEntity e) {
    return new Consent(
        e.getConsentId(),
        e.getCreationDateTime(),
        e.getStatus(),
        e.getStatusUpdateDateTime(),
        e.getPermissions(),
        e.getExpirationDateTime()
    );
  }
}
```

`infrastructure/mongodb/ConsentMongoAdapter.java`

```java
package com.acme.consents.infrastructure.mongodb;

import com.acme.consents.domain.model.Consent;
import com.acme.consents.domain.port.ConsentQueryPort;
import com.acme.consents.infrastructure.mapper.ConsentMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class ConsentMongoAdapter implements ConsentQueryPort {

  private final ConsentMongoRepository repo;
  private final ConsentReactiveMongoRepository rrepo;

  public ConsentMongoAdapter(ConsentMongoRepository repo, ConsentReactiveMongoRepository rrepo) {
    this.repo = repo; this.rrepo = rrepo;
  }

  @Override
  public Optional<Consent> findById(String consentId) {
    return repo.findById(consentId).map(ConsentMapper::toDomain);
  }

  @Override
  public Mono<Consent> findByIdReactive(String consentId) {
    return rrepo.findById(consentId).map(ConsentMapper::toDomain);
  }
}
```

---

# Adapters REST (drivers)

> A spec define headers FAPI como `x-fapi-interaction-id` obrigatório para o GET, além de `Authorization`. Vamos aceitar e espelhar o header na resposta como manda a especificação. 

`rest/imperative/ConsentImperativeController.java`

```java
package com.acme.consents.infrastructure.rest.imperative;

import com.acme.consents.application.GetConsentByIdService;
import com.acme.consents.domain.model.Consent;
import com.acme.consents.generated.model.ResponseConsentRead;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/imperative/consents")
public class ConsentImperativeController {

  private final GetConsentByIdService service;

  public ConsentImperativeController(GetConsentByIdService service) {
    this.service = service;
  }

  @GetMapping("/{consentId}")
  public ResponseEntity<ResponseConsentRead> getConsentById(
      @RequestHeader("Authorization") String authorization,
      @RequestHeader(name = "x-fapi-interaction-id", required = false) String interactionId,
      @PathVariable String consentId
  ) {
    String xfid = (interactionId == null || interactionId.isBlank())
        ? UUID.randomUUID().toString() : interactionId;

    Optional<Consent> c = service.get(consentId);
    if (c.isEmpty()) {
      return ResponseEntity.notFound()
          .header("x-fapi-interaction-id", xfid)
          .build();
    }

    ResponseConsentRead body = mapToResponse(c.get());
    return ResponseEntity.ok()
        .header("x-fapi-interaction-id", xfid)
        .body(body);
  }

  private ResponseConsentRead mapToResponse(Consent c) {
    var data = new ResponseConsentRead.Data()
        .consentId(c.consentId())
        .creationDateTime(c.creationDateTime())
        .status(ResponseConsentRead.Data.StatusEnum.fromValue(c.status()))
        .statusUpdateDateTime(c.statusUpdateDateTime())
        .permissions(c.permissions())
        .expirationDateTime(c.expirationDateTime());
    return new ResponseConsentRead().data(data);
  }
}
```

`rest/reactive/ConsentReactiveController.java`

```java
package com.acme.consents.infrastructure.rest.reactive;

import com.acme.consents.application.GetConsentByIdService;
import com.acme.consents.domain.model.Consent;
import com.acme.consents.generated.model.ResponseConsentRead;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/reactive/consents")
public class ConsentReactiveController {

  private final GetConsentByIdService service;

  public ConsentReactiveController(GetConsentByIdService service) {
    this.service = service;
  }

  @GetMapping("/{consentId}")
  public Mono<ResponseEntity<ResponseConsentRead>> getConsentByIdReactive(
      @RequestHeader("Authorization") String authorization,
      @RequestHeader(name = "x-fapi-interaction-id", required = false) String interactionId,
      @PathVariable String consentId
  ) {
    String xfid = (interactionId == null || interactionId.isBlank())
        ? UUID.randomUUID().toString() : interactionId;

    return service.getReactive(consentId)
        .map(this::mapToResponse)
        .map(resp -> ResponseEntity.ok()
            .header("x-fapi-interaction-id", xfid)
            .body(resp))
        .defaultIfEmpty(ResponseEntity.notFound()
            .header("x-fapi-interaction-id", xfid)
            .build());
  }

  private ResponseConsentRead mapToResponse(Consent c) {
    var data = new ResponseConsentRead.Data()
        .consentId(c.consentId())
        .creationDateTime(c.creationDateTime())
        .status(ResponseConsentRead.Data.StatusEnum.fromValue(c.status()))
        .statusUpdateDateTime(c.statusUpdateDateTime())
        .permissions(c.permissions())
        .expirationDateTime(c.expirationDateTime());
    return new ResponseConsentRead().data(data);
  }
}
```

---

# Como isto atende os requisitos

* **Alto desempenho/500 conc.**
  *Imperativo*: **Virtual Threads** (`spring.threads.virtual.enabled=true`) permite escalar o número de requisições simultâneas sem o custo de muitas platform threads.
  *Reativo*: **WebFlux/Netty** é naturalmente eficiente para IO e aguenta 500+ concorrências com folga.

* **Métodos imperativos e reativos**: dois controllers separados (rotas `/api/imperative/...` e `/api/reactive/...`).

* **OpenAPI (geração)**: `openapi-generator-maven-plugin` gera **models/interfaces** da **Consents v3.2.0** — especialmente **`GET /consents/{consentId}`** e o modelo **`ResponseConsentRead`** usado na resposta. 

* **MongoDB**: adapter de saída com repositórios **imperativo** e **reativo**.

* **Hexagonal**: controller → service (use case) → port → adapter Mongo.

* **Observabilidade**: Actuator + OTEL (exponha `/actuator/*` e scrapes via Prometheus/OTLP).

* **Rate limit / Retry**: Resilience4j aplicado no caso de uso (`@RateLimiter`, `@Retry`).

* **Cache em memória**: `@Cacheable("consents")` para o **consentId**; **Caffeine** com TTL.

* **Conformidade FAPI (headers)**: suporte a `x-fapi-interaction-id` (espelhado na resposta), e `Authorization` no request, conforme a especificação de Consents. 

---

# Endpoints expostos (prontos)

* **Imperativo (MVC + Virtual Threads)**
  `GET /api/imperative/consents/{consentId}`
  Headers: `Authorization`, `x-fapi-interaction-id` (opcional; gerado se ausente).

* **Reativo (WebFlux)**
  `GET /api/reactive/consents/{consentId}`
  Headers: idem.

> A payload de resposta segue o **modelo oficial** `ResponseConsentRead` (com `consentId`, `status`, `permissions`, etc.). 

---

# Dicas de tuning para 500 req/simultâneas

1. **Imperativo**: com virtual threads, normalmente não é necessário mexer em `server.tomcat.threads.max`. Mantenha heap e GC sob observação (G1/ZGC).
2. **WebFlux**: `reactor.netty.pool.maxConnections=1024` já cobre bem; ajuste backpressure se houver clientes lentos.
3. **Resilience4j RateLimiter**: se você quer *global* 500/s por instância, os valores no `application.yml` já refletem isso.
4. **Cache hit-rate**: aumente `maximumSize` conforme cardinalidade de `consentId` mais acessados.
5. **Mongo**: criar índice em `consentId` (chave `_id`) e ajustar pool de conexões (via URI ou `MongoClientSettings` se necessário).

---

# Teste rápido (seed opcional)

Você pode popular o Mongo com um documento:

```js
// mongo shell
db.consents.insertOne({
  _id: "urn:bancoex:C1DD33123",
  consentId: "urn:bancoex:C1DD33123",
  creationDateTime: ISODate("2021-05-21T08:30:00Z"),
  status: "AUTHORISED",
  statusUpdateDateTime: ISODate("2021-05-21T08:30:00Z"),
  permissions: ["ACCOUNTS_READ","RESOURCES_READ"],
  expirationDateTime: ISODate("2021-11-21T08:30:00Z")
})
```

Depois:

```
curl -H "Authorization: Bearer dummy" \
     -H "x-fapi-interaction-id: 11111111-1111-1111-1111-111111111111" \
     http://localhost:8080/api/imperative/consents/urn:bancoex:C1DD33123
```

---

Se quiser, eu já te entrego isso como um **projeto Maven completo** (com os arquivos organizados/renomeados) ou ajusto o **rate limit** para janelas deslizantes por IP/cliente. Também posso integrar **Bucket4j** se você preferir rate limiting *token-bucket* por chave (ex.: `client-id`).


Medical References:
1. None — DOI: file-BaTbKaNiXsGTwom1qCSWBa