# A2A Java SDK - Spring Boot REST Client Example

This example shows how to interact with the Spring Boot REST server example through a small web app with Swagger UI.

## What It Demonstrates

- Fetching the server `AgentCard`
- Creating an A2A REST client from the SDK
- Running a blocking demo flow through a controller endpoint
- Running a streaming demo flow through a controller endpoint
- Running a full scenario that combines both flows
- Inspecting and replaying the demo through Swagger UI

## Run

Start the server example first:

```bash
mvn -pl examples/spring-boot/rest/server -am spring-boot:run
```

Then run the client example in another terminal:

```bash
mvn -pl examples/spring-boot/rest/client -am spring-boot:run
```

The client listens on `http://localhost:18081` by default.

Swagger UI is available at:

```text
http://localhost:18081/swagger-ui.html
```

## How To Test

Use Swagger UI or curl against the client demo endpoints:

- `GET /demo/agent-card` to fetch the server card and verify connectivity
- `POST /demo/blocking` to run the direct request path
- `POST /demo/streaming` to run the streaming task path
- `POST /demo/full-flow` to run the full scenario in one call

The full flow is the recommended manual test:

1. Fetch the remote `AgentCard`
2. Send a blocking message
3. Send a streaming message
4. Read the structured JSON result returned by the client demo

Example request body:

```json
{
  "helloMessage": "hello from the Spring Boot REST client",
  "streamMessage": "stream from the Spring Boot REST client",
  "streamingTimeoutSeconds": 15
}
```

What you should see:

- `blocking` returns a direct response message and a short event trace
- `streaming` returns task events and the final message
- `full-flow` returns a combined report containing the agent card and both scenario results

## Demo Endpoints

- `GET /demo/agent-card`
- `POST /demo/blocking`
- `POST /demo/streaming`
- `POST /demo/full-flow`

Request body example:

```json
{
  "helloMessage": "hello from the Spring Boot REST client",
  "streamMessage": "stream from the Spring Boot REST client",
  "streamingTimeoutSeconds": 15
}
```

## Build

```bash
mvn -pl examples/spring-boot/rest/client -am test
```

## Configuration

Example `application.yml`:

```yaml
server:
  port: 18081

a2a:
  example:
    server-url: http://localhost:18080
    hello-message: hello from the Spring Boot REST client
    stream-message: stream from the Spring Boot REST client
    streaming-timeout-seconds: 15
```
