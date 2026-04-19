# toscano-post1-u8

**Diseño de Algoritmos y Sistemas — Unidad 8: Diseño de Sistemas**  
Post-Contenido 1 · Ingeniería de Sistemas · UDES 2026  
Autor: Andrés Toscano

---

## Descripción

API REST para gestión de pedidos implementada con **arquitectura hexagonal** (Ports & Adapters) en Java 17.  
El núcleo del dominio no depende de ningún framework; la infraestructura se conecta desde afuera mediante interfaces.

---

## Arquitectura Hexagonal — Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────────┐
│                        MUNDO EXTERIOR                               │
│                                                                     │
│   Cliente HTTP                                 Base de datos /      │
│   (curl / Postman)                             memoria / BD real    │
└──────────┬──────────────────────────────────────────┬──────────────┘
           │  petición JSON                            │  SQL / Map
           ▼                                          ▲
┌─────────────────────┐                  ┌────────────────────────────┐
│  ADAPTADOR DE       │                  │  ADAPTADOR DE              │
│  ENTRADA (HTTP)     │                  │  SALIDA (Persistencia)     │
│                     │                  │                            │
│  OrderController    │                  │  InMemoryOrderRepository   │
│  (adapter/in/http)  │                  │  (adapter/out/persistence) │
└──────────┬──────────┘                  └────────────┬───────────────┘
           │ implementa                               │ implementa
           │ puerto de entrada                        │ puerto de salida
           ▼                                          ▲
┌══════════════════════════════════════════════════════════════════════╗
║                     HEXÁGONO (NÚCLEO)                               ║
║                                                                     ║
║  ┌────────────────────────────────────┐                            ║
║  │   PUERTOS DE ENTRADA (interfaces)  │                            ║
║  │   application/port/in/             │                            ║
║  │   ├── CreateOrderUseCase           │                            ║
║  │   ├── GetOrderUseCase              │                            ║
║  │   └── UpdateOrderStatusUseCase     │                            ║
║  └──────────────┬─────────────────────┘                            ║
║                 │ implementados por                                 ║
║                 ▼                                                   ║
║  ┌──────────────────────────────────────┐                          ║
║  │     SERVICIO DE APLICACIÓN           │                          ║
║  │     application/service/             │                          ║
║  │     OrderService                     │                          ║
║  └──────────────┬───────────────────────┘                          ║
║                 │ usa                                               ║
║                 ▼                                                   ║
║  ┌──────────────────────────────────────┐                          ║
║  │   PUERTOS DE SALIDA (interfaces)     │                          ║
║  │   application/port/out/              │                          ║
║  │   └── OrderRepository                │                          ║
║  └──────────────────────────────────────┘                          ║
║                                                                     ║
║  ┌──────────────────────────────────────┐                          ║
║  │   DOMINIO PURO                       │                          ║
║  │   domain/model/                      │                          ║
║  │   ├── Order (record)                 │                          ║
║  │   ├── OrderItem (record)             │                          ║
║  │   ├── OrderStatus (enum)             │                          ║
║  │   └── OrderNotFoundException         │                          ║
║  └──────────────────────────────────────┘                          ║
╚══════════════════════════════════════════════════════════════════════╝
```

**Regla fundamental:** Las flechas de dependencia apuntan siempre *hacia adentro*.  
El dominio y la capa de aplicación nunca importan clases de adaptadores ni frameworks.

---

## Estructura del Proyecto

```
toscano-post1-u8/
├── pom.xml
└── src/
    ├── main/java/orders/
    │   ├── domain/model/
    │   │   ├── Order.java                  # Entidad de dominio (record Java 17)
    │   │   ├── OrderItem.java              # Value object (record Java 17)
    │   │   ├── OrderStatus.java            # Enum de estados con transiciones válidas
    │   │   └── OrderNotFoundException.java # Excepción de dominio
    │   ├── application/
    │   │   ├── port/in/                    # Puertos de entrada (interfaces)
    │   │   │   ├── CreateOrderUseCase.java
    │   │   │   ├── GetOrderUseCase.java
    │   │   │   └── UpdateOrderStatusUseCase.java
    │   │   ├── port/out/                   # Puertos de salida (interfaces)
    │   │   │   └── OrderRepository.java
    │   │   └── service/
    │   │       └── OrderService.java       # Implementa los 3 casos de uso
    │   ├── adapter/
    │   │   ├── in/http/
    │   │   │   └── OrderController.java    # Adaptador HTTP ligero
    │   │   └── out/persistence/
    │   │       ├── InMemoryOrderRepository.java
    │   │       └── OrderDto.java           # DTO de transferencia HTTP↔Dominio
    │   └── Main.java                       # Wiring manual (sin framework de IoC)
    └── test/java/orders/
        ├── application/
        │   └── OrderServiceTest.java       # Tests de los 3 casos de uso (JUnit 5)
        └── adapter/
            └── OrderControllerTest.java    # Tests del adaptador HTTP (JUnit 5)
