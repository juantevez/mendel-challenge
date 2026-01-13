# Detectar sistema operativo
ifeq ($(OS),Windows_NT)
    MVN := mvn
    RM := del /Q
    RMDIR := rmdir /S /Q
    SEP := \\
else
    MVN := $(MVN)
    RM := rm -f
    RMDIR := rm -rf
    SEP := /
endif

export JAVA_HOME := C:\Users\juant\.jdks\corretto-17.0.17

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

package: ## Compilar la aplicación
	$(MVN) clean package -DskipTests

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
	$(MVN) clean test

test-unit: ## Ejecutar solo tests unitarios
	$(MVN) test -Dtest="!*IntegrationTest,!*PerformanceTest,!*LoadTest"

test-integration: ## Ejecutar solo tests de integración
	$(MVN) test -Dtest="*IntegrationTest"

test-e2e: ## Ejecutar solo tests E2E
	$(MVN) test -Dtest=EndToEndIntegrationTest

test-all: ## Ejecutar todos los tests
	$(MVN) clean test

test-coverage: ## Ejecutar tests unitarios con coverage (excluyendo integration y performance)
	$(MVN) clean test jacoco:report -P unit-tests
	@echo "✅ Coverage report (unit tests only): target/site/jacoco/index.html"

test-coverage-integration: ## Ejecutar tests de integración con coverage
	$(MVN) clean test jacoco:report -P integration-tests
	@echo "✅ Coverage report (integration tests): target/site/jacoco/index.html"

test-coverage-all: ## Ejecutar todos los tests con coverage (unit + integration + performance)
	$(MVN) clean test jacoco:report -P all-tests
	@echo "✅ Coverage report (all tests): target/site/jacoco/index.html"

test-coverage-fast: ## Coverage solo de unitarios sin limpiar (más rápido)
	$(MVN) test jacoco:report -P unit-tests
	@echo "✅ Coverage report: target/site/jacoco/index.html"

test-performance: ## Ejecutar tests de performance
	$(MVN) test -Dtest=TransactionRepositoryPerformanceTest

test-load: ## Ejecutar tests de carga
	$(MVN) test -Dtest=TransactionApiLoadTest

test-stress: ## Ejecutar todos los stress tests
	$(MVN) test -Dtest="*PerformanceTest,*LoadTest"

performance-report: ## Generar reporte de performance
	$(MVN) test -Dtest="*PerformanceTest,*LoadTest" surefire-report:report
	@echo "Report: target/site/surefire-report.html"

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

