---
title: A2A Server Guide
description: Run your agentic Java application as an A2A server following the Agent2Agent Protocol.
layout: page
---

# A2A Server

The A2A Java SDK provides a Java server implementation of the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/). To run your agentic Java application as an A2A server, follow the steps below.

## Supported Transports

- JSON-RPC 2.0
- gRPC
- HTTP+JSON/REST

## 1. Add a Server Dependency

### JSON-RPC

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    <!-- Use a released version from https://github.com/a2aproject/a2a-java/releases -->
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

### gRPC

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-grpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

### HTTP+JSON/REST

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

You can add more than one transport dependency to support multiple protocols simultaneously.

## 2. Define an Agent Card

```java
@ApplicationScoped
public class WeatherAgentCardProducer {

    private static final String AGENT_URL = "http://localhost:10001";

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Weather Agent")
                .description("Helps with weather")
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL)))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                        .id("weather_search")
                        .name("Search weather")
                        .description("Helps with weather in cities or states")
                        .tags(Collections.singletonList("weather"))
                        .examples(List.of("weather in LA, CA"))
                        .build()))
                .build();
    }
}
```

## 3. Implement an Agent Executor

```java
@ApplicationScoped
public class WeatherAgentExecutorProducer {

    @Inject
    WeatherAgent weatherAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new WeatherAgentExecutor(weatherAgent);
    }

    private static class WeatherAgentExecutor implements AgentExecutor {

        private final WeatherAgent weatherAgent;

        public WeatherAgentExecutor(WeatherAgent weatherAgent) {
            this.weatherAgent = weatherAgent;
        }

        @Override
        public void execute(RequestContext context, AgentEmitter agentEmitter) throws JSONRPCError {
            if (context.getTask() == null) {
                agentEmitter.submit();
            }
            agentEmitter.startWork();

            String userMessage = extractTextFromMessage(context.getMessage());
            String response = weatherAgent.chat(userMessage);

            agentEmitter.addArtifact(List.of(new TextPart(response)));
            agentEmitter.complete();
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter agentEmitter) throws JSONRPCError {
            Task task = context.getTask();
            if (task == null) {
                agentEmitter.cancel();
                return;
            }
            if (task.getStatus().state() == TaskState.CANCELED ||
                task.getStatus().state() == TaskState.COMPLETED) {
                throw new TaskNotCancelableError();
            }
            agentEmitter.cancel();
        }

        private String extractTextFromMessage(Message message) {
            if (message == null) {
                return "";
            }
            StringBuilder textBuilder = new StringBuilder();
            for (Part<?> part : message.parts()) {
                if (part instanceof TextPart textPart) {
                    textBuilder.append(textPart.text());
                }
            }
            return textBuilder.toString();
        }
    }
}
```

## 4. Configuration

The SDK uses `META-INF/a2a-defaults.properties` for defaults. Override via `application.properties` when using Quarkus/MicroProfile Config:

```properties
# Thread pool for async/streaming operations
a2a.executor.core-pool-size=5
a2a.executor.max-pool-size=50
a2a.executor.keep-alive-seconds=60

# Timeouts for blocking calls
a2a.blocking.agent.timeout.seconds=30
a2a.blocking.consumption.timeout.seconds=5
```

For LLM-based agents, increase `a2a.blocking.agent.timeout.seconds` to 60–120 seconds.

## 5. Task Authorization (Optional)

Implement `TaskAuthorizationProvider` to control per-user access:

```java
@ApplicationScoped
public class MyTaskAuthorizationProvider implements TaskAuthorizationProvider {

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context.getUser(), taskId);
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context.getUser(), taskId);
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation op) {
        return context.getUser().isAuthenticated();
    }

    @Override
    public boolean isTaskRecorded(String taskId) {
        return ownershipStore.contains(taskId);
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation op) {
        ownershipStore.put(taskId, context.getUser().getUsername());
    }
}
```

The SDK discovers the bean via CDI automatically — no additional wiring needed.

| Operation | Authorization check |
|-----------|---------------------|
| `getTask`, `subscribeToTask`, `getTaskPushNotificationConfig`, `listTaskPushNotificationConfigs` | `checkRead` |
| `cancelTask`, `createTaskPushNotificationConfig`, `deleteTaskPushNotificationConfig` | `checkWrite` |
| `messageSend` / `messageSendStream` (existing task) | `checkWrite` |
| `messageSend` / `messageSendStream` (new task) | `checkCreate`, then `recordOwnership` |
| `listTasks` | `checkRead` per task |

## Backward Compatibility with v0.3

Add compat modules alongside v1.0 modules to serve both protocol versions simultaneously. No changes to your `AgentExecutor` are needed.

### Multi-Version Module (recommended)

```xml
<!-- JSON-RPC with automatic v1.0 + v0.3 routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- REST with automatic v1.0 + v0.3 routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

### Individual Compat Modules

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

Version routing uses the `A2A-Version` HTTP header for JSON-RPC and REST; for gRPC it is implicit via protobuf package name.

## Server Integrations

- **Quarkus** — Reference implementations are Quarkus-based (JSON-RPC, gRPC, REST)
- **Jakarta EE** — [a2a-jakarta](https://github.com/wildfly-extras/a2a-jakarta) works with any Jakarta EE Web Profile runtime

See [CONTRIBUTING_INTEGRATIONS.md](https://github.com/a2aproject/a2a-java/blob/main/CONTRIBUTING_INTEGRATIONS.md) to submit your own integration.
