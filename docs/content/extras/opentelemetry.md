---
title: OpenTelemetry
description: OpenTelemetry observability integration for the A2A Java SDK with distributed tracing, context propagation, and metrics for servers and clients.
layout: page
---

# OpenTelemetry

Adds distributed tracing, metrics, and context propagation to A2A servers and clients using [OpenTelemetry](https://opentelemetry.io/).

## Features

- **Distributed Tracing**: Automatic span creation for all A2A protocol methods
- **Context Propagation**: OpenTelemetry trace context propagation across async operations
- **Request/Response Logging**: Optional extraction of request and response data into spans
- **Error Tracking**: Automatic error status and error type attributes on failures

## Modules

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| Common | `a2a-java-sdk-opentelemetry-common` | Shared utilities and constants |
| Server | `a2a-java-sdk-opentelemetry-server` | Server-side tracing |
| Client | `a2a-java-sdk-opentelemetry-client` | Client-side instrumentation |
| Client Propagation | `a2a-java-sdk-opentelemetry-client-propagation` | Context propagation for async client operations |

## Server

The `OpenTelemetryRequestHandlerDecorator` wraps the default request handler to create spans for every A2A protocol method, with automatic error tracking.

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-opentelemetry-server</artifactId>
</dependency>
```

### Architecture

```
Client Request
    |
OpenTelemetryRequestHandlerDecorator
    | (creates span)
Default RequestHandler
    |
Agent Execution (with context propagation)
    |
Response
```

```
HTTP Request (with trace headers)
    |
OpenTelemetry extracts context
    |
Span created for A2A method
    |
ManagedExecutor propagates context
    |
Async agent execution (maintains trace context)
    |
Response (with trace headers)
```

### Context-Aware Async Executor

> **Note:** The `AsyncManagedExecutorProducer` is provided by the **Quarkus reference server** (`reference/common`), not the OpenTelemetry module. It is documented here because it enables context propagation (including trace context) across async boundaries.

The reference server replaces the default `AsyncExecutorProducer` with `AsyncManagedExecutorProducer`:

- **Priority 20**: takes precedence over the default executor (priority 10)
- **Automatic activation**: no configuration needed — included automatically in the Quarkus reference server
- **Context propagation**: uses MicroProfile Context Propagation to maintain trace context and CDI request context across async boundaries

> **Note:** Unlike the default `AsyncExecutorProducer`, the `AsyncManagedExecutorProducer` does not use the `a2a.executor.*` configuration properties. Pool sizing is controlled by the container's `ManagedExecutor` configuration.

#### Quarkus Configuration

```properties
quarkus.thread-pool.core-threads=10
quarkus.thread-pool.max-threads=50
quarkus.thread-pool.queue-size=100
```

For other runtimes, consult your MicroProfile Context Propagation implementation documentation.

### Span Attributes

The following attributes are automatically added to spans:

| Attribute | Description |
|-----------|-------------|
| `gen_ai.agent.a2a.request` | Request parameters (if extraction enabled) |
| `gen_ai.agent.a2a.response` | Response data (if extraction enabled) |
| `error.type` | Error message (on failures) |

### Request/Response Extraction

Enable request and response data extraction in spans using JVM system properties:

```
# Extract request parameters into span attributes (disabled by default)
-Dorg.a2aproject.sdk.server.extract.request=true

# Extract response data into span attributes (disabled by default)
-Dorg.a2aproject.sdk.server.extract.response=true
```

> **Warning:** Extracting request/response data may expose sensitive information in traces. Use with caution in production environments.

## Client

### Instrumentation

Adds OpenTelemetry spans to A2A client operations:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-opentelemetry-client</artifactId>
</dependency>
```

### Context Propagation

For trace context propagation across async client boundaries:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-opentelemetry-client-propagation</artifactId>
</dependency>
```

## Troubleshooting

### Context Not Propagating

**Symptom:** Spans in async tasks are not linked to parent spans.

**Solution:** Ensure the OpenTelemetry server module is included and check logs for:

```
Initializing ManagedExecutor for async operations with CDI context propagation
```

### ManagedExecutor Not Available

**Symptom:** `IllegalStateException: ManagedExecutor not injected - ensure MicroProfile Context Propagation is available`

**Solution:** Ensure your runtime provides MicroProfile Context Propagation. For Quarkus, add:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-context-propagation</artifactId>
</dependency>
```

### Performance Impact

- Disable request/response extraction in production unless needed
- Configure sampling rate to reduce trace volume
- Ensure your OpenTelemetry collector is properly sized

## Best Practices

1. Configure appropriate **sampling rates** for production environments
2. Disable request/response extraction if handling **sensitive data**
3. Add **service name and version** as resource attributes
4. Use **batch processors** in your OpenTelemetry collector to reduce network overhead
5. Monitor the OpenTelemetry **collector's health** and performance

## Dependencies

- MicroProfile Telemetry 2.0.1+
- MicroProfile Context Propagation 1.3+
- OpenTelemetry API
- A2A Server Common

## See Also

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [MicroProfile Telemetry Specification](https://github.com/eclipse/microprofile-telemetry)
- [MicroProfile Context Propagation](https://github.com/eclipse/microprofile-context-propagation)
