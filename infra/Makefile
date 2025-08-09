.PHONY: help up down logs clean

help:
	@echo "Comandos disponíveis:"
	@echo "  make up       - Inicia todos os serviços"
	@echo "  make down     - Para todos os serviços"
	@echo "  make logs     - Mostra logs dos serviços"
	@echo "  make clean    - Remove volumes e containers"
	@echo "  make kafka-ui - Abre Kafka UI no browser"
	@echo "  make grafana  - Abre Grafana no browser"

up:
	docker-compose up -d
	@echo "Aguardando serviços iniciarem..."
	@sleep 10
	@echo "✅ Serviços iniciados!"
	@echo "📊 Kafka UI: http://localhost:8090"
	@echo "📈 Grafana: http://localhost:3000 (admin/admin)"
	@echo "🗄️ Cosmos Emulator: https://localhost:8081/_explorer/index.html"

down:
	docker-compose down

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	@echo "✅ Volumes e containers removidos"

kafka-ui:
	open http://localhost:8090 || xdg-open http://localhost:8090

grafana:
	open http://localhost:3000 || xdg-open http://localhost:3000

# Comandos específicos
kafka-topics:
	docker exec openfinance-kafka kafka-topics --list --bootstrap-server localhost:9092

redis-cli:
	docker exec -it openfinance-redis redis-cli

postgres-cli:
	docker exec -it openfinance-postgres psql -U openfinance -d openfinance