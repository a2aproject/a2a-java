# A2A Java SDK - Spring Boot REST Example

This directory contains the end-to-end REST demo for the A2A Spring Boot integration.

It is split into two runnable apps:

- `server/` - the A2A agent runtime and REST transport server
- `client/` - a small web app that exercises the server through scenario endpoints and Swagger UI

## What This Demo Shows

- How the Spring Boot A2A server is wired with a dedicated `AgentExecutor` bean
- How a client app fetches the remote `AgentCard`
- How to trigger a blocking request and inspect the returned message
- How to trigger a streaming request and observe task updates
- How to run the whole flow in one call
- How to test the client-side demo manually through Swagger UI

## How To Run

1. Start the server app:

```bash
mvn -pl examples/spring-boot/rest/server -am spring-boot:run
```

2. Wait until the server is available on:

```text
http://localhost:18080
```

3. Start the client app:

```bash
mvn -pl examples/spring-boot/rest/client -am spring-boot:run
```

4. Open Swagger UI for the client:

```text
http://localhost:18081/swagger-ui.html
```

## What To Try

Server-side:

- Open `http://localhost:18080/.well-known/agent-card.json`
- Send a `hello` message to get a direct response
- Send a `stream` message to see the streaming task flow

Client-side:

- `GET /demo/agent-card` to fetch the server card
- `POST /demo/blocking` to run the blocking flow
- `POST /demo/streaming` to run the streaming flow
- `POST /demo/full-flow` to run the full demo end-to-end

Example request body:

```json
{
  "helloMessage": "hello from the Spring Boot REST client",
  "streamMessage": "stream from the Spring Boot REST client",
  "streamingTimeoutSeconds": 15
}
```

## What You Should See

- The server prints A2A runtime startup logs and exposes the REST transport endpoints.
- The client prints the fetched `AgentCard`, then logs the blocking and streaming scenario results.
- In Swagger UI you can replay the demo scenarios without using curl or a custom client.

## Build

```bash
mvn -pl examples/spring-boot/rest -am test
```
