# A2A Java SDK - Spring Boot Server REST Example

This example shows how to run an A2A server with the Spring Boot REST starter.

## What It Demonstrates

- Spring Boot auto-configuration from the SDK starter
- A minimal `AgentCard` bean
- A dedicated `AgentExecutor` Spring component
- REST transport endpoints exposed by the server module
- A direct message response for `hello`
- A streaming task flow for `stream`

## Run

From the repository root, run:

```bash
mvn -pl examples/spring-boot/rest/server -am spring-boot:run
```

The example listens on `http://localhost:18080`.

## Build

```bash
mvn -pl examples/spring-boot/rest/server -am test
```

## How To Test

Use a browser or HTTP client and open:

```text
http://localhost:18080/.well-known/agent-card.json
```

Then send messages to the A2A REST transport endpoints:

- `POST /message:send` for a blocking response
- `POST /message:stream` for a streaming task flow
- `POST /tasks/{taskId}:subscribe` to observe task updates

The server demo is intentionally minimal:

- `hello` returns a direct message
- `stream` returns a short streaming task flow
- the executor is a dedicated Spring component in `SpringBootRestServerAgentExecutor`

## Available Endpoints

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

Tenant-prefixed variants are also available for the transport endpoints and extended card:

- `POST /{tenant}/message:send`
- `POST /{tenant}/message:stream`
- `GET /{tenant}/tasks/{taskId}`
- `GET /{tenant}/tasks`
- `POST /{tenant}/tasks/{taskId}:cancel`
- `POST /{tenant}/tasks/{taskId}:subscribe`
- `POST /{tenant}/tasks/{taskId}/pushNotificationConfigs`
- `GET /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}`
- `GET /{tenant}/tasks/{taskId}/pushNotificationConfigs`
- `DELETE /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}`
- `GET /{tenant}/extendedAgentCard`

## Configuration

Example `application.yml`:

```yaml
server:
  port: 18080
a2a:
  executor:
    core-pool-size: 2
    max-pool-size: 4
    keep-alive-seconds: 60
    queue-capacity: 32
  blocking:
    agent-timeout-seconds: 30
    consumption-timeout-seconds: 5
```