```

---

## Contratos de los Puertos

### `CreateOrderUseCase`
| | |
|---|---|
| **Precondición** | `command.customerId` no nulo, `command.items` no vacío |
| **Postcondición** | Pedido creado con estado `PENDING`; retorna el pedido con ID UUID asignado |
| **Excepción** | `IllegalArgumentException` si se violan precondiciones |

### `GetOrderUseCase`
| | |
|---|---|
| **Precondición** | `orderId` no nulo |
| **Postcondición** | Retorna el pedido encontrado |
| **Excepción** | `OrderNotFoundException` si no existe pedido con ese ID |

### `UpdateOrderStatusUseCase`
| | |
|---|---|
| **Precondición** | El pedido debe existir; la transición de estado debe ser válida |
| **Postcondición** | Pedido actualizado y persistido con el nuevo estado |
| **Excepción** | `OrderNotFoundException` si no existe el pedido; `IllegalStateException` si la transición es inválida |

### `OrderRepository`
| Método | Contrato |
|---|---|
| `save(Order)` | Upsert; retorna el pedido guardado |
| `findById(String)` | Retorna `Optional<Order>`; nunca nulo |
| `findByCustomerId(String)` | Retorna lista (puede ser vacía); nunca nula |

### Transiciones de estado válidas
```
PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED
   │                           │
   └──────────────────────────►  CANCELLED
```

---

## Prerrequisitos

- Java 17+ (probado con OpenJDK 21)
- Maven 3.9+

---

## Compilación y Ejecución

### Compilar y ejecutar todos los tests
```bash
mvn test
```

### Solo compilar
```bash
mvn compile
```

### Ejecutar la clase Main (demostración del wiring)
```bash
mvn compile exec:java -Dexec.mainClass="orders.Main"
```

### Compilar y ejecutar manualmente (sin Maven)
```bash
# Compilar
javac --release 17 -d target/classes \
  $(find src/main/java -name "*.java")

# Ejecutar Main
java -cp target/classes orders.Main
```

### Resultado esperado de los tests (37 verificaciones)
```
▶  Modelo de dominio
  ✔  OrderItem quantity=0 rechazado
  ✔  OrderItem price<0 rechazado
  ✔  Estado inicial PENDING
  ✔  Total calculado: 2*10+1*25=45
  ...

▶  Checkpoint 1 — Puertos compilan sin infraestructura
▶  Checkpoint 2 — OrderService con InMemoryOrderRepository (sin mocks)
▶  Checkpoint 3 — OrderController (adaptador HTTP)
▶  Checkpoint 4 — Wiring manual sin framework de inyección
▶  Casos borde adicionales

  RESULTADO: 37 pasaron  |  0 fallaron
```

---

## Checkpoints Verificados

| # | Checkpoint | Estado |
|---|---|---|
| 1 | Puertos compilan sin Spring, JPA ni ningún framework | ✅ |
| 2 | `OrderServiceTest` verifica los 3 casos de uso con `InMemoryOrderRepository`, sin mocks | ✅ |
| 3a | `OrderControllerTest` verifica que `handleCreate` retorna respuesta correcta | ✅ |
| 3b | `OrderControllerTest` verifica que `handleGet` lanza excepción para ID inexistente | ✅ |
| 4 | Wiring en `Main`: `new OrderService(new InMemoryOrderRepository())` sin framework | ✅ |

---

## Decisiones de Diseño

- **Records Java 17** para `Order`, `OrderItem`, `OrderDto` y los comandos: inmutabilidad garantizada, sin boilerplate.
- **`List.copyOf()`** en `Order.create()` para garantizar que la lista de ítems sea inmutable.
- **`Optional<Order>`** en `OrderRepository.findById()`: elimina la posibilidad de `NullPointerException`.
- **Transiciones de estado** validadas en el mismo enum `OrderStatus.canTransitionTo()`: regla de negocio donde pertenece.
- **Inyección manual de dependencias** en `Main.java`: demuestra que la arquitectura hexagonal no requiere frameworks; Spring/CDI serían intercambiables.
- **`ConcurrentHashMap`** en `InMemoryOrderRepository`: preparado para entornos multi-hilo desde el principio.
