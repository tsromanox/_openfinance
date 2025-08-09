#!/bin/bash
echo "🚀 Iniciando ambiente local Open Finance..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando. Por favor, inicie o Docker Desktop."
    exit 1
fi

# Criar arquivo .env se não existir
if [ ! -f .env ]; then
    echo "📝 Criando arquivo .env..."
    cp .env.example .env
fi

# Criar diretórios necessários
mkdir -p config/grafana/provisioning/{dashboards,datasources}
mkdir -p scripts

# Iniciar serviços
echo "🐳 Iniciando containers..."
docker-compose up -d

# Aguardar serviços ficarem prontos
echo "⏳ Aguardando serviços iniciarem..."
sleep 10

# Verificar status dos serviços
echo "🔍 Verificando status dos serviços..."
docker-compose ps

# Criar tópicos Kafka
echo "📨 Criando tópicos Kafka..."
docker exec openfinance-kafka kafka-topics --create \
    --topic account-updates \
    --bootstrap-server localhost:9092 \
    --partitions 10 \
    --replication-factor 1 \
    --if-not-exists

docker exec openfinance-kafka kafka-topics --create \
    --topic consent-updates \
    --bootstrap-server localhost:9092 \
    --partitions 10 \
    --replication-factor 1 \
    --if-not-exists

echo "✅ Ambiente local iniciado com sucesso!"
echo ""
echo "📊 URLs disponíveis:"
echo "  - Kafka UI: http://localhost:8090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Prometheus: http://localhost:9090"
echo "  - Cosmos Emulator: https://localhost:8081/_explorer/index.html"
echo "  - Redis: localhost:6379"
echo "  - PostgreSQL: localhost:5432"
echo ""
echo "💡 Para parar os serviços, execute: docker-compose down"
echo "💡 Para ver os logs, execute: docker-compose logs -f"