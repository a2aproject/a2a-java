# A2A Java SDK - Spring Boot Server Web MVC

This module provides the REST transport adapter for the A2A SDK.

## Artifact

- `a2a-java-sdk-spring-boot-server-rest`

## Responsibilities

- Expose the HTTP response mapper used by MVC endpoints.
- Expose the Spring MVC controller for A2A HTTP transport.
- Register servlet-specific auto-configuration.
- Centralize HTTP error mapping through `A2ASpringBootMvcExceptionHandler`.

## Behavior

- Activates only in Servlet web applications.
- Requires the runtime beans provided by `spring-boot-server-autoconfigure`.
- Keeps the HTTP paths and payload shapes aligned with the core SDK contract.

Example `application.yml`:

```yaml
a2a:
  executor:
    core-pool-size: 5
    max-pool-size: 50
    keep-alive-seconds: 60
    queue-capacity: 100
```

## Endpoints

The module currently exposes the standard A2A HTTP endpoints implemented by the MVC controller, including:

- `/.well-known/agent-card.json`
- `POST /message:send`
- `POST /message:stream`
- `GET /tasks/{taskId}`
- `GET /tasks`
- `POST /tasks/{taskId}:cancel`
- `POST /tasks/{taskId}:subscribe`
- `POST /tasks/{taskId}/pushNotificationConfigs`
- `GET /tasks/{taskId}/pushNotificationConfigs/{configId}`
- `GET /tasks/{taskId}/pushNotificationConfigs`
- `DELETE /tasks/{taskId}/pushNotificationConfigs/{configId}`
- `GET /extendedAgentCard`

Each endpoint also supports the optional tenant-prefixed form `/{tenant}/...` where applicable.

## Extension Point

Applications can replace the default mapper or controller by defining their own Spring beans.

Example `@Configuration`:

```java
@Configuration(proxyBeanMethods = false)
public class CustomA2ARestConfiguration {

    @Bean
    public A2ASpringBootHttpResponseMapper a2aSpringBootHttpResponseMapper() {
        return new A2ASpringBootHttpResponseMapper();
    }
}
```

## Build

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest -am test
```
