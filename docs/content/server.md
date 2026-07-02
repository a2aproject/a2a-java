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

The A2A Java SDK uses a flexible configuration system that works across different frameworks.

**Default behavior:** Configuration values come from `META-INF/a2a-defaults.properties` files on the classpath (provided by core modules and extras). These defaults work out of the box without any additional setup.

**Customizing configuration:**
- **Quarkus/MicroProfile Config users**: Add the [`microprofile-config`](https://github.com/a2aproject/a2a-java/blob/main/integrations/microprofile-config/README.md) integration to override defaults via `application.properties`, environment variables, or system properties
- **Spring/other frameworks**: See the [integration module README](https://github.com/a2aproject/a2a-java/blob/main/integrations/microprofile-config/README.md#custom-config-providers) for how to implement a custom `A2AConfigProvider`
- **Reference implementations**: Already include the MicroProfile Config integration

### Configuration Properties

**Executor Settings** (Optional)

The SDK uses a dedicated executor for async operations like streaming. Default: 5 core threads, 50 max threads.

```properties
# Core thread pool size for the @Internal executor (default: 5)
a2a.executor.core-pool-size=5

# Maximum thread pool size (default: 50)
a2a.executor.max-pool-size=50

# Thread keep-alive time in seconds (default: 60)
a2a.executor.keep-alive-seconds=60
```

**Blocking Call Timeouts** (Optional)

```properties
# Timeout for agent execution in blocking calls (default: 30 seconds)
a2a.blocking.agent.timeout.seconds=30

# Timeout for event consumption in blocking calls (default: 5 seconds)
a2a.blocking.consumption.timeout.seconds=5
```

**Why this matters:**
- **Streaming Performance**: The executor handles streaming subscriptions. Too few threads can cause timeouts under concurrent load.
- **Resource Management**: The dedicated executor prevents streaming operations from competing with the ForkJoinPool.
- **Concurrency**: In production with high concurrent streaming, increase pool sizes accordingly.
- **Agent Timeouts**: LLM-based agents may need longer timeouts (60–120s) compared to simple agents.

**Note:** The reference server implementations (Quarkus-based) automatically include the MicroProfile Config integration, so properties work out of the box in `application.properties`.

## 5. Task Authorization (Optional)

> **⚠ Security note:** For multi-user deployments, a `TaskAuthorizationProvider` **must** be configured. Without one, all operations are permitted regardless of authentication — any authenticated user can read, modify, or cancel any task. Production deployments should use a fail-closed ownership policy (deny access when ownership is unknown).

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

> **Note:** When task authorization is required, always obtain `RequestHandler` through CDI injection. Manual instantiation via `DefaultRequestHandler.create()` bypasses the `AuthorizationRequestHandlerDecorator` and all authorization checks.

### User Identity in ServerCallContext

Authorization decisions rely on `context.getUser()` returning the authenticated user. How the user is populated depends on the transport:

- **JSON-RPC and REST**: The Quarkus route handler extracts the user from the Vert.x routing context (`rc.userContext()`) and sets it on `ServerCallContext` directly.
- **gRPC**: The reference server includes a `QuarkusCallContextFactory` CDI bean that injects the Quarkus `SecurityIdentity` and maps it to the `ServerCallContext` `User`. This happens automatically when using the reference gRPC module. If you provide your own `CallContextFactory`, you are responsible for populating the user.

### Authorization Checks

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
<!-- v0.3 JSON-RPC support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- v0.3 REST support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- v0.3 gRPC support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-grpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

### How Version Routing Works

- **JSON-RPC and REST**: When serving multiple protocol versions, version routing inspects the `A2A-Version` HTTP header on each request. If the header is `"1.0"`, the request is routed to the v1.0 handler. If it is `"0.3"` or absent, the request is routed to the v0.3 handler.
- **gRPC**: Version dispatch is implicit — v0.3 clients use the `a2a.v1` protobuf package and v1.0 clients use `lf.a2a.v1`, so requests are routed to the correct service automatically.
- **Agent card**: When both v1.0 and v0.3 are enabled, the v1.0 `AgentCard` takes precedence and is served at `/.well-known/agent-card.json`. The v0.3 `AgentCard_v0_3` is ignored. If only v0.3 is enabled, the v0.3 agent card is used. If only v1.0 is enabled, the v1.0 agent card is used as-is.

### Making the v1.0 Agent Card Compatible with v0.3 Clients

When serving both protocol versions, you need to ensure the v1.0 agent card contains fields that v0.3 clients expect. Existing v0.3 client implementations (in any language) look for `url`, `preferredTransport`, and `additionalInterfaces` with `transport`/`url` entries — fields that don't exist in the v1.0 format by default.

To make your v1.0 `AgentCard` parsable by v0.3 clients, set these fields on the builder:

```java
AgentCard card = AgentCard.builder()
        .name("My Agent")
        // ... other v1.0 fields ...
        .supportedInterfaces(List.of(
                new AgentInterface("jsonrpc", "http://localhost:9999")))
        // v0.3 backward-compatibility fields:
        .url("http://localhost:9999")
        .preferredTransport("jsonrpc")
        .additionalInterfaces(List.of(
                new Legacy_0_3_AgentInterface("jsonrpc", "http://localhost:9999")))
        .build();
```

The two interface lists serve different clients:

- `supportedInterfaces` — used by **v1.0 clients** to discover endpoints (uses `AgentInterface` with `protocolBinding`/`url`/`tenant` fields)
- `additionalInterfaces` — used by **v0.3 clients** to discover endpoints (uses `Legacy_0_3_AgentInterface` with v0.3 field names: `transport`/`url`)
- `url` and `preferredTransport` — top-level fields that v0.3 clients use to discover the primary endpoint

### Push Notification Behavior

Push notification payloads are automatically formatted to match the protocol version used when the push notification configuration was registered. When a v0.3 client registers a push notification configuration (via any transport), the server records the protocol version alongside the configuration. When a notification is later sent to that webhook, the payload is formatted as a v0.3 Task object. Configurations registered by v1.0 clients receive v1.0 `StreamResponse` payloads as usual. This happens transparently — no additional configuration is needed beyond adding the compat reference module.

## Server Integrations

- **Quarkus** — Reference implementations are Quarkus-based (JSON-RPC, gRPC, REST)
- **Jakarta EE** — [a2a-jakarta](https://github.com/wildfly-extras/a2a-jakarta) works with any Jakarta EE Web Profile runtime

See [CONTRIBUTING_INTEGRATIONS.md](https://github.com/a2aproject/a2a-java/blob/main/CONTRIBUTING_INTEGRATIONS.md) to submit your own integration.
