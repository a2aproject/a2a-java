# A2A Java SDK - Replicated Queue Manager

This module provides a replicated implementation of the `QueueManager` interface that enables event replication across multiple A2A instances using message brokers like Apache Kafka. It ensures that events generated in one A2A instance are propagated to other instances for distributed operation.

The replication works by intercepting events as they are enqueued and sending them to a message broker. Events received from the broker are then processed by the local A2A instance, maintaining consistency across the distributed system.

## Architecture

The main components in the replicated queue manager are:

- **[`ReplicatedQueueManager`](./core/src/main/java/io/a2a/extras/queuemanager/replicated/core/ReplicatedQueueManager.java)**: Core queue manager that wraps the default `InMemoryQueueManager` and handles event replication.
- **[`ReplicationStrategy`](./core/src/main/java/io/a2a/extras/queuemanager/replicated/core/ReplicationStrategy.java)**: Interface for different replication implementations. If `ReplicatedQueueManager` is used, a `ReplicationStrategy` **must** be provided.

Currently, one implementation is provided: [`ReactiveMessagingReplicationStrategy`](./replication-mp-reactive/src/main/java/io/a2a/extras/queuemanager/replicated/mp_reactive/ReactiveMessagingReplicationStrategy.java), which uses MicroProfile Reactive Messaging with message brokers like Apache Kafka.

## Quick Start

This section will get you up and running quickly with a `ReplicatedQueueManager` using the `ReactiveMessagingReplicationStrategy` set up to use Kafka as the message broker.

### 1. Add Dependencies

#### Core Module (Required)

Add the core replicated queue manager module to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replicated-core</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

The `ReplicatedQueueManager` is annotated in such a way that it should take precedence over the default `InMemoryQueueManager`. Hence, it is a drop-in replacement.

#### Replication Strategy Implementation (Required)

You must also include a replication strategy implementation. Currently, we provide one implementation using MicroProfile Reactive Messaging:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-queue-manager-replication-mp-reactive</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

### 2. Basic Configuration

Add to your `application.properties`:

```properties
# Configure the outgoing channel (QueueManager -> Kafka)
mp.messaging.outgoing.replicated-events-out.connector=smallrye-kafka
mp.messaging.outgoing.replicated-events-out.topic=replicated-events
mp.messaging.outgoing.replicated-events-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer

# Configure the incoming channel (Kafka -> QueueManager)
mp.messaging.incoming.replicated-events-in.connector=smallrye-kafka
mp.messaging.incoming.replicated-events-in.topic=replicated-events
mp.messaging.incoming.replicated-events-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

The channel names `replicated-events-in` and `replicated-events-out` correspond to the `@Incoming` and `@Channel` annotations in the ReactiveMessagingReplicationStrategy.

### 3. Kafka Topic Setup

Ensure your Kafka broker has the topic configured:

```bash
# Create the replicated-events topic
kafka-topics.sh --create --topic replicated-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

## Configuration

### Kafka Configuration

#### Basic Settings

```properties
# Kafka broker configuration
kafka.bootstrap.servers=kafka-broker-1:9092,kafka-broker-2:9092

# Topic configuration
mp.messaging.outgoing.replicated-events-out.topic=my-replicated-events
mp.messaging.incoming.replicated-events-in.topic=my-replicated-events

# Consumer behavior
mp.messaging.incoming.replicated-events-in.auto.offset.reset=earliest
```

#### Advanced Settings

