# A2A Java SDK - Extras

This directory contains additions to what is provided by the default SDK implementations.

Please see the README's of each child directory for more details.

[`task-store-database-jpa`](./task-store-database-jpa/README.md) - Replaces the default `InMemoryTaskStore` with a `TaskStore` backed by a RDBMS. It uses JPA to interact with the RDBMS.
[`push-notification-config-store-database-jpa`](./push-notification-config-store-database-jpa/README.md) - Replaces the default `InMemoryPushNotificationConfigStore` with a `PushNotificationConfigStore` backed by a RDBMS. It uses JPA to interact with the RDBMS.
[`queue-manager-replicated`](./queue-manager-replicated/README.md) - Replaces the default `InMemoryQueueManager` with a `QueueManager` supporting replication to other A2A servers implementing the same agent. You can write your own `ReplicationStrategy`, or use the provided `MicroProfile Reactive Messaging implementation`.
[`vertx-http-client`](./vertx-http-client/README.md) - Replaces the default `HttpClient` JDK implementation with a http-client implementation backed by Vertx, better suited for Quarkus applications.