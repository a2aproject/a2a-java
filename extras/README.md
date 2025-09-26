# A2A Java SDK - Extras

This directory contains additions to what is provided by the default SDK implementations.

Please see the README's of each child directory for more details.

[`task-store-database-jpa`](./task-store-database-jpa/README.md) - Replaces the default `InMemoryTaskStore` with a `TaskStore` backed by a RDBMS. It uses JPA to interact with the RDBMS.
[`push-notification-config-store-database-jpa`](./push-notification-config-store-database-jpa/README.md) - Replaces the default `InMemoryPushNotificationConfigStore` with a `PushNotificationConfigStore` backed by a RDBMS. It uses JPA to interact with the RDBMS.