```properties
# Consumer group configuration (important for multiple A2A instances)
mp.messaging.incoming.replicated-events-in.group.id=a2a-instance-group

# Reliability configuration
mp.messaging.outgoing.replicated-events-out.acks=all
mp.messaging.outgoing.replicated-events-out.retries=3

# Performance tuning
mp.messaging.outgoing.replicated-events-out.batch.size=16384
mp.messaging.incoming.replicated-events-in.max.poll.records=500

# Serialization configuration
mp.messaging.outgoing.replicated-events-out.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.incoming.replicated-events-in.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

### Alternative Message Brokers

While Kafka is the primary tested message broker, Quarkus Reactive Messaging supports other brokers:

#### Apache Pulsar

```properties
mp.messaging.outgoing.replicated-events-out.connector=smallrye-pulsar
mp.messaging.incoming.replicated-events-in.connector=smallrye-pulsar
pulsar.client.serviceUrl=pulsar://localhost:6650
```

#### AMQP (RabbitMQ, etc.)

```properties
mp.messaging.outgoing.replicated-events-out.connector=smallrye-amqp
mp.messaging.incoming.replicated-events-in.connector=smallrye-amqp
amqp-host=localhost
amqp-port=5672
```

**Note**: Alternative message brokers have not been tested in this project yet.

### WildFly/Jakarta EE Servers

For non-Quarkus environments, you'll need to configure MicroProfile Reactive Messaging according to your application server's documentation. The exact configuration will depend on your server's messaging capabilities, but generally, you will need to make sure the same properties as above are made available to the server application.

## How It Works

### Event Flow

1. **Event Generation**: When an event is generated in the A2A system (e.g., TaskStatusUpdateEvent), it's enqueued in the local queue
2. **Replication Hook**: The `ReplicationHook` intercepts the event and sends it to the replication strategy
3. **Message Broker**: The replication strategy serializes the event and sends it to the configured message broker
4. **Event Reception**: Other A2A instances receive the event from the message broker
5. **Local Processing**: The received event is deserialized and enqueued in the local instance's queue
6. **Event Processing**: The local instance processes the replicated event, updating its state accordingly

### Event Types

The system replicates various event types while preserving their specific types:

- **TaskStatusUpdateEvent**: Task state changes (SUBMITTED, COMPLETED, etc.)
- **TaskArtifactUpdateEvent**: Task artifact changes
- **Message**: Chat messages and responses
- **Task**: Complete task objects
- **JSONRPCError**: Error events

### Serialization

Events are serialized using Jackson with polymorphic type information to ensure proper deserialization:

```json
{
  "taskId": "task-123",
  "event": {
    "@type": "TaskStatusUpdateEvent",
    "taskId": "task-123",
    "status": {
      "state": "completed",
      "timestamp": "2023-09-29T10:30:00Z"
    },
    "final": true,
    "kind": "status-update"
  }
}
```

## Production Considerations

### Kafka Partitioning Strategy

**Critical for scalability and correctness**: How you partition your Kafka topic significantly impacts system performance and behavior.

#### Simple Approach: Single Partition

The simplest configuration uses a single partition for the replicated events topic:

```bash
kafka-topics.sh --create --topic replicated-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

**Advantages**:
- Guarantees global event ordering
- Simpler to reason about and debug
- Suitable for development, testing, and low-throughput production systems

**Disadvantages**:
- Limited scalability (single partition bottleneck)
- Cannot parallelize consumption across multiple consumer instances
- All events processed sequentially

**When to use**: Development environments, integration tests, production systems with low event volumes (<1000 events/sec), or when strict global ordering is required.

#### Recommended Approach: Partition by Task ID

For production systems with higher throughput, partition events by `taskId`:

```properties
# Configure the producer to use taskId as the partition key
mp.messaging.outgoing.replicated-events-out.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.outgoing.replicated-events-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer
```

```bash
# Create topic with multiple partitions
kafka-topics.sh --create --topic replicated-events --bootstrap-server localhost:9092 --partitions 10 --replication-factor 3
```

The `ReactiveMessagingReplicationStrategy` already sends the `taskId` as the Kafka message key, so Kafka will automatically partition by task ID using its default partitioner.

**Advantages**:
- **Horizontal scalability**: Different tasks can be processed in parallel across partitions
- **Per-task ordering guarantee**: All events for a single task go to the same partition and maintain order
- **Consumer parallelism**: Multiple consumer instances can process different partitions concurrently

**Disadvantages**:
- No global ordering across all tasks
- More complex to debug (events spread across partitions)
- Requires proper consumer group configuration

**When to use**: Production systems with medium to high throughput, systems that need to scale horizontally, distributed deployments with multiple A2A instances.

#### Consumer Group Configuration

When using multiple partitions, ensure all A2A instances belong to the same consumer group:

```properties
mp.messaging.incoming.replicated-events-in.group.id=a2a-instance-group
```

This ensures that:
- Each partition is consumed by exactly one instance
- Events for the same task always go to the same instance (partition affinity)
- System can scale horizontally by adding more instances (up to the number of partitions)

**Rule of thumb**: Number of partitions ≥ number of A2A instances for optimal distribution.

### Transaction-Aware Queue Cleanup ("Poison Pill")

When a task reaches a final state (COMPLETED, FAILED, CANCELED), all nodes in the cluster must terminate their event consumers for that task. This is achieved through a special "poison pill" event (`QueueClosedEvent`) that is replicated to all nodes.

#### How It Works

The poison pill mechanism uses **transaction-aware CDI events** to ensure the poison pill is only sent AFTER the final task state is durably committed to the database:

1. **Task Finalization**: When `JpaDatabaseTaskStore.save()` persists a task with a final state, it fires a `TaskFinalizedEvent` CDI event
2. **Transaction Coordination**: The CDI observer is configured with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`, which delays event delivery until AFTER the JPA transaction commits
3. **Poison Pill Delivery**: `ReplicatedQueueManager.onTaskFinalized()` receives the event and sends `QueueClosedEvent` via the replication strategy
4. **Cluster-Wide Termination**: All nodes receive the `QueueClosedEvent`, recognize it as final, and gracefully terminate their event consumers

**Key Architecture Decision**: We use JPA transaction lifecycle hooks instead of time-based delays for poison pill delivery because:
- **Eliminates race conditions**: No time window where the poison pill might arrive before the database commit
- **Deterministic cleanup**: Queue termination happens immediately after transaction commit, without delay-based tuning
- **Simplicity**: No need to monitor consumer lag or configure delays for cleanup timing
- **Reliability**: Works correctly regardless of network latency or database performance

**Note**: While the poison pill mechanism eliminates delays for cleanup, the system still uses a configurable grace period (`a2a.replication.grace-period-seconds`, default 15s) in `JpaDatabaseTaskStore.isTaskActive()` to handle late-arriving replicated events. This grace period prevents queue recreation for tasks that were recently finalized, accommodating Kafka consumer lag and network delays. See the Grace Period Configuration section below for details.

#### Code Flow

**JpaDatabaseTaskStore** (fires CDI event):
```java
@Inject
Event<TaskFinalizedEvent> taskFinalizedEvent;

