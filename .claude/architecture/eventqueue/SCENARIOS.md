# Usage Scenarios & Common Pitfalls

> Real-world patterns and mistakes to avoid

## Scenario 1: Fire-and-Forget Pattern (TCK)

**Pattern**: Agent emits WORKING status but never completes

```java
// Agent execution
agentExecutor.execute(context, queue) {
    Task workingTask = new Task.Builder()
        .id(taskId)
        .status(new TaskStatus(TaskState.WORKING))  // Non-final!
        .build();
    queue.enqueueEvent(workingTask);
    // Agent finishes WITHOUT emitting COMPLETED/FAILED
}

// What happens:
// 1. ChildQueue closes (client got WORKING event)
// 2. MainQueue.childClosing() checks: isTaskFinalized(taskId) → false
// 3. MainQueue stays OPEN in QueueManager map
// 4. Late resubscription works: queueManager.tap(taskId) → success!
```

**Note**: Queue numbers grow during TCK run - this is EXPECTED and intentional for resubscription support.

**Why This Works**:
- Level 2 protection prevents auto-close when task is non-final
- MainQueue stays in map even with no children
- Later reconnections can tap into same MainQueue

---

## Scenario 2: Late Resubscription

**Pattern**: Client disconnects then reconnects to ongoing task

```
Time 0: Client sends message, gets ChildQueue
Time 1: Agent emits WORKING event
Time 2: Client disconnects, ChildQueue closes
Time 3: Agent still processing (non-final state)
Time 4: All ChildQueues closed, MainQueue.childClosing() fires
        → Checks isTaskFinalized() → false
        → MainQueue stays open
Time 5: Client reconnects: queueManager.tap(taskId)
        → MainQueue still in map!
        → New ChildQueue created
        → Success!
```

**Key Insight**: The gap between Time 4 and Time 5 can be seconds, minutes, or hours. As long as task is non-final, MainQueue remains available.

**Use Cases**:
- Mobile app loses network connection
- Browser tab closed and reopened
- Load balancer routes to different instance
- Debugging: stop client, fix bug, restart

---

## Scenario 3: Normal Completion

**Pattern**: Task completes successfully

```java
// Agent completes
Task completed = new Task.Builder()
    .id(taskId)
    .status(new TaskStatus(TaskState.COMPLETED))  // Final state!
    .build();
queue.enqueueEvent(completed);

// Lifecycle:
// 1. TaskStore persists COMPLETED task
// 2. ChildQueue closes after consuming final event
// 3. Level 2: MainQueue.childClosing()
//    → isTaskFinalized(taskId) = true
//    → mainQueue.doClose()
// 4. Level 1: Cleanup callback fires
//    → isTaskFinalized(taskId) = true
//    → queues.remove(taskId)
```

**Timeline**:
- Event enqueued → MainEventBus → persisted → distributed
- ChildQueue receives COMPLETED event → closes
- MainQueue detects no children + finalized task → closes
- Cleanup callback removes from QueueManager map

**Result**: Prompt cleanup when task actually finishes

---

## Scenario 4: Tapping (Multiple Consumers)

**Pattern**: Multiple clients consuming same task events

```java
// Initial request creates MainQueue
EventQueue mainQueue = queueManager.createOrTap(taskId);

// Second client taps into existing MainQueue
EventQueue childQueue = queueManager.tap(taskId);

// Event distribution (ASYNCHRONOUS via MainEventBus)
// NOTE: Distribution is NOT immediate!
public void enqueueEvent(Event event) {
    // Step 1: Submit to MainEventBus (async processing)
    mainEventBus.submit(event);

    // Step 2: MainEventBusProcessor thread (separate background thread):
    //   - Persists event to TaskStore
    //   - Distributes to all ChildQueues via child.internalEnqueueItem(item)
    //   - Invokes replication hook if configured

    // Key Point: Events are NOT immediately in ChildQueues!
    // There's a delay while MainEventBusProcessor persists and distributes.
}
```

**Use Cases**:
- **Resubscribing to ongoing tasks**: Late reconnection scenario
- **Canceling tasks while receiving events**: Client sends cancel, still receives updates
- **Multiple concurrent consumers**: Admin dashboard + user client both watching same task
- **Testing/debugging**: Monitor task execution while client operates normally

**Key Points**:
- All ChildQueues receive ALL events
- Each ChildQueue has independent consumption
- MainQueue doesn't close until ALL children close AND task finalizes

---

## Common Pitfalls

