.PHONY: help build up down logs clean test restart dev-up dev-down

help: ## Mostrar ayuda
	@echo "Comandos disponibles:"
	@echo "  make build      - Construir imagen Docker"
	@echo "  make up         - Levantar servicios"
	@echo "  make down       - Detener servicios"
	@echo "  make logs       - Ver logs"
	@echo "  make clean      - Limpiar volúmenes y contenedores"
	@echo "  make test       - Ejecutar tests"
	@echo "  make restart    - Reiniciar servicios"
	@echo "  make dev-up     - Levantar solo Redis para desarrollo"
	@echo "  make dev-down   - Detener Redis de desarrollo"

build: ## Construir la imagen Docker
	docker-compose build

up: ## Levantar todos los servicios
	docker-compose up -d
	@echo "Aplicación disponible en http://localhost:8080"
	@echo "Redis disponible en localhost:6379"

down: ## Detener todos los servicios
	docker-compose down

logs: ## Ver logs de todos los servicios
	docker-compose logs -f

logs-app: ## Ver logs solo de la aplicación
	docker-compose logs -f app

logs-redis: ## Ver logs solo de Redis
	docker-compose logs -f redis

clean: ## Limpiar todo (contenedores, volúmenes, imágenes)
	docker-compose down -v
	docker system prune -f

test: ## Ejecutar tests
	./mvnw clean test

restart: down up ## Reiniciar servicios

dev-up: ## Levantar solo Redis para desarrollo local
	docker-compose -f docker-compose.dev.yml up -d
	@echo "Redis disponible en localhost:6379"
	@echo "Redis Commander UI en http://localhost:8081"

dev-down: ## Detener Redis de desarrollo
	docker-compose -f docker-compose.dev.yml down

dev-clean: ## Limpiar Redis de desarrollo
	docker-compose -f docker-compose.dev.yml down -v

health: ## Verificar health de los servicios
	@echo "Verificando health de la aplicación..."
	@curl -s http://localhost:8080/actuator/health | jq .
