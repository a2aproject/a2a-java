---
title: A2A Client Guide
description: Communicate with A2A-compliant agents using the A2A Java SDK client.
layout: page
---

# A2A Client

The A2A Java SDK provides a Java client for communicating with any A2A-compliant agent. Supports JSON-RPC 2.0, gRPC, and HTTP+JSON/REST transports.

## 1. Add the Client Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases -->
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

The client artifact includes the JSON-RPC transport by default. For gRPC or REST, add the corresponding transport:

```xml
<!-- gRPC transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-client-transport-grpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- HTTP+JSON/REST transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-client-transport-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

## 2. Create a Client

```java
// Resolve the agent card from the server
AgentCard agentCard = A2ACardResolver.builder()
        .baseUrl("http://localhost:1234")
        .build()
        .getAgentCard();

// Configure accepted output modes
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

// Define event consumers
List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
    (event, card) -> {
        if (event instanceof MessageEvent messageEvent) {
            // handle message
        } else if (event instanceof TaskEvent taskEvent) {
            // handle task
        } else if (event instanceof TaskUpdateEvent updateEvent) {
            // handle task update
        }
    }
);

// Build the client
Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .addConsumers(consumers)
        .streamingErrorHandler(error -> { /* handle errors */ })
        .build();
```

## 3. Send Messages

```java
// Send a text message (streaming used automatically if supported)
Message message = A2A.toUserMessage("tell me a joke");
client.sendMessage(message);

// Send with per-call custom consumers
client.sendMessage(message, customConsumers, customErrorHandler);

// Send with a call context
client.sendMessage(message, clientCallContext);
```

## Task Management

```java
// Get task state
Task task = client.getTask(new TaskQueryParams("task-1234"));
Task task = client.getTask(new TaskQueryParams("task-1234", 10)); // with history limit

// Cancel a task
Task cancelled = client.cancelTask(new CancelTaskParams("task-1234"));

// Subscribe to an ongoing task
client.subscribeToTask(new TaskIdParams("task-1234"));
client.subscribeToTask(taskIdParams, customConsumers, customErrorHandler);

// Retrieve the server agent card
AgentCard serverCard = client.getExtendedAgentCard();
```

## Push Notifications

```java
// Set a push notification configuration
TaskPushNotificationConfig taskConfig = TaskPushNotificationConfig.builder()
        .id("config-4567")
        .taskId("task-1234")
        .url("https://example.com/callback")
        .authentication(new AuthenticationInfo("bearer", "my-token"))
        .build();

TaskPushNotificationConfig created = client.createTaskPushNotificationConfiguration(taskConfig);

// Get a specific configuration
TaskPushNotificationConfig config = client.getTaskPushNotificationConfiguration(
        new GetTaskPushNotificationConfigParams("task-1234", "config-4567"));

// List configurations
ListTaskPushNotificationConfigsResult result =
        client.listTaskPushNotificationConfigurations(
                new ListTaskPushNotificationConfigsParams("task-1234"));

// Delete a configuration
client.deleteTaskPushNotificationConfigurations(
        new DeleteTaskPushNotificationConfigParams("task-1234", "config-4567"));
```

## Transport Configuration

### JSON-RPC with a Custom HTTP Client

```java
// Use a custom JDK HTTP client
HttpClient jdkHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

Client client = Client
        .builder(agentCard)
        .withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfig(new JdkA2AHttpClient(jdkHttpClient)))
        .build();
```

### Setting a Custom `Host` Header

The JDK-based `JdkA2AHttpClient` (the default) delegates to `java.net.http.HttpClient`,
which rejects an explicit `Host` header (`addHeader("Host", ...)`) with an
`IllegalArgumentException` unless the JVM is started with
`-Djdk.httpclient.allowRestrictedHeaders=host`. This is a JDK-wide restriction and
cannot be worked around per-client-instance.

If you need to send a custom `Host` header — for example, connecting directly to an
instance by IP while presenting the hostname a load balancer would normally supply —
either set that JVM property, or use the `VertxA2AHttpClient` from the
`a2a-java-sdk-http-client-vertx` extra, which does not build on `java.net.http.HttpClient`
and has no such restriction:

```java
// VertxA2AHttpClient is AutoCloseable: its no-args constructor starts and owns a Vert.x
// instance, and closing the Client does NOT close an injected A2AHttpClient — you own its
// lifecycle, so close it yourself when you are done with the Client.
VertxA2AHttpClient httpClient = new VertxA2AHttpClient();
try {
    Client client = Client
            .builder(agentCard)
            .withTransport(JSONRPCTransport.class,
                    new JSONRPCTransportConfig(httpClient))
            .build();
    // use client...
} finally {
    httpClient.close();
}
```

### REST with a Custom HTTP Client

```java
A2AHttpClient customHttpClient = ...

Client client = Client
        .builder(agentCard)
        .withTransport(RestTransport.class, new RestTransportConfig(customHttpClient))
        .build();
```

### gRPC

```java
Function<String, Channel> channelFactory = agentUrl ->
        ManagedChannelBuilder.forTarget(agentUrl).build();

Client client = Client
        .builder(agentCard)
        .withTransport(GrpcTransport.class, new GrpcTransportConfig(channelFactory))
        .build();
```

### Multiple Transports

```java
Client client = Client
        .builder(agentCard)
        .withTransport(GrpcTransport.class, new GrpcTransportConfig(channelFactory))
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .withTransport(RestTransport.class, new RestTransportConfig())
        .build();
```

### SSE Parser Configuration

The client uses a Server-Sent Events (SSE) parser for streaming responses. You can tune its limits via `SSEParserConfig` to handle agents that return large payloads (e.g., large artifacts or tool results):

| Parameter        | Description                                              | Default   |
|------------------|----------------------------------------------------------|-----------|
| `maxLineLength`  | Max bytes per raw SSE line (`0` = disabled)               | 1 MB      |
| `maxBufferLines` | Max `data:` lines per event block                        | 1 000     |
| `maxBufferChars` | Max total characters across all `data:` values per event | 1 MB      |

The defaults are suitable for most deployments. To override them, build a custom `SSEParserConfig` and pass it to the HTTP client.

> **Security note:** setting `maxLineLength` to `0` disables the per-line memory protection entirely. Without this limit, a malicious server sending a single unterminated line (no newline) can consume unbounded memory — `maxBufferChars` only limits accumulated `data:` field values _after_ lines have been decoded, not the raw line itself. Only disable the per-line check when you trust the remote agent or have other safeguards (e.g. a reverse proxy with its own line-length limit). Note that `maxLineLength` is enforced as a byte limit on raw UTF-8 data; for ASCII content (the common case) bytes and characters are equivalent.

```java
SSEParserConfig sseConfig = SSEParserConfig.builder()
        .maxLineLength(4 * 1024 * 1024)   // 4 MB per line
        .maxBufferChars(4 * 1024 * 1024)   // 4 MB per event
        .build();

// Pass to JdkA2AHttpClient, then to your transport config
JdkA2AHttpClient httpClient = JdkA2AHttpClient.withSseConfig(sseConfig);

Client client = Client
        .builder(agentCard)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(httpClient))
        .build();
```

## Observability (Optional)

Add distributed tracing and W3C Trace Context propagation to client calls with the [OpenTelemetry extras modules](extra/opentelemetry#client).

## Communicating with v0.3 Agents

See [Backward Compatibility](compatibility#client-communicating-with-v03-agents) for using `Client_v0_3` with older protocol agents.

## Examples

See [Examples](examples) for Hello World walkthroughs and sample applications.