### 1. Closing EventQueue Before AgentExecutor Finishes

**Problem**:
```java
// WRONG
agentExecutor.execute(context, queue);
queue.close();  // Too early! Agent may still be enqueueing events
```

**Solution**:
```java
// RIGHT - in DefaultRequestHandler.cleanup()
Runnable cleanupTask = () -> {
    agentFuture.join();  // Wait for agent to finish
    queue.close();       // Then close queue
};
```

**Why**: Agent runs asynchronously. Closing queue before agent finishes loses events.

---

### 2. Not Accounting for Async Cleanup in Streaming

**Problem**:
```java
// WRONG assumption
onMessageSendStream() returns → queue is closed
```

**Reality**:
```java
// RIGHT understanding
onMessageSendStream() returns → queue still open
cleanup() happens in background → queue closes later
```

**Why**: Streaming returns publisher immediately. Queue cleanup happens asynchronously after streaming completes.

**Impact**: Tests may see queues still open after streaming response sent. This is expected.

---

### 3. Assuming MainQueue Has Local Queue

**Problem**:
```java
// WRONG
Event event = mainQueue.dequeueEventItem();  // Throws UnsupportedOperationException!
```

**Reality**:
- MainQueue has NO local queue
- Events submit directly to MainEventBus
- Only ChildQueues have local queues

**Why**: Design choice to centralize persistence through MainEventBus.

**Correct Usage**:
```java
// Enqueue to MainQueue (goes to MainEventBus)
mainQueue.enqueueEvent(event);

// Dequeue from ChildQueue only
Event event = childQueue.dequeueEventItem();
```

---

### 4. Not Handling AUTH_REQUIRED Special Case

**Problem**:
```java
// WRONG assumption
AUTH_REQUIRED received → agent stopped → cleanup can be immediate
```

**Reality**:
```java
// RIGHT understanding
AUTH_REQUIRED received → agent STILL RUNNING → cleanup must be async
```

**Why**: Agent waits for authentication credentials. It hasn't finished executing.

**Impact**:
- Non-streaming: Returns task to client immediately, cleanup happens async
- Agent continues running in background
- Future events (COMPLETED, FAILED) update TaskStore

---

### 5. Expecting Immediate Queue Cleanup

**Problem**:
```java
// WRONG expectation
task emits WORKING (non-final) → queue should be cleaned up
```

**Reality**:
```java
// RIGHT understanding
task emits WORKING (non-final) → queue intentionally KEPT OPEN
task emits COMPLETED (final) → queue cleaned up
```

**Why**: Two-level protection checks task finality before cleanup.

**Impact**:
- Non-final tasks: Queues retained intentionally (fire-and-forget support)
- Finalized tasks: Queues cleaned up promptly
- This is NOT a leak, it's intentional design

**When to Worry**: If finalized tasks don't clean up queues (check TaskStateProvider implementation)

---

## Scenario Comparison Table

| Scenario | Task State | Queue Behavior | Use Case |
|----------|-----------|----------------|----------|
| **Fire-and-Forget** | Non-final (WORKING) | Stays open indefinitely | TCK compliance, async agents |
| **Late Resubscription** | Non-final | Stays open for reconnection | Network issues, debugging |
| **Normal Completion** | Final (COMPLETED) | Closes promptly | Standard request/response |
| **Tapping** | Any | Multiple ChildQueues share MainQueue | Monitoring, multi-client |

---

## Debugging Tips

### Check Queue State
```java
// Is MainQueue in map?
boolean exists = queueManager.tap(taskId) != null;

// What's the queue size?
int size = eventQueue.size();  // MainQueue = MainEventBus size, ChildQueue = local size
```

### Check Task State
```java
// Is task finalized?
boolean finalized = taskStateProvider.isTaskFinalized(taskId);

// Is task active?
boolean active = taskStateProvider.isTaskActive(taskId);
```

### Trace Event Flow
```java
// Add EventEnqueueHook for logging
EventQueue.builder()
    .hook(event -> LOGGER.info("Event enqueued: {}", event))
    .build();
```

### Monitor MainEventBus
```java
// Check queue depth
int depth = mainEventBus.size();  // High depth = processing backlog
```

---

## Related Documentation

- **[Main Overview](../EVENTQUEUE.md)** - Architecture and components
- **[Lifecycle](LIFECYCLE.md)** - Queue lifecycle and two-level protection
- **[Flows](FLOWS.md)** - Request handling patterns
