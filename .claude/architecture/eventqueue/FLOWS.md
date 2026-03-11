# Request Flows - EventQueue Processing

> Deep-dive on streaming vs non-streaming request handling

## Non-Streaming Flow (`onMessageSend()`)

**Location**: `DefaultRequestHandler.java`

```
1. initMessageSend()
   → Create TaskManager & RequestContext

2. queueManager.createOrTap(taskId)
   → Get/create EventQueue (MainQueue or ChildQueue)

3. registerAndExecuteAgentAsync()
   → Start AgentExecutor in background thread

4. resultAggregator.consumeAndBreakOnInterrupt(consumer)
   → Poll queue until terminal event or AUTH_REQUIRED
   → Blocking wait for events

5. cleanup(queue, task, async)
   → Close queue immediately OR in background

6. Return Task/Message to client
```

### Terminal Events

Events that cause polling loop exit:
- `TaskStatusUpdateEvent` with `isFinal() == true`
- `Message` (legacy)
- `Task` with state: COMPLETED, CANCELED, FAILED, REJECTED, UNKNOWN

### AUTH_REQUIRED Special Case

**Behavior**:
- Returns current task to client immediately
- Agent continues running in background
- Queue stays open, cleanup happens async
- Future events update TaskStore

**Why**: Allows client to handle authentication prompt while agent waits for credentials.

---

## Streaming Flow (`onMessageSendStream()`)

**Location**: `DefaultRequestHandler.java`

```
1. initMessageSend()
   → Same as non-streaming

2. queueManager.createOrTap(taskId)
   → Same

3. registerAndExecuteAgentAsync()
   → Same

4. resultAggregator.consumeAndEmit(consumer)
   → Returns Flow.Publisher<Event> immediately
   → Non-blocking

5. processor() wraps publisher:
   - Validates task ID
   - Adds task to QueueManager
   - Stores push notification config
   - Sends push notifications

6. cleanup(queue, task, true)
   → ALWAYS async for streaming

7. Return Flow.Publisher<StreamingEventKind>
```

### Key Difference

**Non-Streaming**: Blocks until terminal event, then returns Task/Message
**Streaming**: Returns Flow.Publisher immediately, client receives events as they arrive

**Cleanup**: Streaming ALWAYS uses async cleanup (background thread)

---

## EventConsumer Details

**Location**: `server-common/.../events/EventConsumer.java`

**Purpose**: Consumes events from EventQueue and exposes as reactive stream

**Key Methods**:
- `consume()` → Returns `Flow.Publisher<Event>`
- Polls queue with 500ms timeout
- Closes queue on final event
- Thread-safe concurrent consumption

**Usage**:
```java
EventConsumer consumer = new EventConsumer(eventQueue);
Flow.Publisher<Event> publisher = consumer.consume();
// Subscribe to receive events as they arrive
```

---

## ResultAggregator Modes

**Location**: `server-common/.../tasks/ResultAggregator.java`

Bridges EventConsumer and DefaultRequestHandler with three consumption modes:

### 1. consumeAndBreakOnInterrupt()

**Used by**: `onMessageSend()` (non-streaming)

**Behavior**:
- Polls queue until terminal event or AUTH_REQUIRED
- Returns `EventTypeAndInterrupt(event, interrupted)`
- Blocking operation
- Exits early on AUTH_REQUIRED (interrupted = true)

**Use Case**: Non-streaming requests that need single final response

### 2. consumeAndEmit()

**Used by**: `onMessageSendStream()` (streaming)

**Behavior**:
- Returns all events as `Flow.Publisher<Event>`
- Non-blocking, immediate return
- Client subscribes to stream
- Events delivered as they arrive

**Use Case**: Streaming requests where client wants all events in real-time

### 3. consumeAll()

**Used by**: `onCancelTask()`

**Behavior**:
- Consumes all events from queue
- Returns first `Message` or final `Task` found
- Simple consumption without streaming
- Blocks until queue exhausted

**Use Case**: Task cancellation where final state matters

---

## Flow Comparison Table

| Aspect | Non-Streaming | Streaming |
|--------|---------------|-----------|
| **ResultAggregator Mode** | consumeAndBreakOnInterrupt | consumeAndEmit |
| **Return Type** | Task/Message | Flow.Publisher |
| **Blocking** | Yes (until terminal event) | No (immediate return) |
| **Cleanup** | Immediate or async | Always async |
| **AUTH_REQUIRED** | Early exit, return task | Continue streaming |
| **Use Case** | Simple request/response | Real-time event updates |

---

## Cleanup Integration

### Non-Streaming Cleanup Decision

```java
if (event instanceof Message || isFinalEvent(event)) {
    if (!interrupted) {
        cleanup(queue, task, false);  // Immediate: wait for agent, close queue
    } else {
        cleanup(queue, task, true);   // Async: close in background (AUTH_REQUIRED case)
    }
}
```

**Logic**:
- Terminal event + not interrupted → Immediate cleanup (wait for agent, close queue)
- Terminal event + interrupted (AUTH_REQUIRED) → Async cleanup (agent still running)

### Streaming Cleanup

```java
cleanup(queue, task, true);  // ALWAYS async for streaming
```

**Logic**: Streaming always uses async cleanup because:
- Publisher already returned to client
- Events may still be processing
- Queue cleanup happens in background

---

## Thread Model

### Agent Execution Thread
- `CompletableFuture.runAsync(agentExecutor::execute, executor)`
- Agent runs in background thread pool
- Enqueues events to MainQueue

### MainEventBusProcessor Thread
- Single background thread: "MainEventBusProcessor"
- Processes events from MainEventBus
- Persists to TaskStore, distributes to ChildQueues

### Consumer Thread
- Non-streaming: Request handler thread (blocking)
- Streaming: Subscriber thread (reactive)
- Polls ChildQueue for events

### Cleanup Thread
- Async cleanup: Background thread pool
- Immediate cleanup: Request handler thread

---

## Related Documentation

- **[Main Overview](../EVENTQUEUE.md)** - Architecture and components
- **[Lifecycle](LIFECYCLE.md)** - Queue lifecycle and cleanup
- **[Scenarios](SCENARIOS.md)** - Real-world usage patterns
