---
title: OpenTelemetry
description: OpenTelemetry observability integration for the A2A Java SDK with distributed tracing and context propagation for servers and clients.
layout: page
---

# OpenTelemetry

Adds distributed tracing and context propagation to A2A servers and clients using [OpenTelemetry](https://opentelemetry.io/).

## Features

- **Distributed Tracing**: Automatic span creation for all A2A protocol methods
- **Context Propagation**: OpenTelemetry trace context propagation across async operations
- **Request/Response Logging**: Optional extraction of request and response data into spans
- **Protocol Logs**: Opt-in structured OpenTelemetry log records, with separately opt-in bounded and sanitized payloads
- **Error Tracking**: Automatic error status and error type attributes on failures
- **Metrics**: `gen_ai.client.operation.duration` histogram on the client side; `gen_ai.agent.a2a.streaming.duration` histogram for full streaming lifecycle on the server side

## Modules

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| Common | `a2a-java-sdk-opentelemetry-common` | Shared utilities and constants |
| Server | `a2a-java-sdk-opentelemetry-server` | Server-side tracing |
| Client | `a2a-java-sdk-opentelemetry-client` | Client-side instrumentation |
| Client Propagation | `a2a-java-sdk-opentelemetry-client-propagation` | Context propagation for async client operations |

## Server

The `OpenTelemetryRequestHandlerDecorator` wraps the default request handler to create spans for every A2A protocol method, with automatic error tracking.

### Add Dependency

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

### Streaming Span Lifecycle

Streaming methods (`message/stream` and `tasks/resubscribe`) follow a two-span model to correctly represent the full duration of a streaming response.

**Span 1 — method span** is created when the request arrives and ended as soon as the publisher is returned to the transport layer. It captures request attributes and the initial outcome (success or synchronous error).

**Per-event child spans** (`<method>-event`) are created for each streaming event published via `onNext()`. Each child span is parented to Span 1 and made current during delegate execution, so any nested telemetry is correctly parented.

**Span 2 — end span** (`<method>-end`) is created when the stream terminates — either on completion or error. It is also parented to Span 1.

<pre class="mermaid">
sequenceDiagram
    participant C as Client
    participant D as OTel Decorator
    participant H as RequestHandler
    participant W as WrappedPublisher

    C->>D: onMessageSendStream(params)
    D->>D: create Span₁ (SERVER)
    D->>H: onMessageSendStream(params)
    H-->>D: publisher
    D->>D: capture Span₁.context
    D->>D: Span₁.setStatus(OK) · Span₁.end()
    D-->>C: WrappedPublisher

    note over D: Span₁ closed immediately

    loop for each streaming event
        W->>W: create child span (parent=Span₁)
        W-->>C: onNext(event)
        W->>W: child span.end()
    end

    alt stream completes normally
        W->>W: create Span₂ (SERVER, parent=Span₁)
        W->>W: Span₂.setStatus(OK) · Span₂.end()
        W-->>C: onComplete()
    else stream errors
        W->>W: create Span₂ (SERVER, parent=Span₁)
        W->>W: Span₂.setStatus(ERROR) · Span₂.end()
        W-->>C: onError(throwable)
    end
</pre>

Each per-event child span carries:

| Attribute | Value |
|-----------|-------|
| `gen_ai.agent.a2a.streaming-event` | Event kind identifier (`"message"`, `"task"`, `"statusUpdate"`, `"artifactUpdate"`) |
| `gen_ai.agent.a2a.task.status.state` | Task status state (for `TaskStatusUpdateEvent` and `Task` events) |
| `gen_ai.agent.a2a.task_id` | Task identifier (when available from the event) |

### Metrics

The server decorator records the following histogram instruments:

| Instrument | Type | Unit | Description |
|------------|------|------|-------------|
| `gen_ai.agent.a2a.streaming.duration` | Histogram | `s` | Duration from stream initiation to last event, for `message/stream` and `tasks/resubscribe` |

Attributes on `gen_ai.agent.a2a.streaming.duration`:

| Attribute | Value |
|-----------|-------|
| `gen_ai.agent.a2a.operation.name` | A2A method name (`message/stream` or `tasks/resubscribe`) |
| `gen_ai.system` | `a2a` |
| `error.type` | `error` (only on stream failure) |

Meter injection uses CDI `Instance<Meter>`, so the histogram is silently skipped if no `Meter` bean is available in the container.

### Context-Aware Async Executor

> **Note:** The `AsyncManagedExecutorProducer` is provided by the **Quarkus reference server** ([`reference/common`](https://github.com/a2aproject/a2a-java/blob/main/reference/common/src/main/java/org/a2aproject/sdk/server/common/quarkus/AsyncManagedExecutorProducer.java)), not the OpenTelemetry module. It is documented here because it enables context propagation (including trace context) across async boundaries.


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

The following attributes are automatically added to spans (defined in `A2AObservabilityNames`):

| Attribute | Description |
|-----------|-------------|
| `gen_ai.agent.a2a.operation.name` | The A2A method name (e.g., `message/send`, `tasks/get`) |
| `gen_ai.agent.a2a.task_id` | Task identifier (when available) |
| `gen_ai.agent.a2a.context_id` | Context/conversation identifier (when available) |
| `gen_ai.agent.a2a.message_id` | Message identifier (when available) |
| `gen_ai.agent.a2a.role` | Message role (when available) |
| `gen_ai.agent.a2a.extensions` | Comma-separated extension names (when present) |
| `gen_ai.agent.a2a.parts.number` | Number of message parts |
| `gen_ai.agent.a2a.config_id` | Push notification config identifier (when available) |
| `gen_ai.agent.a2a.request` | Full request parameters (only if extraction enabled) |
| `gen_ai.agent.a2a.response` | Full response data (only if extraction enabled) |
| `error.type` | Error message (on failures) |

### Push Notification Delivery Tracing

When the server delivers a push notification to a client webhook, `OpenTelemetryPushNotificationSenderDecorator` creates a `CLIENT` span named `SendPushNotification`. This span is independent of the original request span but carries the `gen_ai.agent.a2a.task_id` attribute, allowing correlation by task ID in your observability tool. The span status reflects the actual delivery outcome: `OK` when all notifications succeed, `ERROR` when one or more deliveries fail (with `error.type` set to `delivery_failed`) or when an exception is thrown.

The module also provides **W3C trace-context propagation** for outbound push notification HTTP requests via the `@Instrumented` CDI qualifier and `OpenTelemetryHttpClientProducer`.

#### How it works

The `OpenTelemetryHttpClientProducer` CDI bean produces an `@Instrumented A2AHttpClient` — a wrapper that injects `traceparent` and `tracestate` headers into every outbound HTTP request. `BasePushNotificationSender` resolves the HTTP client in this order:

1. An `@Instrumented A2AHttpClient` bean (if available — e.g. the one produced by this module)
2. An unqualified `A2AHttpClient` bean (if the application provides one)
3. `A2AHttpClientFactory.create()` as last resort

The `@Instrumented` qualifier (defined in `server-common`) marks HTTP client beans that have been wrapped with observability instrumentation. It ensures the instrumented client does not compete with the application's own `A2AHttpClient` bean in default CDI resolution — the two coexist, and the instrumented variant wraps the application's bean as its delegate.

#### Custom HTTP client compatibility

If your application provides a custom `A2AHttpClient` CDI bean (e.g. for mTLS, proxy, or custom timeouts), the OpenTelemetry wrapper **preserves that configuration** by delegating to your bean rather than replacing it. No additional annotations or configuration are needed — just produce your bean as usual:

```java
@Produces
@ApplicationScoped
public A2AHttpClient myHttpClient() {
    // your custom client (mTLS, proxy, auth, etc.)
}
```

The OpenTelemetry module automatically wraps it with trace-context injection.

#### Context-propagating executor

The producer also creates an `@Instrumented Executor` (via `Context.taskWrapping()`) that ensures the OTel trace context survives the async boundary when push notifications are dispatched to multiple recipients in parallel. This allows the receiving webhook to join the distributed trace.

`BasePushNotificationSender` resolves the dispatch executor in this order:

1. An `@Instrumented Executor` bean (if available — e.g. the one produced by this module)
2. A `@Named("a2a.pushNotificationDispatch") Executor` bean (if the application provides one)
3. `ForkJoinPool.commonPool()` as last resort

#### Custom dispatch executor compatibility

If your application provides a custom `@Named("a2a.pushNotificationDispatch") Executor` CDI bean (e.g. for custom thread pool sizing), the OpenTelemetry module **preserves that configuration** by wrapping it with `Context.taskWrapping()` rather than replacing it. Just produce your bean as usual:

```java
@Produces
@ApplicationScoped
@Named("a2a.pushNotificationDispatch")
public Executor myDispatchExecutor() {
    // your custom thread pool
}
```

The OpenTelemetry module automatically wraps it with context propagation.

| Attribute | Description |
|-----------|-------------|
| `gen_ai.agent.a2a.operation.name` | `SendPushNotification` |
| `gen_ai.agent.a2a.task_id` | Task identifier |
| `gen_ai.agent.a2a.context_id` | Context identifier (when available) |
| `gen_ai.agent.a2a.push_notification.event_kind` | The type of streaming event delivered (e.g. `TaskStatusUpdateEvent`) |
| `gen_ai.agent.a2a.response` | Full event payload (only if extraction enabled) |
| `error.type` | `delivery_failed` when one or more deliveries fail, or the exception message when an exception is thrown |

### Request/Response Extraction

Enable request and response data extraction in spans using JVM system properties:

```
# Extract request parameters into span attributes (disabled by default)
-Dorg.a2aproject.sdk.server.extract.request=true

# Extract response data into span attributes (disabled by default)
-Dorg.a2aproject.sdk.server.extract.response=true
```

> **Warning:** Extracting request/response data may expose sensitive information in traces. Use with caution in production environments.

### Server Protocol Logs

Server protocol logs are disabled unless the application provides a CDI bean of type
`ProtocolLogEmitter`. The request handler records incoming message requests and subscriptions,
outgoing responses and streaming events, and stream completion, failure, or cancellation. The push
sender records aggregate delivery outcomes and, for the CDI-managed default HTTP sender, each
per-destination delivery outcome without recording the destination URL or credentials.

```java
@Produces
@Singleton
ProtocolLogEmitter protocolLogEmitter(OpenTelemetry openTelemetry) {
    return new ProtocolLogEmitter(openTelemetry.getLogsBridge().get("my-service"));
}
```

This enables metadata-only records. To opt in to payload bodies, construct the emitter with a
`ProtocolPayloadFormatter` as described under [Payload Capture and Security](#payload-capture-and-security).
Applications must configure an OpenTelemetry logs SDK and exporter. Protocol logs are observability
data, not a durable audit trail, and authorization decisions still require application-level
instrumentation.

## Client

### Instrumentation

Adds OpenTelemetry spans and metrics to A2A client operations:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-opentelemetry-client</artifactId>
</dependency>
```

### Metrics

The client transport wrapper is auto-discovered via ServiceLoader — just adding the dependency enables tracing. To also enable `gen_ai.client.operation.duration` metrics, pass a `Meter` via the transport config:

```java
config.setParameters(Map.of(
    OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY, openTelemetry.getTracer("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_METER_KEY,  openTelemetry.getMeter("my-service")
));
```

If no `Meter` is provided, the histogram is silently skipped.

| Instrument | Type | Unit | Description |
|------------|------|------|-------------|
| `gen_ai.client.operation.duration` | Histogram | `s` | Duration of each A2A client operation call |

Attributes on `gen_ai.client.operation.duration`:

| Attribute | Value |
|-----------|-------|
| `gen_ai.agent.a2a.operation.name` | A2A method name (e.g., `message/send`, `tasks/get`) |
| `gen_ai.system` | `a2a` |
| `error.type` | `error` (only on failure) |

### Client Protocol Logs

Client protocol logs are disabled unless an OpenTelemetry `Logger` is supplied together with the
tracer. Supplying a logger enables metadata-only DEBUG records; `OTEL_LOG_SEVERITY_KEY` can select a
different OpenTelemetry `Severity`.

```java
config.setParameters(Map.of(
    OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY,
        openTelemetry.getTracer("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_LOGGER_KEY,
        openTelemetry.getLogsBridge().get("my-service")
));
```

Payload capture is a separate opt-in. Add `OTEL_PAYLOAD_FORMATTER_KEY` with a
`ProtocolPayloadFormatter`:

```java
config.setParameters(Map.of(
    OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY,
        openTelemetry.getTracer("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_LOGGER_KEY,
        openTelemetry.getLogsBridge().get("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_PAYLOAD_FORMATTER_KEY,
        new ProtocolPayloadFormatter(ProtocolPayloadFormatter.DEFAULT_MAX_BYTES, value -> value)
));
```

### Context Propagation

For trace context propagation across async client boundaries:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-opentelemetry-client-propagation</artifactId>
</dependency>
```

## Protocol Log Schema

Every record includes `a2a.event.name`, `a2a.operation.name`, and `a2a.log.direction` (`inbound` or
`outbound`). Records include `a2a.event.kind` where applicable and task, context, message, artifact,
notification, and task-state attributes when available. These identifiers can themselves be
sensitive and must follow the application's logging policy. Records use the current trace and span
context when one is available.

| Event | Trigger | Direction | Kind |
|-------|---------|-----------|------|
| `a2a.authorization` | Server checks access for a task operation | Server inbound | `allow` or `deny` |
| `a2a.negotiation` | Client selects an interface from the agent card | Outbound | Selected protocol binding |
| `a2a.task.request` | Client sends or server receives a message request | Client outbound; server inbound | Omitted |
| `a2a.task.response` | Client receives or server returns a synchronous response | Client inbound; server outbound | Response kind |
| `a2a.task.subscription` | Client sends or server receives a stream subscription | Client outbound; server inbound | Omitted |
| `a2a.task.state_change` | Client receives or server emits a task status update | Client inbound; server outbound | Protocol event kind |
| `a2a.task.artifact` | Client receives or server emits an artifact update | Client inbound; server outbound | Protocol event kind |
| `a2a.task.event` | Client receives or server emits another stream event | Client inbound; server outbound | Protocol event kind |
| `a2a.task.stream.end` | A stream completes, fails, or is cancelled | Client inbound; server outbound | `complete`, `error`, or `cancel` |
| `a2a.notification.subscription` | A push-notification configuration is created or deleted | Server outbound | `created` or `deleted` |
| `a2a.notification.delivery` | A push sender invocation completes | Server outbound | `success`, `failure`, or `error` |
| `a2a.notification.delivery.attempt` | One configured notification delivery finishes or is rejected before sending | Server outbound | `success` or `failure` |

Client and server instrumentation produce separate records for their respective perspectives.
JSON-RPC, HTTP+JSON (including SSE), and gRPC share the same client transport and server request
handler logging paths.

### Authorization Audit Records

`a2a.authorization` records the outcome of every `RequestHandler.authorizeTaskAccess` check. The
`a2a.operation.name` attribute carries the `TaskOperation` being authorized (for example
`GET_TASK`), and `a2a.task.id` is present when the operation is task-scoped. Denials add
`error.type` with the simple class name of the rejection.

The A2A protocol deliberately reports a missing task and an inaccessible task identically so that
callers cannot probe for task existence. `error.type` preserves that distinction server-side, where
it is needed to investigate a denial, so these records must not be exposed to callers.

Records carry no caller identity. Add the principal through a log processor if your audit policy
requires it, and treat the result as personal data.

### Payload Capture and Security

Payload capture is disabled by default even when protocol logging is enabled. When a
`ProtocolPayloadFormatter` is configured, it creates a bounded projection before serialization,
masks common secret field names and bearer or query-string credentials, omits file content and
unknown object types, and then applies the caller's string sanitizer. The default limit is 4096 UTF-8
bytes. Truncated output ends in ` [truncated]` and is plain text rather than guaranteed-valid JSON;
attributes report truncation and, when known, the projected size.

No general-purpose sanitizer can identify every secret in arbitrary text. Use an
application-specific sanitizer, review representative data, and apply access and retention controls
before enabling payload capture. If sanitization fails or returns `null`, the payload is omitted.
The request/response span-extraction system properties are independent and are not sanitized by the
protocol-log formatter.

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
