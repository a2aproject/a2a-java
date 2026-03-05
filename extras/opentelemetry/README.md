# OpenTelemetry Integration for A2A

This module provides OpenTelemetry observability integration for A2A servers, including distributed tracing, metrics, and context propagation across asynchronous boundaries.

## Features

- **Distributed Tracing**: Automatic span creation for all A2A protocol methods
- **Context Propagation**: OpenTelemetry trace context propagation across async operations
- **Request/Response Logging**: Optional extraction of request and response data into spans
- **Error Tracking**: Automatic error status and error type attributes on failures

## Modules

### `opentelemetry-common`
Common utilities and constants shared across OpenTelemetry modules.

### `opentelemetry-client`
OpenTelemetry integration for A2A clients.

### `opentelemetry-client-propagation`
Context propagation support for A2A clients.

### `opentelemetry-server`
OpenTelemetry integration for A2A servers, including the context-aware executor.

### `opentelemetry-integration-tests`
Integration tests for OpenTelemetry functionality.

## Usage

### Basic Setup

Add the OpenTelemetry server module to your dependencies:

```xml
<dependency>
    <groupId>io.a2a</groupId>
    <artifactId>a2a-extras-opentelemetry-server</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

### Context-Aware Async Executor

The `AsyncManagedExecutorProducer` provides a `ManagedExecutor` that automatically propagates OpenTelemetry trace context across asynchronous boundaries. This ensures that spans created in async tasks are properly linked to their parent spans.

#### How It Works

When the OpenTelemetry server module is included, the `AsyncManagedExecutorProducer` automatically replaces the default `AsyncExecutorProducer` using CDI alternatives:

- **Priority 20**: Takes precedence over the default executor producer (priority 10)
- **Automatic Activation**: No configuration needed - just include the module
- **Context Propagation**: Uses MicroProfile Context Propagation to maintain trace context

#### Configuration

The `ManagedExecutor` is container-managed and configured through your runtime environment:

**Quarkus:**
```properties
# Configure the managed executor pool
quarkus.thread-pool.core-threads=10
quarkus.thread-pool.max-threads=50
quarkus.thread-pool.queue-size=100
```

**Other Runtimes:**
Consult your MicroProfile Context Propagation implementation documentation for configuration options.

> **Note**: Unlike the default `AsyncExecutorProducer`, the `AsyncManagedExecutorProducer` does not use the `a2a.executor.*` configuration properties. Pool sizing is controlled by the container's ManagedExecutor configuration.

#### Example

```java
@ApplicationScoped
public class MyAgent implements Agent {
    
    @Inject
    @Internal
    Executor executor;  // Automatically uses ManagedExecutor with context propagation
    
    @Override
    public void execute(RequestContext context, AgentEmitter emitter) {
        // Current span context is automatically propagated
        executor.execute(() -> {
            // This code runs in a different thread but maintains the trace context
            Span currentSpan = Span.current();
            currentSpan.addEvent("Processing in async task");
            
            // Do async work...
        });
    }
}
```

### Request/Response Extraction

Enable request and response data extraction in spans:

```properties
# Extract request parameters into span attributes
a2a.opentelemetry.extract-request=true

# Extract response data into span attributes
a2a.opentelemetry.extract-response=true
```

> **Warning**: Extracting request/response data may expose sensitive information in traces. Use with caution in production environments.

### Span Attributes

The following attributes are automatically added to spans:

- `genai.request`: Request parameters (if extraction enabled)
- `genai.response`: Response data (if extraction enabled)
- `error.type`: Error message (on failures)

## Architecture

### Request Handler Decoration

The `OpenTelemetryRequestHandlerDecorator` wraps the default request handler and creates spans for each A2A protocol method:

```
Client Request
    ↓
OpenTelemetryRequestHandlerDecorator
    ↓ (creates span)
Default RequestHandler
    ↓
Agent Execution (with context propagation)
    ↓
Response
```

### Context Propagation Flow

```
HTTP Request (with trace headers)
    ↓
OpenTelemetry extracts context
    ↓
Span created for A2A method
    ↓
ManagedExecutor propagates context
    ↓
Async agent execution (maintains trace context)
    ↓
Response (with trace headers)
```

## Testing

The module includes comprehensive unit tests:

- `AsyncManagedExecutorProducerTest`: Tests for the context-aware executor producer
- `OpenTelemetryRequestHandlerDecoratorTest`: Tests for span creation and error handling

Run tests:
```bash
mvn test -pl extras/opentelemetry/server
```

## Troubleshooting

### Context Not Propagating

**Symptom**: Spans in async tasks are not linked to parent spans.

**Solution**: Ensure the OpenTelemetry server module is included and the `ManagedExecutor` is being injected correctly. Check logs for:
```
Initializing OpenTelemetry-aware ManagedExecutor for async operations
```

### ManagedExecutor Not Available

**Symptom**: `IllegalStateException: ManagedExecutor not injected - ensure MicroProfile Context Propagation is available`

**Solution**: Ensure your runtime provides MicroProfile Context Propagation support. For Quarkus, add:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-context-propagation</artifactId>
</dependency>
```

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

- MicroProfile Telemetry 2.0.1+
- MicroProfile Context Propagation 1.3+
- OpenTelemetry API
- A2A Server Common

## See Also

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [MicroProfile Telemetry Specification](https://github.com/eclipse/microprofile-telemetry)
- [MicroProfile Context Propagation](https://github.com/eclipse/microprofile-context-propagation)
