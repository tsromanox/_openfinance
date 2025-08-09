# OpenFinance Core Library

Biblioteca compartilhada contendo configurações e utilitários comuns para os microserviços do Open Finance Brasil.

## 📦 Estrutura do Projeto

```
openfinance-core/
├── src/
│   └── main/
│       └── java/
│           └── br/
│               └── com/
│                   └── openfinance/
│                       └── core/
│                           ├── client/
│                           │   └── BaseOpenFinanceClient.java
│                           ├── config/
│                           │   ├── CoreConfiguration.java
│                           │   ├── KafkaConfiguration.java
│                           │   ├── RedisConfiguration.java
│                           │   └── WebClientConfiguration.java
│                           ├── kafka/
│                           │   └── ReactiveKafkaConsumerTemplate.java
│                           ├── metrics/
│                           │   └── MetricsService.java
│                           ├── security/
│                           │   ├── OAuth2Configuration.java
│                           │   └── SecurityUtils.java
│                           └── util/
│                               ├── DateTimeUtils.java
│                               └── JsonUtils.java
├── pom.xml
└── README.md
```

## 🚀 Como Usar

### 1. Instalação Local

```bash
cd openfinance-core
mvn clean install
```

### 2. Adicionar como Dependência

```xml
<dependency>
    <groupId>br.com.openfinance</groupId>
    <artifactId>openfinance-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Importar Configurações

No seu microserviço, importe as configurações necessárias:

```java
@SpringBootApplication
@Import({
    CoreConfiguration.class,
    KafkaConfiguration.class,
    RedisConfiguration.class,
    WebClientConfiguration.class
})
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

## 📋 Configurações Disponíveis

### 1. **CoreConfiguration**
- `ObjectMapper`: Configurado para Java Time e enums
- `MeterRegistry`: Para métricas
- `virtualThreadExecutor`: Executor com Virtual Threads (Java 21)
- `customForkJoinPool`: Pool otimizado para paralelismo

### 2. **KafkaConfiguration**
- Templates reativos e imperativos
- Configuração de producer e consumer
- Serialização JSON automática
- Idempotência e compressão habilitadas

### 3. **RedisConfiguration**
- Templates reativos e imperativos
- Pool de conexões Lettuce
- Serialização JSON
- Cache Manager configurado

### 4. **WebClientConfiguration**
- Pool de conexões otimizado
- Timeouts configuráveis
- Logging de requests/responses
- Suporte para SSL customizado

## ⚙️ Propriedades de Configuração

### Kafka
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: openfinance-group
```

### Redis
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ""
    timeout: 2000
    lettuce:
      pool:
        max-active: 100
        max-idle: 50
        min-idle: 20
```

### WebClient
```yaml
openfinance:
  webclient:
    timeout: 30
    max-in-memory-size: 10485760
    max-connections: 500
    max-pending-acquires: 1000
    ssl:
      trust-all: false
```

## 🔧 Classes Utilitárias

### BaseOpenFinanceClient
Classe base para implementar clientes de API:

```java
public class YourApiClient extends BaseOpenFinanceClient {
    public YourApiClient(WebClient.Builder webClientBuilder) {
        super(webClientBuilder);
    }
    
    public Mono<Response> callApi(String token) {
        return webClient.get()
            .uri("/endpoint")
            .headers(h -> addCommonHeaders(h, token))
            .retrieve()
            .bodyToMono(Response.class);
    }
}
```

### ReactiveKafkaConsumerTemplate
Wrapper para consumidor reativo Kafka:

```java
@Component
public class YourConsumer {
    private final ReactiveKafkaConsumerTemplate<String, YourEvent> consumer;
    
    @PostConstruct
    public void startConsuming() {
        consumer.receive()
            .doOnNext(record -> processRecord(record))
            .subscribe();
    }
}
```

## 🏗️ Próximas Adições Planejadas

1. **Security Module**
    - OAuth2 client configuration
    - mTLS support
    - Token management

2. **Observability Module**
    - Distributed tracing
    - Structured logging
    - Custom metrics

3. **Database Module**
    - Cosmos DB configuration
    - MongoDB configuration
    - Common repository patterns

4. **Error Handling Module**
    - Global exception handlers
    - Error response builders
    - Circuit breaker patterns

## 📝 Versionamento

- **1.0.0**: Versão inicial com configurações básicas
- **1.1.0**: (Planejado) Adicionar módulo de segurança
- **1.2.0**: (Planejado) Adicionar módulo de observabilidade

## 🤝 Contribuindo

1. Faça um fork do projeto
2. Crie uma feature branch (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença Apache 2.0.