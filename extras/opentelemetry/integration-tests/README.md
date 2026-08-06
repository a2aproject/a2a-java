# OpenTelemetry Integration Tests (Quarkus-based)

## Overview

This module provides **Quarkus-based integration tests** for OpenTelemetry tracing in the A2A Java SDK, similar to the approach used in the [Quarkus OpenTelemetry quickstart](https://github.com/quarkusio/quarkus/tree/main/integration-tests/opentelemetry-quickstart).

The tests start an actual Quarkus application, make real HTTP requests, and validate that OpenTelemetry spans are created correctly.

## Architecture

### Components

1. **SimpleAgentExecutor** - A basic A2A `AgentExecutor` implementation for testing
   - Echoes the user's message back and completes the task immediately
   - Supports cancellation

2. **A2ATestRoutes** - Vert.x Web test routes
   - Exposes test utilities (`/test/task`, `/test/queue/...`)
   - Exposes span inspection endpoints (`/export`, `/reset`)
   - Provides the `InMemorySpanExporter` CDI bean used to capture spans

3. **TestUtilsBean** - Test helper that gives direct access to the `TaskStore` and `QueueManager`

4. **TestAgentCardProducer** - Produces the `AgentCard` used by the tests

### Tracing

The server request handling is instrumented by the
`OpenTelemetryRequestHandlerDecorator` (from `a2a-java-sdk-opentelemetry-server`),
which creates a span for every A2A protocol method. Spans are exported to the
`InMemorySpanExporter` CDI bean and inspected by the tests through the `/export`
route.

## Test Strategy

- **OpenTelemetryA2ATest** (`@QuarkusTest`): JVM-mode tests that use the A2A client API to call the server
  - `testGetTaskCreatesSpans` - verifies a SERVER span is created for `getTask`
  - `testListTasksCreatesSpans` - verifies a SERVER span is created for `listTasks`
  - `testCancelTaskCreatesSpans` - verifies a SERVER span is created for `cancelTask`
  - `testSpanAttributes` - verifies span attributes (operation name, task ID, service name)

- **OpenTelemetryA2AIT** (`@QuarkusIntegrationTest`): packaged-application mode running the same test suite

- **OpenTelemetryTest** (`@QuarkusTest`): verifies that a span is created for the `/hello` route

All tests currently pass in both JVM and packaged (integration) modes.

## Running the Tests

### Prerequisites
```bash
# Build all A2A SDK modules first
mvn clean install -DskipTests
```

### Run Integration Tests
```bash
# From the integration-tests directory
mvn clean verify

# Or from the root
mvn verify -pl extras/opentelemetry/integration-tests -am
```

### Run Specific Test
```bash
mvn test -Dtest=OpenTelemetryA2ATest
```

## Configuration

### Application Properties
- `src/main/resources/application.properties` - Runtime configuration
- `src/test/resources/application.properties` - Test-specific configuration

Key settings:
```properties
# OpenTelemetry
quarkus.otel.sdk.disabled=false
quarkus.otel.traces.enabled=true
quarkus.otel.service.name=a2a-opentelemetry-integration-test

# In-memory exporter (CDI bean produced by A2ATestRoutes)
quarkus.otel.traces.exporter=cdi
```

### beans.xml
Located at `src/main/resources/META-INF/beans.xml`:
- Enables CDI bean discovery

## References

- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [Quarkus OpenTelemetry Quickstart](https://github.com/quarkusio/quarkus/tree/main/integration-tests/opentelemetry-quickstart)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/languages/java/)
