# OpenTelemetry Integration Tests (Quarkus-based)

## Overview

This module provides **Quarkus-based integration tests** for OpenTelemetry tracing in the A2A Java SDK.

The tests start a real Quarkus application, make HTTP requests via the A2A client SDK, and validate that the `OpenTelemetryRequestHandlerDecorator` (from the `opentelemetry-server` module) creates spans with the expected attributes.

## Architecture

### Application Components

- **SimpleAgentExecutor** — A basic `AgentExecutor` that echoes back the user's message and completes the task immediately.
- **TestAgentCardProducer** — CDI producer for the test agent's `AgentCard` (JSON-RPC interface on port 8081, streaming enabled).
- **A2ATestRoutes** — Vert.x routes exposing test utility endpoints for task/queue manipulation (`/test/task/*`, `/test/queue/*`) and span inspection (`/export`, `/reset`, `/hello`). Also contains the `InMemorySpanExporterProducer` that captures spans in memory for assertion.
- **TestUtilsBean** — CDI bean wrapping `TaskStore` and `QueueManager` for direct test manipulation (save/get/delete tasks, create queues, enqueue events).

### Test Classes

- **OpenTelemetryTest** (`@QuarkusTest`) — Smoke test: hits the `/hello` endpoint and verifies a span is created.
- **OpenTelemetryA2ATest** (`@QuarkusTest`) — JVM-mode tests for A2A protocol operations (getTask, listTasks, cancelTask) verifying server-side spans, span attributes (`gen_ai.agent.a2a.operation.name`, `gen_ai.agent.a2a.task_id`), and span metadata (kind, status, parent).
- **OpenTelemetryA2AIT** (`@QuarkusIntegrationTest`) — Same tests running in native/integration mode.
- **OpenTelemetryA2ABaseTest** — Abstract base class containing the shared test logic. Uses the A2A `Client` SDK to make JSON-RPC calls and `java.net.HttpClient` to interact with the test utility endpoints.
- **BaseTest** — Provides a `getSpans()` helper that fetches captured spans from the `/export` endpoint via REST Assured.

### Span Capture Flow

```
A2A Client SDK call (e.g., getTask)
    ↓
Quarkus HTTP server
    ↓
OpenTelemetryRequestHandlerDecorator creates SERVER span
    ↓
RequestHandler processes request
    ↓
InMemorySpanExporter captures completed span
    ↓
Test calls GET /export → asserts on span attributes
```

## Running the Tests

### Prerequisites
```bash
# Build all A2A SDK modules first
mvn clean install -DskipTests
```

### Run Integration Tests
```bash
# From the root
mvn verify -pl extras/opentelemetry/integration-tests -am

# Or from the integration-tests directory
mvn clean verify
```

### Run a Specific Test
```bash
mvn test -pl extras/opentelemetry/integration-tests -Dtest=OpenTelemetryA2ATest
mvn test -pl extras/opentelemetry/integration-tests -Dtest=OpenTelemetryTest
```

## Configuration

### `src/main/resources/application.properties`
```properties
quarkus.http.port=8081

quarkus.otel.sdk.disabled=false
quarkus.otel.traces.enabled=true
quarkus.otel.metrics.enabled=false
quarkus.otel.logs.enabled=false
quarkus.otel.instrument.vertx-http=false

quarkus.otel.bsp.schedule.delay=0
quarkus.otel.bsp.export.timeout=5s

quarkus.otel.service.name=a2a-opentelemetry-integration-test
quarkus.otel.propagators=tracecontext
```

Key choices:
- **Vert.x HTTP instrumentation disabled** (`instrument.vertx-http=false`) to avoid Quarkus HTTP spans polluting assertions — only the A2A decorator spans are captured.
- **Batch span processor delay set to 0** (`bsp.schedule.delay=0`) so spans are exported immediately for test assertions.
- **Metrics and logs disabled** to keep the test focused on tracing.

## References

- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/languages/java/)
