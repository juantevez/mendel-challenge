# Mendel Challenge - Transaction Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Servicio RESTful de transacciones implementado con **arquitectura hexagonal**, **DDD** y principios **SOLID**. Soporta almacenamiento en memoria y Redis con capacidad de cambio dinámico entre estrategias.

## Características

-  **Arquitectura Hexagonal** (Ports & Adapters)
-  **Domain-Driven Design (DDD)**
-  **Principios SOLID**
-  **Dual Storage**: IN_MEMORY y REDIS
-  **Jerarquías de transacciones** (relaciones padre-hijo)
-  **Suma recursiva** de transacciones anidadas
-  **API RESTful** con Spring Boot
-  **Tests unitarios** con ~100% de cobertura
-  **Docker** ready

##  Tabla de Contenidos

- [Inicio Rápido](#-inicio-rápido)
- [Prerequisitos](#-prerequisitos)
- [Instalación](#-instalación)
- [Uso](#-uso)
- [API Endpoints](#-api-endpoints)
- [Ejemplos](#-ejemplos)
- [Testing](#-testing)
- [Docker](#-docker)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Contribuir](#-contribuir)

## Inicio Rápido

### Opción 1: Docker (Recomendado)
```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/mendel-challenge.git
cd mendel-challenge

# Levantar con Docker
make up

# O usando docker-compose directamente
docker-compose up -d
```

La aplicación estará disponible en: `http://localhost:8080`

### Opción 2: Ejecución Local
```bash
# Clonar el repositorio
git clone https://github.com/juantevez/mendel-challenge.git
cd mendel-challenge

# Compilar
./mvnw clean package

# Ejecutar
./mvnw spring-boot:run
```

##  Prerequisitos

### Para ejecución local:
- Java 17 o superior
- Maven 3.8+
- (Opcional) Redis 7+ para storage persistente en memoria

### Para Docker:
- Docker 20.10+
- Docker Compose 2.0+

## Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/juantevez/mendel-challenge.git
cd mendel-challenge
```

### 2. Configuración (Opcional)

Editar `src/main/resources/application.yml` si es necesario:
```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: localhost
      port: 6379

redis:
  enabled: true  # false para solo usar IN_MEMORY (con HashMap)
```

### 3. Compilar
```bash
./mvnw clean package
```

## Uso

### Levantar solo Redis (para desarrollo local)
```bash
make dev-up
```

### Ejecutar la aplicación
```bash
./mvnw spring-boot:run
```

### Verificar que está funcionando
```bash
curl http://localhost:8080/actuator/health
```

## API Endpoints

### Importar a Postman u otra herramienta de Servicios Web 

### ``` src/main/resources/postman/mendel-challenge-postman-collection.json ``` 


## Docker

### Comandos disponibles
```bash
# Ver todos los comandos
make help

# Construir imágenes
make build

# Levantar servicios
make up

# Ver logs
make logs
make logs-app
make logs-redis

# Detener servicios
make down

# Limpiar todo
make clean

# Solo Redis para desarrollo
make dev-up
```

### Docker Compose
```bash
# Levantar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Detener
docker-compose down

# Limpiar volúmenes
docker-compose down -v
```

### Acceder a Redis
```bash
# Ejecutar redis-cli
docker exec -it mendel-redis redis-cli

# Ver todas las keys
docker exec -it mendel-redis redis-cli KEYS "*"

# Obtener un valor
docker exec -it mendel-redis redis-cli GET "transaction:1"
```

##  Arquitectura

Para información detallada sobre la arquitectura del proyecto, consulta [ARCHITECTURE.md](ARCHITECTURE.md).

**Resumen:**
- **Domain Layer**: Entidades, ports y lógica de negocio
- **Application Layer**: Controllers y DTOs
- **Infrastructure Layer**: Implementaciones de repositorios (IN_MEMORY, REDIS)

##  Tecnologías

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data Redis**
- **Jackson** (JSON serialization)
- **Maven**
- **JUnit 5**
- **Mockito**
- **AssertJ**
- **Docker & Docker Compose**
- **Redis 7**

##  Estructura del Proyecto
```
mendel-challenge/
├── src/
│   ├── main/
│   │   ├── java/com/mendel/challenge/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   ├── port/
│   │   │   │   └── service/
│   │   │   ├── application/
│   │   │   │   └── rest/
│   │   │   └── infrastructure/
│   │   │       ├── adapter/
│   │   │       └── factory/
│   │   └── resources/
│   └── test/
├── docker-compose.yml
├── Dockerfile
├── Makefile
├── pom.xml
├── README.md
└── ARCHITECTURE.md
```



## Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para más detalles.

## 👤 Autor

**Juan Tevez**

- GitHub: [@juantevez](https://github.com/juantevez)
- LinkedIn: [@Juan-Tevez](https://www.linkedin.com/in/juan-tevez-100/)

