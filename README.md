# A2A Java SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

<!-- markdownlint-disable no-inline-html -->

<html>
   <h2 align="center">
   <img src="https://raw.githubusercontent.com/google-a2a/A2A/refs/heads/main/docs/assets/a2a-logo-black.svg" width="256" alt="A2A Logo"/>
   </h2>
   <h3 align="center">A Java library that helps run agentic applications as A2AServers following the <a href="https://a2a-protocol.org/">Agent2Agent (A2A) Protocol</a>.</h3>
</html>

The A2A Java SDK is a comprehensive library designed to help developers build, deploy, and interact with agentic applications. It provides a full implementation of the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/), supporting multiple transports and offering a modular architecture for easy integration.

## Table of Contents

- [Installation](#installation)
- [Core Architectural Components](#core-architectural-components)
- [A2A Server](#a2a-server)
  - [1. Add Dependencies](#1-add-an-a2a-java-sdk-server-maven-dependency-to-your-project)
  - [2. Create an Agent Card](#2-add-a-class-that-creates-an-a2a-agent-card)
  - [3. Create an Agent Executor](#3-add-a-class-that-creates-an-a2a-agent-executor)
  - [4. Configuration System](#4-configuration-system)
- [A2A Client](#a2a-client)
- [Discovery & Metadata](#discovery--metadata)
- [Recent Parity Updates](#recent-parity-updates)
- [Examples](#examples)

---

## Installation

You can build the A2A Java SDK using `mvn`:

```bash
mvn clean install
```

### Regeneration of gRPC files
We copy https://github.com/a2aproject/A2A/blob/main/specification/grpc/a2a.proto to the [`spec-grpc/`](./spec-grpc) project, and adjust the `java_package` option to be as follows:
```
option java_package = "io.a2a.grpc";
```
Then build the `spec-grpc` module with `mvn clean install  -Dskip.protobuf.generate=false` to regenerate the gRPC classes in the `io.a2a.grpc` package.

---

## Core Architectural Components

The A2A Java SDK is built on a modular architecture that separates the protocol concerns from the agent execution logic.

### 1. Agent Executor (`AgentExecutor`)
The `AgentExecutor` is the primary interface you implement to define your agent's behavior. It receives a `RequestContext` and an `EventQueue` to process messages and stream results back to the client.

### 2. Task Management
Tasks represent units of work assigned to an agent. The SDK provides robust task management:
- **`TaskManager`**: The central service for creating, retrieving, and updating tasks.
- **`TaskStore`**: SPI for task persistence. The default is `InMemoryTaskStore`.
- **`TaskUpdater`**: A high-level utility that simplifies common task operations like starting work, adding artifacts, and completing or failing a task.

### 3. Event System
Asynchronous communication is handled via an event-driven model:
- **`EventQueue`**: A per-task or per-session queue where events (messages, status updates, artifacts) are published.
- **`QueueManager`**: Manages the lifecycle and distribution of event queues.

### 4. Runner Management
The `RunnerManager` (found in integrations and reference implementations) handles the lifecycle of agents, ensuring they are initialized with the correct session state and tools. It bridges the gap between the A2A server and the underlying agent implementation.

---

## A2A Server

The A2A Java SDK provides a Java server implementation of the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/). To run your agentic Java application as an A2A server, simply follow the steps below.

### 1. Add an A2A Java SDK Server Maven dependency to your project

Adding a dependency on an A2A Java SDK Server will provide access to the core classes that make up the A2A specification.

#### Server Transports 
The A2A Java SDK Reference Server implementations support the following transports:

* **JSON-RPC 2.0**: Standardized JSON-based RPC, typically over HTTP.
* **gRPC**: Optimized for high-performance, bidirectional streaming.
* **HTTP+JSON/REST**: Traditional RESTful endpoints for maximum compatibility.

To use the reference implementation with the JSON-RPC protocol, add the following dependency to your project:

> *⚠️ The `io.github.a2asdk` `groupId` below is temporary and will likely change for future releases.*

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

### 2. Add a class that creates an A2A Agent Card

```java
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
...

@ApplicationScoped
public class WeatherAgentCardProducer {
    
    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Weather Agent")
                .description("Helps with weather")
                .url("http://localhost:10001")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
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
                .protocolVersion(io.a2a.spec.AgentCard.CURRENT_PROTOCOL_VERSION)
                .build();
    }
}
```

### 3. Add a class that creates an A2A Agent Executor

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
...

@ApplicationScoped
public class WeatherAgentExecutorProducer {

    @Inject
    WeatherAgent weatherAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new WeatherAgentExecutor(weatherAgent);
    }

    private static class WeatherAgentExecutor implements AgentExecutor {
        // ... implementation details ...
        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();
            // ... agent logic ...
            updater.complete();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            // ... cancel logic ...
            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
        }
    }
}
```

### 4. Configuration System

The A2A Java SDK uses a flexible, framework-agnostic configuration system (`A2AConfigProvider`).

- **Default behavior:** Configuration values come from `META-INF/a2a-defaults.properties` files on the classpath.
- **Customizing configuration:** Overrides via environment variables, system properties, or framework-specific config (e.g., Quarkus `application.properties` via `microprofile-config`).

#### Key Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `a2a.executor.core-pool-size` | 5 | Core thread pool size for async operations. |
| `a2a.executor.max-pool-size` | 50 | Maximum thread pool size. |
| `a2a.blocking.agent.timeout.seconds` | 30 | Timeout for agent execution in blocking calls. |

---

## A2A Client

The A2A Java SDK provides a Java client implementation of the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/), allowing communication with A2A servers.

### Sample Usage

```java
// Create the client using the builder
Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .addConsumers(consumers)
        .streamingErrorHandler(errorHandler)
        .build();

// Send a message
client.sendMessage(A2A.toUserMessage("tell me a joke"));

// Get the current state of a task
Task task = client.getTask(new TaskQueryParams("task-1234"));
```

---

## Discovery & Metadata

### 1. Agent Card (`AgentCard`)
A metadata structure describing an agent's name, description, capabilities, and skills.

### 2. Discovery Service (`AgentDiscoveryService`)
Responsible for advertising and discovering available agents and their cards.

---

## Recent Parity Updates

These features ensure parity with the A2A protocol specification:

### 1. ID Generation SPI
The SDK uses an SPI for generating unique identifiers for tasks, messages, and events.
- **Interface**: `io.a2a.server.util.IdGenerator`
- **Default Implementation**: `io.a2a.server.util.UUIDIdGenerator`.

### 2. AgentCard Signing & Security
Supports signing cards using JSON Web Signatures (JWS) to ensure authenticity.
- **`SigningService`**: Uses `nimbus-jose-jwt` for cryptographic operations.

### 3. Telemetry & Observability
Built-in support for distributed tracing via OpenTelemetry is integrated into the core server components via `A2ATelemetry`.

---

## Examples

You can find examples of how to use the A2A Java SDK in the [a2a-samples repository](https://github.com/a2aproject/a2a-samples/tree/main/samples/java/agents).
