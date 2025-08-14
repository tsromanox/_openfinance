# API de Participantes Open Finance Brasil

API desenvolvida em Java 21 com Spring Boot 3.5.3 para consultar e fornecer informações sobre os participantes do Open Finance Brasil.

## 🚀 Funcionalidades

- ✅ Consulta automática da API oficial de participantes a cada 2 horas
- ✅ Cache em memória para alta performance
- ✅ Busca de participantes por CNPJ
- ✅ Listagem de endpoints de API por participante
- ✅ Filtros por família de API (accounts, payments, etc.)
- ✅ Suporte para 10.000 requisições simultâneas
- ✅ Documentação OpenAPI/Swagger
- ✅ Health checks e métricas
- ✅ Containerização com Docker

## 🛠️ Tecnologias

- Java 21
- Spring Boot 3.5.3
- Spring WebFlux (para cliente HTTP reativo)
- Spring Cache
- SpringDoc OpenAPI
- Lombok
- Docker
- Prometheus (métricas)

## 📋 Pré-requisitos

- Java 21 ou superior
- Maven 3.8+
- Docker (opcional)

## 🔧 Instalação e Execução

### Via Maven

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/openfinance-participants-api.git
cd openfinance-participants-api

# Compile e execute
mvn clean install
mvn spring-boot:run
```

### Via Docker

```bash
# Build da imagem
docker build -t openfinance-participants-api .

# Execute o container
docker run -p 8080:8080 openfinance-participants-api
```

### Via Docker Compose

```bash
# Inicia a aplicação e Prometheus
docker-compose up -d
```

## 📚 Documentação da API

Após iniciar a aplicação, acesse:

- Swagger UI: http://localhost:8080/participants/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/participants/api-docs

## 🔍 Endpoints Principais

### 1. Buscar participante por CNPJ
```http
GET /participants/api/v1/{cnpj}
```

Exemplo:
```bash
curl "http://localhost:8080/participants/api/v1/00000000000191"
```

### 2. Buscar endpoints de API por CNPJ
```http
GET /participants/api/v1/{cnpj}/endpoints?apiFamily=accounts
```

Exemplo:
```bash
curl "http://localhost:8080/participants/api/v1/00000000000191/endpoints?apiFamily=accounts"
```

### 3. Listar todos os participantes
```http
GET /participants/api/v1
```

### 4. Listar famílias de API disponíveis
```http
GET /participants/api/v1/api-families
```

### 5. Status do cache
```http
GET /participants/api/v1/cache/status
```

### 6. Forçar atualização do cache
```http
POST /participants/api/v1/cache/refresh
```

### 7. Health check
```http
GET /participants/health
```

## 📊 Exemplos de Resposta

### Buscar participante por CNPJ
```json
{
  "cnpj": "00000000000191",
  "organisationId": "5f69111d-5cc3-43ea-ba97-30392654a505",
  "organisationName": "Banco do Brasil S.A.",
  "legalEntityName": "Banco do Brasil S.A.",
  "status": "Active",
  "apiEndpoints": {
    "accounts": [
      "https://api.bb.com.br/open-banking/accounts/v2"
    ],
    "payments": [
      "https://api.bb.com.br/open-banking/payments/v3"
    ]
  },
  "lastUpdated": "2024-01-15T10:30:00"
}
```

### Listar endpoints de API
```json
[
  {
    "apiFamily": "accounts",
    "version": "2.4.2",
    "baseUrl": "https://api.bb.com.br",
    "fullEndpoint": "https://api.bb.com.br/open-banking/accounts/v2",
    "certificationStatus": "Certified"
  }
]
```

## ⚙️ Configuração

### application.yml
```yaml
openfinance:
  participants:
    api:
      url: https://data.directory.openbankingbrasil.org.br/participants
      timeout: 30
  webclient:
    timeout: 30
    max-memory-size: 10485760

server:
  tomcat:
    threads:
      max: 500
    accept-count: 10000
    max-connections: 10000
```

## 🏗️ Arquitetura

### Cache em Memória
- Utiliza `ConcurrentHashMap` para armazenamento thread-safe
- Atualização automática a cada 2 horas via `@Scheduled`
- Suporte para atualização manual via endpoint

### Alta Concorrência
- Configurado para suportar 10.000 requisições simultâneas
- Pool de threads otimizado
- WebClient reativo para chamadas HTTP não-bloqueantes

### Estrutura do Projeto
```
src/
├── main/
│   ├── java/
│   │   └── br/com/openfinance/participants/
│   │       ├── config/         # Configurações
│   │       ├── controller/     # Controllers REST
│   │       ├── dto/           # Data Transfer Objects
│   │       ├── exception/     # Tratamento de exceções
│   │       └── service/       # Lógica de negócio
│   └── resources/
│       └── application.yml    # Configurações da aplicação
```

## 📈 Monitoramento

### Métricas Prometheus
Disponível em: http://localhost:8080/actuator/prometheus

### Health Check
```bash
curl http://localhost:8080/participants/health
```

Resposta:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00",
  "cache": {
    "totalParticipants": 150,
    "lastGlobalUpdate": "2024-01-15T10:00:00",
    "cacheHealthy": true,
    "availableApiFamilies": 12
  }
}
```

## 🧪 Testes

```bash
# Executar todos os testes
mvn test

# Executar com cobertura
mvn test jacoco:report
```

## 🔒 Segurança

- Validação de entrada para CNPJ
- Timeout configurável para chamadas HTTP
- Tratamento de exceções global
- Headers de segurança configurados

## 🤝 Contribuindo

1. Fork o projeto
2. Crie sua Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a Branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está sob a licença Apache 2.0. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 👥 Autores

- Open Finance Brasil Community

## 🙏 Agradecimentos

- [Open Finance Brasil](https://openfinancebrasil.org.br)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [SpringDoc OpenAPI](https://springdoc.org)