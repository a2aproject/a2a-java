---
title: Replicated Queue Manager
description: Distributed QueueManager implementation for the A2A Java SDK that replicates events across multiple server instances using message brokers like Apache Kafka.
layout: page
---

# Replicated Queue Manager

Replaces the default `InMemoryQueueManager` with a queue manager that replicates events across multiple A2A server instances. Required for multi-instance (load-balanced) deployments where all nodes must see every event.

## Architecture

The replicated queue manager has two core components:

- **`ReplicatedQueueManager`** — wraps the default `InMemoryQueueManager` and handles event replication. It is annotated to take precedence over the default, making it a drop-in replacement.
- **`ReplicationStrategy`** — interface for pluggable replication implementations. A `ReplicationStrategy` **must** be provided when using `ReplicatedQueueManager`.

The provided implementation, `ReactiveMessagingReplicationStrategy`, uses MicroProfile Reactive Messaging with message brokers like Apache Kafka.

## Event Flow

1. An event is generated in the A2A system (e.g., `TaskStatusUpdateEvent`) and enqueued locally
2. A `ReplicationHook` intercepts the event and sends it to the replication strategy
3. The replication strategy serializes the event and sends it to the message broker
4. Other A2A instances receive the event from the broker
5. The received event is deserialized and enqueued in each instance's local queue

### Replicated Event Types

The system replicates these event types while preserving their specific types:

- `TaskStatusUpdateEvent` — task state changes (SUBMITTED, COMPLETED, etc.)
- `TaskArtifactUpdateEvent` — task artifact changes
- `Message` — chat messages and responses
- `Task` — complete task objects
- `A2AError` — error events

Events are serialized using Jackson with polymorphic type information:

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

## Quick Start

### 1. Add Dependencies

```xml
<!-- Core (required) -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-queue-manager-replicated-core</artifactId>
</dependency>

<!-- MicroProfile Reactive Messaging strategy (required) -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-queue-manager-replication-mp-reactive</artifactId>
</dependency>
```

### 2. Configure Kafka

Add to your `application.properties`:

```properties
# Outgoing (QueueManager -> Kafka)
mp.messaging.outgoing.replicated-events-out.connector=smallrye-kafka
mp.messaging.outgoing.replicated-events-out.topic=replicated-events
mp.messaging.outgoing.replicated-events-out.value.serializer=org.apache.kafka.common.serialization.StringSerializer

# Incoming (Kafka -> QueueManager)
mp.messaging.incoming.replicated-events-in.connector=smallrye-kafka
mp.messaging.incoming.replicated-events-in.topic=replicated-events
mp.messaging.incoming.replicated-events-in.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

The channel names `replicated-events-in` and `replicated-events-out` correspond to the `@Incoming` and `@Channel` annotations in the `ReactiveMessagingReplicationStrategy`.

### 3. Create the Kafka Topic

```bash
kafka-topics.sh --create --topic replicated-events \
    --bootstrap-server localhost:9092 \
    --partitions 3 --replication-factor 1
```

## Configuration

### Kafka Settings

```properties
# Kafka broker configuration
kafka.bootstrap.servers=kafka-broker-1:9092,kafka-broker-2:9092

# Topic configuration
mp.messaging.outgoing.replicated-events-out.topic=my-replicated-events
mp.messaging.incoming.replicated-events-in.topic=my-replicated-events

# Consumer group (all A2A instances should share the same group)
mp.messaging.incoming.replicated-events-in.group.id=a2a-instance-group

# Consumer behavior
mp.messaging.incoming.replicated-events-in.auto.offset.reset=earliest

# Reliability configuration
mp.messaging.outgoing.replicated-events-out.acks=all
mp.messaging.outgoing.replicated-events-out.retries=3

# Performance tuning
mp.messaging.outgoing.replicated-events-out.batch.size=16384
mp.messaging.incoming.replicated-events-in.max.poll.records=500

# Serialization (key serialization needed for partition-by-taskId strategy)
mp.messaging.outgoing.replicated-events-out.key.serializer=org.apache.kafka.common.serialization.StringSerializer
mp.messaging.incoming.replicated-events-in.key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

## Production Considerations

### Kafka Partitioning Strategy

How you partition your Kafka topic significantly impacts scalability and correctness.

#### Single Partition

```bash
kafka-topics.sh --create --topic replicated-events \
    --bootstrap-server localhost:9092 \
    --partitions 1 --replication-factor 1
```

- Guarantees global event ordering
- Suitable for development, testing, and low-throughput production (&lt;1000 events/sec)
- Cannot parallelize consumption

#### Partition by Task ID (Recommended)

