# A2A Java SDK - Spring Boot Examples

This directory contains runnable Spring Boot examples for the A2A Java SDK.

## Layout

- `rest/`
  - REST transport examples.
  - Currently includes runnable server and client examples.
  - `server/`
    - Runnable Spring Boot REST server example.
  - `client/`
    - Runnable Spring Boot REST client example.
- `jsonrpc/`
  - Reserved for future JSON-RPC examples.
  - `server/`
    - Reserved for a future Spring Boot JSON-RPC server example.
  - `client/`
    - Reserved for a future Spring Boot JSON-RPC client example.
- `grpc/`
  - Reserved for future gRPC examples.
  - `server/`
    - Reserved for a future Spring Boot gRPC server example.
  - `client/`
    - Reserved for a future Spring Boot gRPC client example.

## Current Example

- `rest/server`
  - Demonstrates a runnable A2A server built with the Spring Boot REST starter.
  - Exposes the standard A2A REST endpoints.
- `rest/client`
  - Demonstrates a runnable Spring Boot client that calls the server and logs the response.

## Run

The REST example can be started in two terminals:

```bash
mvn -pl examples/spring-boot/rest/server -am spring-boot:run
```

```bash
mvn -pl examples/spring-boot/rest/client -am spring-boot:run
```

The server listens on `http://localhost:18080` and the client connects to that URL by default.

## Endpoint Preview

Once running, the example exposes:

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

## Next Transport Examples

The example tree is transport-first so `jsonrpc` and `grpc` can be added later as sibling transport roots.

## Build

```bash
mvn -pl examples/spring-boot -am test
```
