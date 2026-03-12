# EventQueue Architecture - A2A Java SDK

> **Quick Reference** for event processing, queue management, and task lifecycle

## Overview

The EventQueue architecture guarantees:
1. **Events persist BEFORE clients see them** (no unpersisted events visible)
2. **Serial processing** eliminates concurrent update race conditions
3. **Task state drives queue lifecycle** (fire-and-forget support, late reconnections)

## Architecture Diagram

```
AgentExecutor.execute() [YOUR CODE]
    ↓
AgentEmitter → MainQueue.enqueueEvent()
    ↓
MainEventBus.submit() [ALL events queue here FIRST]
    ↓
MainEventBusProcessor.take() [single background thread]
    ↓
1. TaskStore.save() FIRST ← Persist before visibility
2. PushNotificationSender.send()
3. MainQueue.distributeToChildren() ← Clients see LAST
    ↓
ChildQueue → EventConsumer → ResultAggregator → Client
```

**Key Insight**: All events flow through a single-threaded processor that persists events BEFORE distributing to clients.

---

## Core Components

### MainEventBus
**Location**: `server-common/.../events/MainEventBus.java`

- `@ApplicationScoped` CDI bean - single instance shared by all MainQueues
- `LinkedBlockingDeque<MainEventBusContext>` - thread-safe centralized queue
- `submit(taskId, eventQueue, item)` - enqueue events (called by MainQueue)
- `take()` - blocking consumption (called by MainEventBusProcessor)

**Guarantees**: Events persist BEFORE distribution, serial processing, push notifications AFTER persistence

### MainEventBusProcessor
**Location**: `server-common/.../events/MainEventBusProcessor.java`

Single background thread "MainEventBusProcessor" that processes events in order:
1. `TaskManager.process(event)` → persist to TaskStore
2. `PushNotificationSender.send()` → notifications
3. `mainQueue.distributeToChildren()` → clients receive

**Exception Handling**: Converts `TaskStoreException` to `InternalError` events, continues processing

### EventQueue System
**Location**: `server-common/.../events/EventQueue.java`

**Queue Types**:
- **MainQueue**: No local queue - events submit directly to MainEventBus
- **ChildQueue**: Has local queue for client consumption

**Characteristics**: Bounded (1000 events), thread-safe, graceful shutdown, hook support

### QueueManager
**Location**: `server-common/.../events/QueueManager.java`

- `createOrTap(taskId)` → Get existing MainQueue or create new
- `tap(taskId)` → Create ChildQueue for existing MainQueue
- **Default**: InMemoryQueueManager (thread-safe ConcurrentHashMap)
- **Replicated**: ReplicatedQueueManager (Kafka-based)

### EventConsumer & ResultAggregator
**Locations**: `server-common/.../events/EventConsumer.java`, `server-common/.../tasks/ResultAggregator.java`

**EventConsumer**: Polls queue, returns `Flow.Publisher<Event>`, closes queue on final event

**ResultAggregator** bridges EventConsumer and DefaultRequestHandler:
- `consumeAndBreakOnInterrupt()` - Non-streaming (polls until terminal/AUTH_REQUIRED)
- `consumeAndEmit()` - Streaming (returns Flow.Publisher immediately)
- `consumeAll()` - Simple consumption

---

## Key Concepts

### Queue Structure
- MainQueue has NO local queue (events → MainEventBus directly)
- Only ChildQueues have local queues
- `MainQueue.dequeueEventItem()` throws `UnsupportedOperationException`
- `MainQueue.size()` returns `mainEventBus.size()`
- `ChildQueue.size()` returns local queue size

### Terminal Events
Events that cause polling loop exit:
- `TaskStatusUpdateEvent` with `isFinal() == true`
- `Message` (legacy)
- `Task` with state: COMPLETED, CANCELED, FAILED, REJECTED, UNKNOWN

### AUTH_REQUIRED Special Case
- Returns task to client immediately
- Agent continues in background
- Queue stays open, async cleanup
- Future events update TaskStore

---

## Deep Dives

For detailed documentation on specific aspects:

- **[Queue Lifecycle & Two-Level Protection](eventqueue/LIFECYCLE.md)**
  - THE BIG IDEA: fire-and-forget, late reconnections
  - TaskStateProvider interface and state-driven cleanup
  - Memory management and cleanup modes

- **[Request Flows](eventqueue/FLOWS.md)**
  - Non-streaming vs streaming flows
  - DefaultRequestHandler orchestration
  - Background cleanup patterns

- **[Usage Scenarios & Pitfalls](eventqueue/SCENARIOS.md)**
  - Fire-and-forget pattern (TCK)
  - Late resubscription scenarios
  - Tapping and multiple consumers
  - Common mistakes to avoid

---

## Key Files Reference

| Component | Path |
|-----------|------|
| MainEventBus | `server-common/.../events/MainEventBus.java` |
| MainEventBusProcessor | `server-common/.../events/MainEventBusProcessor.java` |
| EventQueue | `server-common/.../events/EventQueue.java` |
| QueueManager | `server-common/.../events/QueueManager.java` |
| InMemoryQueueManager | `server-common/.../events/InMemoryQueueManager.java` |
| EventConsumer | `server-common/.../events/EventConsumer.java` |
| ResultAggregator | `server-common/.../tasks/ResultAggregator.java` |
| DefaultRequestHandler | `server-common/.../requesthandlers/DefaultRequestHandler.java` |
| TaskStateProvider | `server-common/.../tasks/TaskStateProvider.java` |
| AgentEmitter | `server-common/.../tasks/AgentEmitter.java` |

---

## Related Documentation

- **Main Architecture**: `AGENTS.md` - High-level system overview
- **Task Persistence**: See TaskStore exception handling in main docs
- **Replication**: `extras/queue-manager-replicated/README.md`