```bash
kafka-topics.sh --create --topic replicated-events \
    --bootstrap-server localhost:9092 \
    --partitions 10 --replication-factor 3
```

The `ReactiveMessagingReplicationStrategy` sends the `taskId` as the Kafka message key, so Kafka automatically partitions by task ID.

- **Per-task ordering guarantee**: all events for a single task go to the same partition
- **Horizontal scalability**: different tasks can be processed in parallel across partitions
- **Consumer parallelism**: multiple consumer instances can process different partitions concurrently
- **Rule of thumb**: number of partitions &ge; number of A2A instances

### Consumer Group Configuration

When using multiple partitions, ensure all A2A instances belong to the same consumer group:

```properties
mp.messaging.incoming.replicated-events-in.group.id=a2a-instance-group
```

This ensures each partition is consumed by exactly one instance, and events for the same task always reach the same instance.

### Poison Pill Cleanup

When a task reaches a final state (COMPLETED, FAILED, CANCELED), all nodes must terminate their event consumers for that task. This is handled automatically via a `QueueClosedEvent` (poison pill) that is replicated to all nodes.

The mechanism uses **transaction-aware CDI events**: the poison pill is only sent AFTER the final task state is committed to the database (`@Observes(during = TransactionPhase.AFTER_SUCCESS)`), eliminating race conditions.

```java
// JpaDatabaseTaskStore fires CDI event after persist
if (task.status().state().isFinal()) {
    taskFinalizedEvent.fire(new TaskFinalizedEvent(task.id(), task));
}

// ReplicatedQueueManager observes it after transaction commit
public void onTaskFinalized(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) TaskFinalizedEvent event) {
    QueueClosedEvent closedEvent = new QueueClosedEvent(event.getTaskId());
    replicationStrategy.send(event.getTaskId(), closedEvent);
}
```

No configuration is required — the poison pill mechanism works automatically when using `JpaDatabaseTaskStore` together with `ReplicatedQueueManager`.

### Grace Period

A configurable grace period prevents queue recreation for tasks that were recently finalized, accommodating Kafka consumer lag and network delays:

```properties
# Grace period for handling late-arriving events (default: 15 seconds)
a2a.replication.grace-period-seconds=15
```

When a replicated event arrives after a task is finalized, the system checks if the task is still within the grace period before creating a new queue. Increase the grace period in high-latency networks; decrease it for systems with low latency and high task turnover.

## Alternative Message Brokers

While Kafka is the primary tested broker, MicroProfile Reactive Messaging supports other brokers:

### Apache Pulsar

```properties
mp.messaging.outgoing.replicated-events-out.connector=smallrye-pulsar
mp.messaging.incoming.replicated-events-in.connector=smallrye-pulsar
pulsar.client.serviceUrl=pulsar://localhost:6650
```

### AMQP (RabbitMQ, etc.)

```properties
mp.messaging.outgoing.replicated-events-out.connector=smallrye-amqp
mp.messaging.incoming.replicated-events-in.connector=smallrye-amqp
amqp-host=localhost
amqp-port=5672
```

> **Note:** Alternative message brokers have not been tested in this project yet.

### WildFly/Jakarta EE Servers

For non-Quarkus environments, configure MicroProfile Reactive Messaging according to your application server's documentation.

## Custom Replication Strategies

You can implement your own `ReplicationStrategy` if MicroProfile Reactive Messaging does not fit your environment:

```java
@ApplicationScoped
public class CustomReplicationStrategy implements ReplicationStrategy {

    @Override
    public void send(String taskId, Event event) {
        // Your replication logic (e.g., database, REST API, etc.)
    }
}
```

## Monitoring

### Logging

Enable debug logging to monitor replication activity:

```properties
quarkus.log.category."org.a2aproject.sdk.extras.queuemanager.replicated".level=DEBUG
quarkus.log.category."org.a2aproject.sdk.extras.taskstore.database.jpa".level=DEBUG
```

Example log entries:

```
Task abc-123 is in final state, firing TaskFinalizedEvent
Task abc-123 finalized - sending poison pill (QueueClosedEvent) after transaction commit
Skipping replicated event for inactive task abc-123
```

### Health Checks

When using Quarkus, the module integrates with MicroProfile Health:

```properties
quarkus.messaging.kafka.health.timeout=5s
```

### CDI Event Monitoring

You can monitor replicated events by observing CDI events:

```java
@ApplicationScoped
public class ReplicationMonitor {

    public void onReplicatedEvent(@Observes ReplicatedEventQueueItem replicatedEvent) {
        LOGGER.info("Received replicated event for task: " + replicatedEvent.getTaskId());
    }
}
```