public void save(Task task) {
    // ... persist task to database ...

    // Fire CDI event if task reached final state
    if (task.getStatus().state().isFinal()) {
        taskFinalizedEvent.fire(new TaskFinalizedEvent(task.getId()));
    }
    // Transaction commits here (end of method)
}
```

**ReplicatedQueueManager** (observes and sends poison pill):
```java
public void onTaskFinalized(@Observes(during = TransactionPhase.AFTER_SUCCESS) TaskFinalizedEvent event) {
    String taskId = event.getTaskId();
    LOGGER.debug("Task {} finalized - sending poison pill after transaction commit", taskId);

    // Send QueueClosedEvent to all nodes via replication
    QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);
    replicationStrategy.send(taskId, closedEvent);
}
```

#### Configuration

No configuration is required for the poison pill mechanism - it works automatically when:
1. Using `JpaDatabaseTaskStore` for task persistence
2. Using `ReplicatedQueueManager` for event replication
3. Both modules are present in your application

#### Monitoring

Enable debug logging to monitor poison pill delivery:

```properties
quarkus.log.category."io.a2a.extras.queuemanager.replicated".level=DEBUG
quarkus.log.category."io.a2a.extras.taskstore.database.jpa".level=DEBUG
```

You should see log entries like:
```
Task abc-123 is in final state, firing TaskFinalizedEvent
Task abc-123 finalized - sending poison pill (QueueClosedEvent) after transaction commit
```

#### Grace Period Configuration

While the poison pill mechanism provides deterministic cleanup timing, the system uses a configurable **grace period** to handle late-arriving replicated events. This is separate from the poison pill mechanism and serves a different purpose.

**Purpose**: The grace period prevents queue recreation for tasks that were recently finalized. When a replicated event arrives after a task is finalized, the system checks if the task is still within the grace period before creating a new queue.

**Configuration**:
```properties
# Grace period for handling late-arriving events (default: 15 seconds)
a2a.replication.grace-period-seconds=15
```

**How It Works**:
1. When a task is finalized, `JpaDatabaseTaskStore` records the `finalizedAt` timestamp
2. When a replicated event arrives, `ReplicatedQueueManager.onReplicatedEvent()` calls `taskStateProvider.isTaskActive(taskId)`
3. `JpaDatabaseTaskStore.isTaskActive()` returns `true` if:
   - Task is not in a final state, OR
   - Task is final but within the grace period (`now < finalizedAt + gracePeriodSeconds`)
4. If `isTaskActive()` returns `false`, the replicated event is skipped (no queue created)

**When to Adjust**:
- **Increase** the grace period if you observe warnings about skipped events for inactive tasks in high-latency networks
- **Decrease** the grace period to reduce memory usage in systems with very low latency and high task turnover
- **Default (15s)** is suitable for most deployments with typical Kafka consumer lag

**Monitoring**:
```properties
quarkus.log.category."io.a2a.extras.queuemanager.replicated".level=DEBUG
```

Watch for:
```
Skipping replicated event for inactive task abc-123  # Event arrived too late
```

**Important**: This grace period is for **late event handling**, not cleanup timing. The poison pill mechanism handles cleanup deterministically without delays.

## Advanced Topics

### Custom Replication Strategies

The architecture is designed to support additional replication strategies in the future. To implement a custom replication strategy, create a class that implements the `ReplicationStrategy` interface and ensure it's discoverable via CDI:

```java
@ApplicationScoped
public class CustomReplicationStrategy implements ReplicationStrategy {

    @Override
    public void send(String taskId, Event event) {
        // Implement custom replication logic
        // e.g., send to database, REST API, etc.
    }
}
```

### Monitoring Events

You can monitor replicated events by observing CDI events:

```java
@ApplicationScoped
public class ReplicationMonitor {

    public void onReplicatedEvent(@Observes ReplicatedEvent event) {
        // Monitor replicated events for metrics, logging, etc.
        LOGGER.info("Received replicated event for task: " + event.getTaskId());
    }
}
```

### Logging

Enable debug logging to monitor replication activity:

```properties
# For Quarkus
quarkus.log.category."io.a2a.extras.queuemanager.replicated".level=DEBUG

# For other servers, configure your logging framework accordingly
```

### Health Checks

When using Quarkus, the module integrates with MicroProfile Health to provide health checks:

```properties
# Configure health check timeout
quarkus.messaging.kafka.health.timeout=5s
```
