# OpenTelemetry Integration for A2A

This module provides OpenTelemetry observability integration for A2A servers and clients, including distributed tracing and context propagation.

## Features

- **Distributed Tracing**: Automatic span creation for all A2A protocol methods (both client and server)
- **Context Propagation**: W3C Trace Context injection into outbound client requests
- **Request/Response Logging**: Optional extraction of request and response data into span attributes
- **Error Tracking**: Automatic error status and error type attributes on failures

## Modules

### `opentelemetry-common`
Constants and span attribute names shared across client and server modules. See `A2AObservabilityNames` for the full list of attribute keys.

### `opentelemetry-client`
Client-side distributed tracing. Wraps `ClientTransport` to create spans for every A2A operation. Discovered automatically via `ServiceLoader` when a `Tracer` is present in the transport configuration parameters.

### `opentelemetry-client-propagation`
Injects W3C Trace Context headers into outbound client requests using `OpenTelemetry.getPropagators().getTextMapPropagator()`. Discovered automatically via `ServiceLoader` when an `OpenTelemetry` instance is present in the transport configuration parameters.

### `opentelemetry-server`
CDI decorator (`OpenTelemetryRequestHandlerDecorator`) that wraps `RequestHandler` to create server-side spans. Automatically activated when the module JAR is on the classpath (via the bundled `beans.xml`).

### `opentelemetry-integration-tests`
Quarkus-based integration tests for OpenTelemetry functionality.

## Server Usage

### Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-extras-opentelemetry-server</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

The `OpenTelemetryRequestHandlerDecorator` is registered automatically via the `beans.xml` bundled in the module JAR. No additional configuration is required.

> **Note**: The decorator relies on the runtime (e.g., Quarkus OpenTelemetry extension) to extract trace context from incoming HTTP requests. It does not perform context extraction itself — it creates a new span within the already-established trace context.

### Request/Response Extraction

Optionally include full request/response data as span attributes. These are controlled by **JVM system properties** (not application config), read via `Boolean.getBoolean()`:

```bash
# Pass as JVM system properties (-D flags)
-Dorg.a2aproject.sdk.server.extract.request=true
-Dorg.a2aproject.sdk.server.extract.response=true
```

> **Warning**: Extracting request/response data may expose sensitive information in traces. Use with caution in production environments.

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

### Streaming Methods

For streaming methods (`onMessageSendStream`, `onSubscribeToTask`), the server-side span covers only the creation of the `Flow.Publisher`, not the actual streaming of events. The span is started and ended synchronously before events are emitted, and the response attribute is set to `"Stream publisher created"`. This means the span duration does not reflect the actual duration of the streaming operation.

On the client side, streaming is handled differently: the `OpenTelemetryClientTransport` creates child spans linked to the parent for each streaming event and error callback, providing per-event tracing.

## Client Usage

### Instrumentation (Tracing Spans)

Add the client tracing module:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-extras-opentelemetry-client</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

Enable tracing by adding a `Tracer` instance to the transport configuration parameters:

```java
import static org.a2aproject.sdk.extras.opentelemetry.client.OpenTelemetryClientTransportWrapper.OTEL_TRACER_KEY;

config.setParameters(Map.of(
    OTEL_TRACER_KEY, openTelemetry.getTracer("my-service")
));
```

### Client protocol logs

Protocol logs are disabled unless an OpenTelemetry `Logger` is supplied together with the tracer.
The optional logger records DEBUG metadata for message requests, responses, stream events, and stream
termination. Configure an OpenTelemetry logs SDK and exporter to collect these records. Streaming
records use the captured request context even if the callback runs on a different thread.
Set `OTEL_LOG_SEVERITY_KEY` to an OpenTelemetry `Severity` to override the default DEBUG severity.

Payload capture is separately opt-in by adding `OTEL_PAYLOAD_FORMATTER_KEY` with a
`ProtocolPayloadFormatter`. For example:

