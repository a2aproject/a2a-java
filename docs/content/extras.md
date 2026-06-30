---
title: Extras
description: Optional add-ons for the A2A Java SDK including alternative HTTP clients, database-backed stores, distributed queue replication, and OpenTelemetry observability.
layout: page
---

# Extras

The A2A Java SDK ships with sensible defaults — an in-memory task store, an in-memory push notification config store, a single-instance queue manager, and a JDK HttpClient. The **extras** modules are drop-in replacements that swap these defaults for production-grade alternatives.

All extras use **Java SPI** or **CDI alternative** discovery, so adding the dependency is typically all you need — no code changes required.

## BOM

Import the extras BOM to manage versions:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.a2aproject.sdk</groupId>
            <artifactId>a2a-java-extras-bom</artifactId>
            <version>$\{org.a2aproject.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## [HTTP Clients](/extras/http-clients)

Alternative `A2AHttpClient` implementations. The **Vert.x HTTP Client** (`a2a-java-sdk-http-client-vertx`) replaces the default JDK HttpClient with a reactive, non-blocking Vert.x WebClient — recommended for Quarkus, reactive frameworks, and high-throughput scenarios. An **Android HTTP Client** (`a2a-java-sdk-http-client-android`) is also available for mobile applications.

## [Storage & Persistence](/extras/storage)

JPA-backed replacements for the in-memory stores, providing database persistence for production and load-balanced deployments. Includes a **JPA Task Store** (`a2a-java-extras-task-store-database-jpa`) and a **JPA Push Notification Config Store** (`a2a-java-extras-push-notification-config-store-database-jpa`). Both use Jakarta Persistence API (JPA 3.0+) and share the same datasource configuration.

## [Replicated Queue Manager](/extras/replicated-queue-manager)

Replaces the default `InMemoryQueueManager` with event replication across multiple A2A server instances via message brokers. Required for multi-instance deployments. The core module (`a2a-java-queue-manager-replicated-core`) pairs with a MicroProfile Reactive Messaging strategy (`a2a-java-queue-manager-replication-mp-reactive`) supporting Apache Kafka, Pulsar, or AMQP. You can also write your own `ReplicationStrategy`.

## [OpenTelemetry](/extras/opentelemetry)

Distributed tracing, metrics, and context propagation for A2A servers and clients using OpenTelemetry. The server module (`a2a-java-sdk-opentelemetry-server`) adds automatic span creation for all protocol methods with context propagation across async boundaries. Client modules (`a2a-java-sdk-opentelemetry-client`, `a2a-java-sdk-opentelemetry-client-propagation`) instrument A2A client operations.
