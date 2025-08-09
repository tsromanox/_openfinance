Aqui um `docker-compose.yml` completo e otimizado para desenvolvimento local com todos os recursos básicos necessários:

### 🐳 Serviços Incluídos:

1. **Azure Cosmos DB Emulator**
   - Porta: 8081
   - Explorer: https://localhost:8081/_explorer/index.html
   - Chave padrão do emulador incluída

2. **Redis** (Cache)
   - Porta: 6379
   - Configurado com política LRU
   - Limite de memória: 512MB

3. **Kafka com KRaft** (Sem Zookeeper!)
   - Porta: 9092
   - Modo KRaft (mais simples e rápido)
   - UI incluída na porta 8090

4. **PostgreSQL** (Alternativa ao Cosmos DB)
   - Porta: 5432
   - Schema e tabelas pré-configurados
   - Usuário: openfinance / openfinance123

5. **Prometheus + Grafana** (Monitoramento)
   - Prometheus: 9090
   - Grafana: 3000 (admin/admin)

### 🚀 Como Usar:

```bash
# 1. Clonar o projeto e navegar até a pasta
cd openfinance-system

# 2. Copiar arquivo de ambiente
cp .env.example .env

# 3. Iniciar todos os serviços
docker-compose up -d

# 4. Verificar se está tudo rodando
docker-compose ps

# 5. Ver logs (opcional)
docker-compose logs -f
```

### 🛠️ Comandos Úteis (Makefile):

```bash
make up       # Inicia todos os serviços
make down     # Para todos os serviços
make logs     # Mostra logs
make clean    # Remove tudo (incluindo volumes)
make kafka-ui # Abre Kafka UI no browser
make grafana  # Abre Grafana no browser
```

### 📝 Configuração da Aplicação:

Para conectar sua aplicação Spring Boot aos serviços:

```yaml
# application-local.yml
spring:
  profiles:
    active: local

azure:
  cosmos:
    endpoint: https://localhost:8081
    key: C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIuFJQ==
    database: openfinance

spring:
  redis:
    host: localhost
    port: 6379
    
  kafka:
    bootstrap-servers: localhost:9092
```

### 🎯 Vantagens do Setup:

1. **Kafka com KRaft**: Não precisa mais do Zookeeper!
2. **Tudo em containers**: Nada instalado localmente
3. **Volumes persistentes**: Dados mantidos entre restarts
4. **Health checks**: Garante que serviços estão prontos
5. **UI incluída**: Kafka UI para visualizar tópicos/mensagens
6. **Monitoramento**: Prometheus + Grafana pré-configurados

### 🔧 Requisitos Mínimos:

- Docker Desktop instalado
- 8GB RAM disponível
- 10GB espaço em disco

Este setup permite desenvolvimento local completo sem precisar de recursos cloud!