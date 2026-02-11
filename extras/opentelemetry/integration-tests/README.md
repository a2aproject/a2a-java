# OpenTelemetry Integration Tests (Quarkus-based)

## Overview

This module provides **Quarkus-based integration tests** for OpenTelemetry tracing in the A2A Java SDK, similar to the approach used in the [Quarkus OpenTelemetry quickstart](https://github.com/quarkusio/quarkus/tree/main/integration-tests/opentelemetry-quickstart).

Unlike the previous mock-based tests, these are **real integration tests** that:
- Start an actual Quarkus application
- Expose a REST API with A2A agent endpoints
- Make real HTTP requests
- Validate that OpenTelemetry spans are created correctly

## Architecture

### Components

1. **SimpleAgent** - A basic A2A agent implementation for testing
   - Implements all RequestHandler methods
   - Stores tasks in memory
   - Provides simple echo responses for messages

2. **AgentResource** - JAX-RS REST resource
   - Exposes HTTP endpoints (`/a2a/tasks`, `/a2a/messages`, etc.)
   - Delegates to the RequestHandler
   - Creates ServerCallContext for each request

3. **InstrumentedRequestHandler** - CDI Alternative
   - Wraps SimpleAgent with OpenTelemetry decorator
   - Delegates to the OpenTelemetry decorator for span creation
   - Ensures spans are created for each operation

4. **OpenTelemetryProducer** - CDI bean producer
   - Creates the Tracer from Quarkus OpenTelemetry
   - Produces the OpenTelemetryRequestHandlerDecorator (CDI decorator)
   - Integrates with Quarkus OpenTelemetry extension

## Test Strategy

### Functional Tests (`OpenTelemetryIntegrationTest`)
- Use `@QuarkusTest` annotation
- Make real HTTP requests using REST Assured
- Verify HTTP responses are correct
- Ensure the application behaves correctly end-to-end

### Tracing Tests (`OpenTelemetryTracingTest`)
- Use `InMemorySpanExporter` to capture spans
- Verify that HTTP requests create OpenTelemetry spans
- Validate span names, kinds (CLIENT/SERVER), and status codes
- Check that spans are properly ended

## Current Status

### ✅ Completed
- Project structure and POM configuration
- Quarkus dependencies and plugins (including opentelemetry-sdk-testing)
- SimpleAgentExecutor following reference module pattern
- TestAgentCardProducer with proper JSONRPC interface
- InMemorySpanExporter producer for span validation
- OpenTelemetryIntegrationTest using Client API
- Tests compile and run (service loader issues resolved)

### 🔨 In Progress / Known Issues

1. **Test Execution Timeouts**
   - Tests are timing out during message send operations
   - Error: "Timeout waiting for consumption to complete for task test-task-1"
   - Likely a configuration issue between client (non-streaming) and server (streaming capable)
   - Need to investigate JSONRPC transport configuration

2. **Span Validation**
   - InMemorySpanExporter is configured but needs verification
   - Some tests are not finding expected spans
   - May need to configure span processor to route to InMemorySpanExporter

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
mvn test -Dtest=OpenTelemetryIntegrationTest
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

# Test mode: use in-memory exporter
quarkus.otel.traces.exporter=none
```

### beans.xml
Located at `src/main/resources/META-INF/beans.xml`:
- Enables CDI bean discovery
- Configures alternatives (InstrumentedRequestHandler)

## Next Steps

To complete this integration test module:

1. **Resolve CDI ambiguity**
   - Add `@Named` or custom qualifier to SimpleAgent
   - Or exclude DefaultRequestHandler from test classpath
   - Or use `@Alternative` more effectively

2. **Configure span exporter**
   - Properly wire InMemorySpanExporter into Quarkus OpenTelemetry
   - May need custom OTel SDK configuration

3. **Add more test scenarios**
   - Test error handling and error spans
   - Test streaming operations
   - Test context propagation across services
   - Test span attributes and metadata

4. **Performance testing** (optional)
   - Measure overhead of OpenTelemetry instrumentation
   - Verify spans don't impact performance significantly

## Comparison with Quarkus Quickstart

This implementation follows the same patterns as the Quarkus OpenTelemetry quickstart:
- ✅ Uses `@QuarkusTest` for integration tests
- ✅ Uses REST Assured for HTTP testing
- ✅ Integrates with Quarkus OpenTelemetry extension
- ✅ Uses in-memory span exporter for validation
- ✅ Tests actual HTTP requests, not mocks

Unlike the quickstart which is a standalone app, this module:
- Tests A2A SDK-specific functionality
- Validates OpenTelemetry CDI decorator integration
- Focuses on A2A protocol operations (tasks, messages, etc.)

## References

- [Quarkus OpenTelemetry Guide](https://quarkus.io/guides/opentelemetry)
- [Quarkus OpenTelemetry Quickstart](https://github.com/quarkusio/quarkus/tree/main/integration-tests/opentelemetry-quickstart)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/languages/java/)