```java
import org.a2aproject.sdk.extras.opentelemetry.ProtocolPayloadFormatter;
import org.a2aproject.sdk.extras.opentelemetry.client.OpenTelemetryClientTransportWrapper;

config.setParameters(Map.of(
    OTEL_TRACER_KEY, openTelemetry.getTracer("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_LOGGER_KEY, openTelemetry.getLogsBridge().get("my-service"),
    OpenTelemetryClientTransportWrapper.OTEL_PAYLOAD_FORMATTER_KEY,
        new ProtocolPayloadFormatter(ProtocolPayloadFormatter.DEFAULT_MAX_BYTES, value -> value)
));
```

The formatter masks common secret field names, URL/URI fields, bearer credentials, and common
query-string token/key values; omits file parts and unknown object types; and applies the caller's
additional string sanitizer. It cannot identify every secret
in arbitrary free text: provide an application-specific sanitizer and review the data before enabling
payload capture. The body is limited to the configured UTF-8 byte count; truncated bodies end in
` [truncated]` and are plain text rather than necessarily valid JSON. Attributes indicate truncation
and, when the full projection fits within the traversal budget, its projected byte count. Large
strings are reduced to a bounded prefix before invoking the sanitizer, and traversal stops when its
budget is exhausted; in that case the projected size is unknown and the size attribute is omitted.
Sanitization failures omit the payload.
These controls apply to protocol logs only. The existing opt-in request and response span
attributes use separate system properties and are not sanitized by the log formatter.

### Server protocol logs

Server protocol logs are disabled unless the application provides a CDI bean of type
`ProtocolLogEmitter`. The request handler decorator records incoming message requests and
subscriptions, outgoing responses and streaming events, and stream completion, failure, or
cancellation. Supply a formatter only when payload capture is wanted:

```java
@Produces
@Singleton
ProtocolLogEmitter protocolLogEmitter(OpenTelemetry openTelemetry) {
    return new ProtocolLogEmitter(openTelemetry.getLogsBridge().get("my-service"));
}
```

The application must configure a logs SDK and exporter. Logs are observability data, not a durable
audit trail. Authorization decisions require instrumentation by the application.
The push sender emits one aggregate delivery outcome for the sender invocation: a successful result
can also mean that no notifications were configured. The CDI-managed default HTTP sender additionally
reports each configured delivery outcome, including requests rejected before an HTTP attempt,
without recording the destination URL or credentials. Directly
constructed senders do not receive the CDI observer.

### Protocol log event schema

The emitter records DEBUG events with `a2a.event.name`, `a2a.operation.name`,
`a2a.log.direction` (`inbound` or `outbound`), and, where applicable, `a2a.event.kind`.
Task, context, message, artifact IDs and task state are included when available; these identifiers
may themselves be sensitive and should be governed by the application's logging policy.
The OpenTelemetry record contains the current trace and span context when one is available.
Client and server instrumentation produce separate events for their respective perspectives.
Request and streaming protocol logging wrap the client transport SPI and the server request handler,
so JSON-RPC, HTTP+JSON (including SSE), and gRPC share the same logging path. Transport-specific
implementations do not emit or transform these records.

| Event | Trigger | Direction | Kind |
| --- | --- | --- | --- |
| `a2a.task.request` | Client sends or server receives a message request | client outbound; server inbound | omitted |
| `a2a.negotiation` | Client selects an interface from the agent card | outbound | selected protocol binding |
| `a2a.task.response` | Client receives or server returns a synchronous response | client inbound; server outbound | response kind |
| `a2a.task.subscription` | Client sends or server receives a stream subscription | client outbound; server inbound | omitted |
| `a2a.task.state_change` | Client receives or server emits a task status update | client inbound; server outbound | protocol event kind |
| `a2a.task.artifact` | Client receives or server emits an artifact update | client inbound; server outbound | protocol event kind |
| `a2a.task.event` | Client receives or server emits another streaming event | client inbound; server outbound | protocol event kind |
| `a2a.task.stream.end` | Streaming callback terminates | client inbound; server outbound | `complete`, `error`, or server-side `cancel` |
| `a2a.notification.subscription` | Server completes a create or delete config operation | outbound | `created` or `deleted` |
| `a2a.notification.delivery` | Push sender finishes or fails | outbound | `success`, `failure`, or `error` |
| `a2a.notification.delivery.attempt` | Default HTTP sender completes or rejects an individual configured delivery | outbound | `success` or `failure` |
| `a2a.authorization` | Server checks access for a task operation | inbound | `allow` or `deny` |

