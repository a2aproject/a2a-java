# A2A Java SDK - Spring Boot Server Integration

This directory is the server-side Spring Boot integration tree.

## Layout

- `spring-boot-server-autoconfigure/`
  - Shared runtime auto-configuration for all Spring Boot transports.
  - Works without Servlet APIs.
- `rest/`
  - Transport aggregator for the REST/MVC server integration.
  - Contains the runtime controller, starter, integration tests, and the REST TCK SUT.
- `jsonrpc/`
  - Placeholder aggregator for the future JSON-RPC server integration.
- `grpc/`
  - Placeholder aggregator for the future gRPC server integration.

## Recommended Dependency

For Spring Boot applications that use the REST transport, depend on:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-spring-boot-starter-server-rest</artifactId>
</dependency>
```

## Runtime Properties

The server runtime is configured with `a2a.*` properties.

Example `application.yml`:

```yaml
a2a:
  executor:
    core-pool-size: 5
    max-pool-size: 50
    keep-alive-seconds: 60
    queue-capacity: 100
  blocking:
    agent-timeout-seconds: 30
    consumption-timeout-seconds: 5
  agent-card:
    cache:
      max-age: 3600
```

## Required Application Beans

The starter wires the runtime and transport infrastructure, but the application still needs to provide:

- `AgentCard`
- `AgentExecutor`

## Override Points

Applications can replace these beans when custom behavior is needed:

- `TaskStore`
- `PushNotificationConfigStore`
- `PushNotificationSender`
- `RequestHandler`
- `A2ASpringBootHttpResponseMapper`
- `A2ASpringBootMvcController`

## Build

```bash
mvn -pl integrations/spring-boot/server -am test
```

## TCK SUT

Run the REST TCK end-to-end with:

```bash
bash ./scripts/run-spring-boot-rest-tck.sh
```

If you want to run only the SUT locally, use:

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am spring-boot:run
```

The SUT listens on `http://localhost:9999` by default so the external A2A TCK can target it directly.

For CI, see `.github/workflows/run-spring-boot-rest-tck.yml`.
