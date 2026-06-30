# A2A Java SDK - Spring Boot Server REST Starter

This module is a dependency-only starter for Spring Boot REST/MVC applications.

## Artifact

- `a2a-java-sdk-spring-boot-starter-server-rest`

## Transitive Dependencies

- `a2a-java-sdk-spring-boot-server-autoconfigure`
- `a2a-java-sdk-spring-boot-server-rest`
- `spring-boot-starter-web`

## Purpose

Add this starter to get the full Spring Boot server integration with a single dependency.

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
```

## Application Requirements

The application should still provide:

- `AgentCard`
- `AgentExecutor`

Those beans drive the agent identity and runtime execution logic.

## Build

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-starter-server-rest -am test
```
