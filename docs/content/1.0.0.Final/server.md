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
            if (task.status().state() == TaskState.CANCELED ||
                task.status().state() == TaskState.COMPLETED) {
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

See [Configuration](configuration) for all config properties and tuning.

## Backward Compatibility with v0.3

See [Backward Compatibility](compatibility) for multi-version modules, version routing, and v0.3 client support.

## Server Integrations

- **Quarkus** — Reference implementations are Quarkus-based (JSON-RPC, gRPC, REST)
- **Jakarta EE** — [a2a-jakarta](https://github.com/wildfly-extras/a2a-jakarta) works with any Jakarta EE Web Profile runtime

See [CONTRIBUTING_INTEGRATIONS.md](https://github.com/a2aproject/a2a-java/blob/main/CONTRIBUTING_INTEGRATIONS.md) to submit your own integration.
