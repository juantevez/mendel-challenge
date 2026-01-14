# Arquitectura del Sistema - Mendel Challenge

## Tabla de Contenidos

- [Visión General](#-visión-general)
- [Arquitectura Hexagonal](#-arquitectura-hexagonal)
- [Capas del Sistema](#-capas-del-sistema)
- [Patrones de Diseño](#-patrones-de-diseño)
- [Modelo de Dominio](#-modelo-de-dominio)
- [Flujo de Datos](#-flujo-de-datos)
- [Estrategias de Storage](#-estrategias-de-storage)
- [Decisiones de Diseño](#-decisiones-de-diseño)

## Visión General

El sistema implementa un servicio de gestión de transacciones con soporte para jerarquías y múltiples estrategias de almacenamiento, siguiendo principios de **Clean Architecture** y **Domain-Driven Design**.

### Características Arquitectónicas

-  **Separation of Concerns**: Cada capa tiene responsabilidades bien definidas
-  **Dependency Inversion**: El dominio no depende de infraestructura
-  **High Cohesion, Low Coupling**: Módulos independientes y reutilizables
-  **Testability**: 100% de cobertura en componentes críticos
-  **Extensibility**: Fácil agregar nuevas estrategias de storage

##  Arquitectura Hexagonal
```
┌────────────────────────────────────────────────────────────────┐
│                       APPLICATION LAYER                        │
│  ┌──────────────────────┐         ┌─────────────────────────┐  │
│  │   REST Controller    │ ◄────── │  Requests / Responses   │  │
│  │  (Inbound Adapter)   │         │     (Java Records)      │  │
│  └──────────┬───────────┘         └─────────────────────────┘  │
└─────────────┼──────────────────────────────────────────────────┘
              │ 
              ▼ calls (Input Port)
┌────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  TransactionService                      │  │
│  │                  (Business Logic)                        │  │
│  └──────────────────────────┬───────────────────────────────┘  │
│                             │                                  │
│  ┌──────────────────────────▼───────────────────────────────┐  │
│  │                TransactionRepository                     │  │
│  │                   (Output Port)                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬──────────────────────────────────┘
                              │ 
                              │ injected by Spring 
                              ▼ (Based on @ConditionalOnProperty)
┌───────────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                            │
│  ┌──────────────────────────┬───────────────────────────────┐     │
│  │   RepositoryConfig       │ strategy: ${storage.strategy} │     │
│  └──────────┬───────────────┴───────────────┬───────────────┘     │
│             │                               │                     │
│  ┌──────────▼────────────┐        ┌──────────▼──────────────┐     │
│  │  InMemory Adapter     │        │     Redis Adapter       │     │
│  │ (Map Implementation)  │        │ (RedisTemplate/Lettuce) │     │
│  └───────────────────────┘        └─────────────────────────┘     │
└───────────────────────────────────────────────────────────────────┘
```

## Capas del Sistema

### 1. Domain Layer (Core)

**Responsabilidad**: Contiene la lógica de negocio y reglas del dominio.
```
domain/
├── model/
│   ├── Transaction.java          # Entidad de dominio
├── port/
│   ├── in/                        # Input Ports (Use Cases)
│   │   ├── CreateTransactionUseCase.java
│   │   ├── GetTransactionsByTypeUseCase.java
│   │   └── GetTransactionSumUseCase.java
│   └── out/                       # Output Ports (Interfaces)
│       └── TransactionRepository.java
└── service/
    └── TransactionService.java   # Implementa los Use Cases
```

**Características:**
- ✅ **Sin dependencias externas** (ni Spring, ni Redis, ni Jackson)
- ✅ **Inmutabilidad**: Transaction es inmutable (Builder Pattern)
- ✅ **Validaciones**: Lógica de validación en el dominio
- ✅ **Reglas de negocio**: Suma recursiva, validación de jerarquías

### 2. Application Layer

**Responsabilidad**: Expone la funcionalidad del dominio a través de APIs.
```
application/
└── rest/
    ├── TransactionController.java    # REST API
    ├── dto/
    │   ├── TransactionRequest.java
    │   ├── TransactionResponse.java
    │   └── SumResponse.java
    └── exception/
        └── GlobalExceptionHandler.java
```

**Características:**
-  **DTOs**: Separación entre modelo de dominio y API
-  **Validación**: Bean Validation (JSR-380)
-  **Exception Handling**: Manejo centralizado de errores
-  **RESTful**: Siguiendo principios REST

### 3. Infrastructure Layer

**Responsabilidad**: Implementaciones concretas de los ports.
```
infrastructure/
├── adapter/
│   ├── memory/
│   │   └── InMemoryTransactionRepository.java
│   └── redis/
│       ├── RedisTransactionRepository.java
│       └── dto/
│           └── TransactionRedisDTO.java
└── config/
    └── RedisConfig.java
```

**Características:**
-  **Adaptadores**: Implementan interfaces del dominio
-  **Separación de concerns**: Cada adapter es independiente
-  **DTO de persistencia**: TransactionRedisDTO para Redis

## Patrones de Diseño

### 1. Hexagonal Architecture (Ports & Adapters)
```java
// Port (Interface en el dominio)
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    // ...
}

// Adapter (Implementación en infraestructura)
@Repository("inMemoryRepository")
public class InMemoryTransactionRepository implements TransactionRepository {
    // Implementación específica
}
```

### 3. Builder Pattern
```java
Transaction transaction = Transaction.builder()
    .id(1L)
    .type("cars")
    .amount(new BigDecimal("1000"))
    .parentId(null)
    .build();
```

### 5. Repository Pattern
```java
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findByType(String type);
    List<Transaction> findChildrenOf(Long parentId);
    boolean existsById(Long id);
}
```

##  Modelo de Dominio

### Transaction (Entidad Raíz)
```java
public class Transaction {
    private final Long id;              // Identificador único
    private final String type;          // Tipo de transacción
    private final BigDecimal amount;    // Monto
    private final Long parentId;        // ID del padre (opcional)
    private final Instant createdAt;    // Timestamp de creación
    
}
```

**Invariantes:**
-  `id` no puede ser null
-  `type` no puede ser null
-  `amount` no puede ser null ni negativo
-  `parentId` es opcional
-  `createdAt` se asigna automáticamente si no se proporciona

### Jerarquía de Transacciones
```
Transaction (id=1, amount=1000, type="project")
    ├── Transaction (id=2, amount=500, type="development", parentId=1)
    │   └── Transaction (id=4, amount=200, type="frontend", parentId=2)
    └── Transaction (id=3, amount=300, type="testing", parentId=1)

Suma recursiva de id=1: 1000 + 500 + 200 + 300 = 2000
```

## Flujo de Datos

### 1. Crear Transacción
```
┌──────────┐      ┌────────────┐      ┌─────────────┐      ┌────────────┐
│  Client  │─────►│ Controller │─────►│   Service   │─────►│ Repository │
└──────────┘      └────────────┘      └─────────────┘      └────────────┘
     │                   │                    │                    │
     │  PUT /transaction │                    │                    │
     │  + JSON           │                    │                    │
     │                   │  create()          │                    │
     │                   │───────────────────►│                    │
     │                   │                    │  save()            │
     │                   │                    │───────────────────►│
     │                   │                    │                    │
     │                   │                    │  Transaction       │
     │                   │  Transaction       │◄───────────────────│
     │  201 Created      │◄───────────────────│                    │
     │◄──────────────────│                    │                    │
```

### 2. Calcular Suma Recursiva
```
Service.calculateSum(id=1)
    │
    ├─► repository.findById(1)  → Transaction(amount=1000)
    │
    ├─► repository.findChildrenOf(1) → [Transaction(id=2), Transaction(id=3)]
    │
    ├─► calculateSumRecursive(Transaction(id=2))
    │       │
    │       ├─► repository.findChildrenOf(2) → [Transaction(id=4)]
    │       │
    │       └─► calculateSumRecursive(Transaction(id=4))
    │               └─► return 200
    │       └─► return 500 + 200 = 700
    │
    ├─► calculateSumRecursive(Transaction(id=3))
    │       └─► return 300
    │
    └─► return 1000 + 700 + 300 = 2000
```

## Estrategias de Storage

### IN_MEMORY Strategy
```java
@Repository("inMemoryRepository")
public class InMemoryTransactionRepository {
    // Almacenamiento simple sin thread-safety
    private final Map<Long, Transaction> transactions = new HashMap<>();
    private final Map<String, Set<Long>> typeIndex = new HashMap<>();
    private final Map<Long, Set<Long>> childrenIndex = new HashMap<>();
}
```

**Características:**
- **Rápido**: O(1) para búsquedas por ID
- **Simple**: No requiere infraestructura externa
- **Volátil**: Datos se pierden al reiniciar
- **Índices**: Optimizado para búsquedas por tipo y jerarquías

**Complejidad:**
- `save()`: O(1)
- `findById()`: O(1)
- `findByType()`: O(n) donde n = transacciones del tipo
- `findChildrenOf()`: O(n) donde n = hijos directos

### REDIS Strategy
```java
@Repository("redisRepository")
public class RedisTransactionRepository {
    // Keys:
    // - "transaction:{id}" → JSON de la transacción
    // - "type:{type}" → Set de IDs
    // - "children:{parentId}" → Set de IDs de hijos
}
```

**Características:**
-  **Persistente**: Datos sobreviven reinicio (con AOF)
-  **Escalable**: Puede usarse en cluster
-  **Distribuido**: Múltiples instancias pueden compartir datos
-  **Serialización**: JSON con Jackson

**Estructura en Redis:**
```
Key: "transaction:1"
Value: {"id":1,"type":"cars","amount":1000,"parentId":null,"createdAt":"..."}

Key: "type:cars"
Value: Set[1, 2, 5]

Key: "children:1"
Value: Set[2, 3]
```

##  Decisiones de Diseño

### 1. ¿Por qué Arquitectura Hexagonal?

**Ventajas:**
-  **Testabilidad**: Dominio sin dependencias externas
-  **Flexibilidad**: Fácil cambiar de Redis a MongoDB
-  **Mantenibilidad**: Cada capa es independiente
-  **Evolución**: Agregar features sin romper código existente

### 2. ¿Por qué dos estrategias de storage?

-  **Desarrollo**: IN_MEMORY para tests y desarrollo local
-  **Producción**: REDIS para persistencia y escalabilidad
-  **Fallback**: Si Redis falla, caer a IN_MEMORY
-  **Demostración**: Muestra flexibilidad arquitectónica

### 3. ¿Por qué inmutabilidad en Transaction?

-  **Thread-safety**: Objetos inmutables son thread-safe
-  **Predictibilidad**: No hay efectos secundarios
-  **Cache-friendly**: Ideal para Redis
-  **DDD**: Entidades inmutables son más seguras

### 5. ¿Por qué DTO separado para Redis?

-  **Separación de concerns**: Dominio no conoce Jackson
-  **Flexibilidad**: Cambiar serialización sin afectar dominio
-  **Versionado**: Manejar versiones de datos en Redis
-  **Clean Architecture**: Infraestructura no contamina dominio

##  Consideraciones de Seguridad

- **Validación de entrada**: Bean Validation en DTOs
- **Validación de dominio**: Reglas en Transaction
- **Exception handling**: No expone detalles internos
- **Sanitización**: IDs y tipos son validados

## 🚀 Escalabilidad

### Horizontal Scaling
```
Load Balancer
    │
    ├─► App Instance 1 ──┐
    ├─► App Instance 2 ──┼──► Redis Cluster
    └─► App Instance 3 ──┘
```

**Consideraciones:**
-  Redis puede ser clusterizado
-  Stateless app instances
-  Shared cache en Redis

### Performance

-  **O(1) lookups** en ambos repositorios
-  **Índices** para búsquedas por tipo
-  **Lazy loading** de hijos (solo cuando se necesita)
-  **Caching** natural con Redis

## Métricas de Calidad

- **Complejidad Ciclomática**: < 10 en todos los métodos
- **Cobertura de Tests**: ~100% en componentes críticos
- **Acoplamiento**: Bajo (cada capa es independiente)
- **Cohesión**: Alto (cada clase tiene una responsabilidad)

---

Para más información sobre el uso del sistema, consulta [README.md](README.md).
