# A2A Java SDK - Extras

This directory contains additions to what is provided by the default SDK implementations.

Please see the README's of each child directory for more details.

## HTTP Client

[`http-client-vertx`](./http-client-vertx/README.md) - Vert.x WebClient-based implementation of `A2AHttpClient` for reactive, high-performance HTTP communication. Replaces the default JDK HttpClient with a non-blocking, event-loop based client. Uses SPI for automatic discovery - simply add this library as a dependency to use it. Recommended for reactive applications, Quarkus, and high-throughput scenarios.

## Storage & Persistence

[`task-store-database-jpa`](./task-store-database-jpa/README.md) - Replaces the default `InMemoryTaskStore` with a `TaskStore` backed by a RDBMS. It uses JPA to interact with the RDBMS, providing persistence across application restarts and shared state in multi-instance deployments.

[`push-notification-config-store-database-jpa`](./push-notification-config-store-database-jpa/README.md) - Replaces the default `InMemoryPushNotificationConfigStore` with a `PushNotificationConfigStore` backed by a RDBMS. It uses JPA to interact with the RDBMS, ensuring push notification subscriptions survive restarts.

## Distributed Systems

[`queue-manager-replicated`](./queue-manager-replicated/README.md) - Replaces the default `InMemoryQueueManager` with a `QueueManager` supporting replication to other A2A servers implementing the same agent. Required for multi-instance deployments. You can write your own `ReplicationStrategy`, or use the provided MicroProfile Reactive Messaging implementation with Apache Kafka, Pulsar, or AMQP.