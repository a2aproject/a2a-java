# A2A Java SDK - Extras

This directory contains optional extensions to the A2A Java SDK that enhance the default in-memory implementations with production-ready persistence and distributed capabilities.

## Overview

The default A2A SDK uses in-memory storage for simplicity and ease of getting started. The extras modules provide drop-in replacements for production deployments that require:

- **Data Persistence**: Survive application restarts without losing tasks or configurations
- **Scalability**: Support horizontal scaling across multiple server instances
- **High Availability**: Enable distributed A2A deployments with event replication

All extras modules are designed as **drop-in replacements** using CDI priority annotations, requiring only dependency addition and configuration - no code changes needed.

## Available Modules

### 1. [JPA Database TaskStore](./task-store-database-jpa/README.md)

**What it does**: Replaces the default `InMemoryTaskStore` with a JPA-based implementation that persists tasks to a relational database (PostgreSQL, MySQL, Oracle, etc.).

**Problem it solves**:
- In-memory storage loses all task data on application restart
- Cannot share task state across multiple server instances
- Limited by available memory for large numbers of tasks

**When to use**:
- Production deployments requiring task persistence across restarts
- Multi-instance deployments where task state must be shared
- Long-running tasks that need to survive server updates
- Compliance requirements for task history and audit trails

**Key features**:
- Drop-in replacement (just add dependency and configure database)
- Works with any JPA 3.0+ provider (Hibernate, EclipseLink, etc.)
- Compatible with Quarkus and Jakarta EE application servers
- Automatic schema generation and migration support
- Transaction-aware task state management

---

### 2. [JPA Database PushNotificationConfigStore](./push-notification-config-store-database-jpa/README.md)

**What it does**: Replaces the default `InMemoryPushNotificationConfigStore` with a JPA-based implementation that persists push notification configurations to a relational database.

**Problem it solves**:
- Push notification subscriptions are lost on application restart
- Cannot share notification configurations across server instances
- Clients must re-subscribe after every deployment

**When to use**:
- Production deployments with push notification support
- Multi-instance A2A servers sharing notification state
- Systems requiring durable client subscriptions
- Applications where notification reliability is critical

**Key features**:
- Drop-in replacement (just add dependency and configure database)
- Composite primary key support (taskId + configId)
- Works with any JPA 3.0+ provider
- Survives application restarts and redeployments
- Shared state across multiple server instances

---

### 3. [Replicated Queue Manager](./queue-manager-replicated/README.md)

**What it does**: Replaces the default `InMemoryQueueManager` with a replicated implementation that synchronizes events across multiple A2A server instances using message brokers (Apache Kafka, Pulsar, AMQP).

**Problem it solves**:
- Events generated in one server instance are invisible to others
- Cannot horizontally scale A2A servers while maintaining event consistency
- No coordination between distributed A2A instances serving the same agent
- Task state changes not propagated across cluster nodes

**When to use**:
- **Required** when deploying multiple instances of the same A2A agent
- High-availability deployments with load balancing
- Cloud-native deployments requiring horizontal scaling
- Distributed systems where events must be synchronized
- Microservices architectures with event-driven coordination

**Key features**:
- Event replication via Apache Kafka (primary), Pulsar, or AMQP
- Pluggable replication strategies via `ReplicationStrategy` interface
- Transaction-aware "poison pill" mechanism for clean queue termination
- Automatic serialization/deserialization with type preservation
- Kafka partitioning by taskId for scalability and ordering guarantees
- Configurable grace period for handling late-arriving events
- MicroProfile Reactive Messaging integration (provided implementation)
- Custom replication strategies supported

**Architecture**:
- **Core Module**: `queue-manager-replicated-core` (required)
- **Replication Strategy**: `queue-manager-replication-mp-reactive` (MicroProfile implementation)
- Extensible design: implement your own `ReplicationStrategy` for custom backends

---

## Module Comparison

| Feature | InMemory (Default) | Database (JPA) | Replicated Queue |
|---------|-------------------|----------------|------------------|
| **Persistence** | No | Yes | No* |
| **Multi-Instance Support** | No | Yes | Yes |
| **Horizontal Scaling** | No | Limited** | Yes |
| **Production Ready** | Development only | Yes | Yes |
| **Additional Infrastructure** | None | RDBMS | Message Broker |
| **Configuration Complexity** | Minimal | Low | Medium |

\* Replicated Queue Manager focuses on event synchronization. Use with JPA TaskStore for full persistence.
\** Database provides shared state but lacks event coordination across instances.

## Recommended Combinations

### Development / Testing
```xml
<!-- Use defaults - no extras needed -->
```

### Production - Single Instance
```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
</dependency>
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-push-notification-config-store-database-jpa</artifactId>
</dependency>
```

### Production - Multi-Instance / High Availability
```xml
<!-- Task and config persistence -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-task-store-database-jpa</artifactId>
</dependency>
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-push-notification-config-store-database-jpa</artifactId>
</dependency>

<!-- Event replication across instances -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replicated-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replication-mp-reactive</artifactId>
</dependency>
```

## Getting Started

Each module contains its own detailed README with:
- Quick start guides
- Configuration examples for Quarkus and Jakarta EE
- Database schema details
- Production considerations
- Troubleshooting tips

Click the module names above to view detailed documentation.