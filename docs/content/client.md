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
Task cancelled = client.cancelTask(new TaskIdParams("task-1234"));

// Subscribe to an ongoing task
client.subscribeToTask(new TaskIdParams("task-1234"));
client.subscribeToTask(taskIdParams, customConsumers, customErrorHandler);

// Retrieve the server agent card
AgentCard serverCard = client.getAgentCard();
```

## Push Notifications

```java
// Set a push notification configuration
PushNotificationConfig pushConfig = PushNotificationConfig.builder()
        .url("https://example.com/callback")
        .authenticationInfo(new AuthenticationInfo(List.of("jwt"), null))
        .build();

TaskPushNotificationConfig taskConfig = TaskPushNotificationConfig.builder()
        .taskId("task-1234")
        .pushNotificationConfig(pushConfig)
        .build();

client.createTaskPushNotificationConfiguration(taskConfig);

// List configurations
List<TaskPushNotificationConfig> configs =
        client.listTaskPushNotificationConfigurations(
                new ListTaskPushNotificationConfigParams("task-1234"));

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

## Communicating with v0.3 Agents

Use `Client_v0_3` to communicate with agents that only support protocol v0.3:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-transport-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

```java
AgentCard card = A2ACardResolver.builder().baseUrl("http://localhost:1234")
        .build().getAgentCard();

AgentInterface v03Interface = card.supportedInterfaces().stream()
        .filter(i -> A2AProtocol_v0_3.PROTOCOL_VERSION.equals(i.protocolVersion()))
        .findFirst().orElseThrow();

Client_v0_3 client = ClientBuilder_v0_3.forUrl(v03Interface.url())
        .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3())
        .build();
```

## Examples

- [Hello World Client](https://github.com/a2aproject/a2a-java/blob/main/examples/helloworld/client/README.md) — Java client talking to a Python A2A server
- [Hello World Server](https://github.com/a2aproject/a2a-java/blob/main/examples/helloworld/server/README.md) — Python client talking to a Java A2A server
- [a2a-samples repository](https://github.com/a2aproject/a2a-samples/tree/main/samples/java/agents) — More agent examples
