# Stream Lifecycle Hook Example

This example demonstrates `TaskStreamLifecycleHook`, a CDI-discoverable hook that lets you observe and control streaming connections for a task. The server closes all active streams when 3 subscribers connect to the same task.

For example, in order to save resources associated with streaming connections, you might want to close streams:

* If no events are received for a Task in a given timeframe. This can help if you have a lot of Tasks taking a very long time to complete
* Stopping rogue client opening too many subscriptions to the same Task

## Prerequisites

- Java 17 or higher
- Maven

## What It Does

**Server** — An agent sends 20 progress messages (one every 500ms). A `CloseStreamsHook` monitors subscriber count and calls `StreamCloseHandle.closeStreams()` when 3 subscribers are connected, gracefully closing all streams.

**Client** — Connects 3 subscribers sequentially:
1. Subscriber 1 sends a message (creates the task, starts streaming)
2. Subscriber 2 subscribes to the same task after 1.5 seconds
3. Both subscribers receive progress messages for 2 seconds
4. Subscriber 3 subscribes — the hook fires and closes all streams
5. All 3 subscribers see their streams end gracefully

## Run the Example

### 1. Build the SDK

From the repository root:

```bash
mvn clean install -DskipTests
```

### 2. Start the Server

```bash
cd examples/stream-lifecycle/server
mvn quarkus:dev
```

### 3. Run the Client

In a separate terminal:

```bash
cd examples/stream-lifecycle/client
mvn exec:java
```

### Expected Output (Client)

The exact event interleaving depends on timing, but you should see something like:

```
Resolved agent card: Stream Lifecycle Demo Agent
[Sub-1] Sending message to create task...
[Sub-1] StatusUpdate — TASK_STATE_WORKING

=== Task created: <task-id> ===

[Sub-1] ArtifactEvent — Progress update 1/20
[Sub-1] ArtifactEvent — Progress update 2/20
[Sub-1] ArtifactEvent — Progress update 3/20
[Sub-2] Subscribing to task <task-id>...

=== Subscribers 1 and 2 are active — receiving events... ===

[Sub-2] TaskEvent — state: TASK_STATE_WORKING, id: <task-id>
[Sub-1] ArtifactEvent — Progress update 4/20
[Sub-2] ArtifactEvent — Progress update 4/20
[Sub-1] ArtifactEvent — Progress update 5/20
[Sub-2] ArtifactEvent — Progress update 5/20
...
[Sub-3] Subscribing to task <task-id> (will trigger stream close)...
[Sub-1] Stream closed.
[Sub-2] Stream closed.
[Sub-3] Stream closed.

=== All streams closed. ===
```

### Expected Output (Server)

```
[HOOK] Subscriber added for task <id>. Active subscribers: 1
[AGENT] Starting execution for task <id>
[AGENT] Sending: Progress update 1/20
[HOOK] Event distributed for task <id>: Message (subscribers: 1)
...
[HOOK] Subscriber added for task <id>. Active subscribers: 2
...
[HOOK] Subscriber added for task <id>. Active subscribers: 3
[HOOK] Subscriber count reached 3 for task <id> — closing all streams
[HOOK] Subscriber removed for task <id>. Active subscribers: 2
[HOOK] Subscriber removed for task <id>. Active subscribers: 1
[HOOK] Subscriber removed for task <id>. Active subscribers: 0
```

## Transport Protocol Selection

Set `quarkus.agentcard.protocol` on both server and client (must match). Available values:

| Value | Transport |
|-------|-----------|
| `JSONRPC` | JSON-RPC 2.0 (default) |
| `GRPC` | gRPC |
| `HTTP+JSON` | HTTP+JSON/REST |

```bash
# Server — gRPC example
mvn quarkus:dev -Dquarkus.agentcard.protocol=GRPC

# Client — must use the same value
mvn exec:java -Dquarkus.agentcard.protocol=GRPC
```

## Key Files

| File | Description |
|------|-------------|
| `server/.../CloseStreamsHook.java` | `TaskStreamLifecycleHook` implementation — closes streams at 3 subscribers |
| `server/.../AgentExecutorProducer.java` | Agent that sends 20 progress messages over 10 seconds |
| `server/.../AgentCardProducer.java` | Agent card with streaming enabled |
| `client/.../StreamLifecycleClient.java` | Client that creates 3 subscribers and logs events |

## Integration Tests

The server module includes `@QuarkusTest` integration tests that verify the hook behavior across all three transports:

```bash
cd examples/stream-lifecycle/server
mvn test
```
