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
