# Queue Lifecycle - THE BIG IDEA

> Deep-dive on task state-driven queue lifecycle management

## Problem Solved

- **Fire-and-forget tasks**: Agent finishes without emitting final state
- **Client reconnections**: Late reconnections after disconnect
- **Replicated events**: Late-arriving events for ongoing tasks
- **Queue leaks**: Proper cleanup when tasks finalize

## Solution: Two-Level Protection

**Core Principle**: MainQueues stay open in QueueManager map as long as Task is in non-final state, enabling fire-and-forget and late resubscriptions.

---

## Level 1: Cleanup Callback

**When**: MainQueue closes
**Location**: `InMemoryQueueManager.getCleanupCallback()`

```java
Runnable cleanupCallback = () -> {
    if (taskStateProvider != null && !taskStateProvider.isUnsatisfied()) {
        boolean isFinalized = taskStateProvider.isTaskFinalized(taskId);
        if (!isFinalized) {
            LOGGER.info("Task {} is not finalized, keeping queue in map", taskId);
            return;  // Don't remove from map - task still active
        }
    }
    queues.remove(taskId);  // Only remove if finalized
};
```

**Purpose**: Prevents removal from QueueManager map for non-final tasks (enables resubscription).

---

## Level 2: Auto-Close Prevention

**When**: Last ChildQueue closes
**Location**: `MainQueue.childClosing()`

```java
void childClosing(ChildQueue child, boolean immediate) {
    children.remove(child);

    if (!children.isEmpty()) {
        return;  // Other children still active
    }

    // No children left - check if task finalized before auto-closing
    if (taskStateProvider != null && taskId != null) {
        boolean isFinalized = taskStateProvider.isTaskFinalized(taskId);
        if (!isFinalized) {
            LOGGER.info("MainQueue for task {} has no children, but task is not finalized - keeping queue open", taskId);
            return;  // Keep MainQueue OPEN for resubscriptions!
        }
    }

    this.doClose(immediate);  // Close only if task finalized
}
```

**Purpose**: Prevents auto-close when all children disconnect (keeps queue alive for late arrivals).

---

## TaskStateProvider Interface

**Location**: `server-common/.../tasks/TaskStateProvider.java`

```java
public interface TaskStateProvider {
    boolean isTaskActive(String taskId);      // Is task still being worked on?
    boolean isTaskFinalized(String taskId);   // Is task in final state?
}
```

### Implementations
- `InMemoryTaskStore` implements TaskStateProvider
- `JpaDatabaseTaskStore` implements TaskStateProvider
- Injected via CDI: `Instance<TaskStateProvider>`

### State Checks
- `isTaskActive()`: Used by ReplicatedQueueManager to skip events for inactive tasks
- `isTaskFinalized()`: Used by both protection levels to determine cleanup eligibility

---

## Queue Close Modes

### Graceful Close (`queue.close()`)

- Drains remaining events before closing
- Used by normal termination
- ChildQueues close individually

### Immediate Close (`queue.close(true)`)

- Clears all pending events immediately
- Used by error conditions
- Forces all children to close

---

## Background Cleanup

**Location**: `DefaultRequestHandler.cleanup()`

### Non-Streaming Cleanup

```java
if (event instanceof Message || isFinalEvent(event)) {
    if (!interrupted) {
        cleanup(queue, task, false);  // Immediate: wait for agent, close queue
    } else {
        cleanup(queue, task, true);   // Async: close in background
    }
}
```

### Streaming Cleanup (always async)

```java
cleanup(queue, task, true);  // Background cleanup after streaming completes
```

### Cleanup Implementation

```java
private void cleanup(EventQueue queue, Task task, boolean async) {
    Runnable cleanupTask = () -> {
        agentFuture.join();  // Wait for agent to finish
        queue.close();       // Close ChildQueue (triggers Level 2 check)
    };

    if (async) {
        CompletableFuture.runAsync(cleanupTask, executor);
    } else {
        cleanupTask.run();
    }
}
```

---

## Memory Management

### Non-Final Tasks
- Queues retained in QueueManager map
- Small memory footprint (queue object + taskId)
- Enables fire-and-forget and resubscription patterns

### Finalized Tasks
- Queues cleaned up immediately
- Removed from QueueManager map
- Grace period in JpaDatabaseTaskStore (48 hours)

### Replicated Scenario
- Late-arriving events can still be processed
- MainQueue stays in map until finalization
- Each instance manages own queue lifecycle

---

## Why Two Levels?

**Level 1** (Cleanup Callback):
- Prevents removal from map for non-final tasks
- Enables resubscription after queue close

**Level 2** (Auto-Close Prevention):
- Prevents auto-close when all children disconnect
- Keeps queue alive for late arrivals
- Supports fire-and-forget pattern

**Together**: Guarantee that queues stay available for non-final tasks while cleaning up promptly when tasks complete.

---

## Related Documentation

- **[Main Overview](../EVENTQUEUE.md)** - Architecture and components
- **[Request Flows](FLOWS.md)** - How cleanup integrates with request handling
- **[Scenarios](SCENARIOS.md)** - Real-world usage patterns