`a2a.authorization` carries the `TaskOperation` in `a2a.operation.name`, `a2a.task.id` for
task-scoped operations, and `error.type` with the simple class name of the rejection on a denial.
The protocol reports a missing task and an inaccessible task identically so callers cannot probe
for task existence; `error.type` keeps that distinction server-side and must not be returned to
callers. These records carry no caller identity.

Event and payload capture can produce high log volumes for streams. Configure filtering and
asynchronous exporting in the OpenTelemetry SDK or collector. The negotiation event covers the
interface selected by `ClientBuilder`; it deliberately omits endpoint URLs and does not include
the entire agent card. Application-specific negotiation requires instrumentation by the application.

### Context Propagation (W3C Trace Headers)

Add the propagation module:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-extras-opentelemetry-client-propagation</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

Enable propagation by adding an `OpenTelemetry` instance to the transport configuration parameters:

```java
import static org.a2aproject.sdk.extras.opentelemetry.client.propagation.OpenTelemetryClientPropagatorTransportWrapper.OTEL_OPEN_TELEMETRY_KEY;

config.setParameters(Map.of(
    OTEL_TRACER_KEY, openTelemetry.getTracer("my-service"),
    OTEL_OPEN_TELEMETRY_KEY, openTelemetry
));
```

Both wrappers are discovered automatically via `ServiceLoader` and activate only when their respective keys are present in the configuration parameters. The client wrapper (priority 600) runs after the propagation wrapper (priority 500), so trace context headers are injected before the tracing span is created.

## Architecture

### Server Request Handler Decoration

```
Client Request
    ↓
Runtime (e.g., Quarkus OTel) extracts trace context from HTTP headers
    ↓
OpenTelemetryRequestHandlerDecorator creates span within trace context
    ↓
Default RequestHandler
    ↓
Agent Execution
    ↓
Response
```

### Client Transport Wrapping

```
Application calls ClientTransport method
    ↓
OpenTelemetryClientPropagatorTransport injects W3C trace context headers (priority 500)
    ↓
OpenTelemetryClientTransport creates tracing span (priority 600)
    ↓
Underlying transport (JSON-RPC, gRPC, REST)
    ↓
Response
```

## Testing

The module includes:

- `OpenTelemetryRequestHandlerDecoratorTest`: Tests for span creation and error handling

Run tests:
```bash
mvn test -pl extras/opentelemetry/server
```

## Troubleshooting

### Context Not Propagating

**Symptom**: Spans in async tasks are not linked to parent spans.

**Solution**: Ensure your runtime provides trace context propagation for async boundaries. For Quarkus, this is handled automatically by MicroProfile Context Propagation. The context-aware `ManagedExecutor` that propagates trace context across async boundaries is provided by the reference server module (`reference/common`), not this OpenTelemetry module.

### Performance Impact

**Symptom**: Increased latency with OpenTelemetry enabled.

**Solution**:
- Disable request/response extraction in production
- Configure sampling rate to reduce trace volume
- Ensure your OpenTelemetry collector is properly sized

## Best Practices

1. **Sampling**: Configure appropriate sampling rates for production environments
2. **Sensitive Data**: Disable request/response extraction if handling sensitive data
3. **Resource Attributes**: Add service name and version as resource attributes
4. **Collector Configuration**: Use batch processors to reduce network overhead
5. **Monitoring**: Monitor the OpenTelemetry collector's health and performance

## Dependencies

- OpenTelemetry API
- A2A Server Common (for server module)
- A2A Client Transport SPI (for client modules)

## See Also

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [MicroProfile Telemetry Specification](https://github.com/eclipse/microprofile-telemetry)
- [Helloworld Example with OpenTelemetry](../../examples/helloworld/)
