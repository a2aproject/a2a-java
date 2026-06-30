# A2A Java SDK - Spring Boot Server Integration Tests

This module contains integration tests for the Spring Boot REST stack.

## Artifact

- `a2a-java-sdk-spring-boot-server-integration-tests`

## Coverage

- Spring Boot application context startup
- REST endpoint wiring
- request delegation into the A2A runtime
- server-sent events responses
- starter-level dependency behavior

## Purpose

The tests verify the assembled Spring Boot integration rather than individual unit classes only. They are meant to guard the full wiring between:

- runtime auto-configuration
- Servlet auto-configuration
- Spring MVC controller mapping
- A2A request handling

## Test Focus

The integration suite should continue to cover:

- application startup with the starter on the classpath
- bean replacement behavior for runtime and MVC components
- JSON response contract for A2A endpoints
- SSE response contract for streaming endpoints
- request delegation into `RequestHandler`

Example test application setup:

```yaml
a2a:
  executor:
    core-pool-size: 1
    max-pool-size: 1
    keep-alive-seconds: 1
    queue-capacity: 1
```

## Build

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-integration-tests -am test
```
