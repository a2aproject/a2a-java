# A2A Java SDK - Spring Boot Server AutoConfiguration

This module provides Spring Boot auto-configuration for the A2A server runtime layer.

## Artifact

- `a2a-java-sdk-spring-boot-server-autoconfigure`

## Responsibilities

- Bind `a2a.*` configuration properties.
- Adapt Spring `Environment` to the A2A `A2AConfigProvider` contract.
- Provide runtime beans for the server core:
  - `DefaultValuesConfigProvider`
  - `A2AConfigProvider`
  - `TaskStore`
  - `MainEventBus`
  - `QueueManager`
  - `MainEventBusProcessor`
  - `PushNotificationConfigStore`
  - `PushNotificationSender`
  - internal executor beans
  - `RequestHandler`

## Configuration Properties

The module binds these `a2a.*` properties:

| Property | Default | Purpose |
| --- | --- | --- |
| `a2a.executor.core-pool-size` | `5` | Core size of the internal executor. |
| `a2a.executor.max-pool-size` | `50` | Maximum size of the internal executor. |
| `a2a.executor.keep-alive-seconds` | `60` | Idle timeout for extra executor threads. |
| `a2a.executor.queue-capacity` | `100` | Queue size for the internal executor. |
| `a2a.blocking.agent-timeout-seconds` | `30` | Timeout for agent-side blocking operations. |
| `a2a.blocking.consumption-timeout-seconds` | `5` | Timeout for event consumption operations. |
| `a2a.agent-card.cache.max-age` | `3600` | Agent card cache max age in seconds. |

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

## Bean Overrides

Application beans override the defaults when they are present in the Spring context.

### Runtime beans

- `TaskStore`
- `MainEventBus`
- `QueueManager`
- `MainEventBusProcessor`
- `PushNotificationConfigStore`
- `PushNotificationSender`
- `RequestHandler`

### Executor beans

- `a2aInternalExecutor`
- `a2aEventConsumerExecutor`

### Configuration provider chain

`A2AConfigProvider` reads from the Spring `Environment` first and falls back to the classpath defaults loaded by `DefaultValuesConfigProvider`.

## Notes

- This module does not depend on Servlet APIs.
- It is safe to use in non-web Spring Boot applications.
- `RequestHandler` is created only when the application provides an `AgentExecutor`.
- The module registers its auto-configuration through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Build

```bash
mvn -pl integrations/spring-boot/server/spring-boot-server-autoconfigure -am test
```
