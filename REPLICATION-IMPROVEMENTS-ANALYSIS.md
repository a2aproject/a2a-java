# Analysis for ReplicatedQueueManager Improvements

This document summarizes the brainstorming and analysis for improving `ReplicatedQueueManager` to ensure a consistent, cluster-wide queue lifecycle for tasks.

## Primary Goal

Ensure that for any given `taskId` corresponding to a non-final `Task`, a `MainQueue` effectively exists across the entire cluster. When the `Task` reaches a final state, the `MainQueue` and its resources should be cleaned up on all nodes.

---

### Challenge 1: Cluster-Wide State Synchronization

**Problem:** How to reliably inform all nodes in a cluster that a `MainQueue` should be created or destroyed for a given `taskId`.

**Recommended Solution:**
1.  **Infer state from a shared database** rather than sending explicit `QueueCreated`/`QueueClosed` events over Kafka. This avoids race conditions and complex state reconstruction for new servers.
2.  To manage the coupling between the queue manager and the data store, create a new interface: `ReplicatedTaskStateProvider`.
3.  `ReplicatedQueueManager` will depend on this interface.
4.  The initial implementation, `JpaTaskStateProvider`, will use the existing `JpaDatabaseTaskStore` to check if a task is active.

---

### Challenge 2: Race Condition for In-Flight Events

**Problem:** An event is published to Kafka while a task is active. Before the event is consumed on a replica node, the task's state is updated to `COMPLETED` in the database. The replica node then incorrectly discards the valid, in-flight event.

**Recommended Solution:**
1.  Implement a **Grace Period**. Avoids complex timestamp comparisons and clock-skew issues.
2.  The `ReplicatedTaskStateProvider` will consider a task "active" if its state is not final, **OR** if its state is final but it was finalized less than a configurable `N` seconds ago.
3.  This grace period should be longer than the maximum expected Kafka replication lag, ensuring in-flight events are accepted.

---

### Challenge 3: Cross-Node Consumer Cleanup

**Problem:** A consumer on Server B is actively polling a queue for a task. The task is finalized on Server A. The consumer on Server B needs to be notified to stop polling and terminate its client connection.

**Recommended Solution:**
1.  Use a **"Poison Pill" Event**.
2.  When a task is finalized, the originating node broadcasts a special, replicated `QueueClosedEvent(taskId)` to the Kafka topic.
3.  This event is consumed by all `EventConsumer` instances for that task. Upon receiving it, the consumer knows the stream is terminated and shuts down gracefully.
4.  **Prerequisite:** To guarantee order, all events for a given `taskId` (including the `QueueClosedEvent`) must be published to the **same Kafka partition**.

---

### Challenge 4: Duplicate Database Writes

**Problem:** If a consumer on a replica node processes a replicated event, it might call `TaskManager.process()`, resulting in a duplicate write to the shared `TaskStore`.

**Recommended Solution:**
1.  **Mark the Event Origin.** When an event is published to Kafka, the originating node should add metadata (e.g., an `origin_node_id`).
2.  When a replica node receives an event from Kafka, it should flag it internally as a "replicated event" before placing it in the local queue.
3.  The `ResultAggregator` must be updated to check for this flag. If the flag is present, it **must not** call `TaskManager.process()`. It should only use the event for streaming to its local subscribers.
