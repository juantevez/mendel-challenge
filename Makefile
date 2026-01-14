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

export JAVA_HOME := #path from java home environment variable

.PHONY: help build up down logs clean test restart dev-up dev-down

help:
	@echo "Comandos disponibles:"
    @echo "  make package    - Compilacion de la aplicacion"
	@echo "  make build      - Construir imagen Docker"
	@echo "  make up         - Levantar servicios (mendel-app y mendel-redis)"
	@echo "  make down       - Detener servicios"
	@echo "  make logs       - Ver logs"
	@echo "  make clean      - Limpiar volúmenes y contenedores"
	@echo "  make restart    - Reiniciar servicios"

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

restart: down up ## Reiniciar servicios

