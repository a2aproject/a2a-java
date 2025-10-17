# TODO: Implement Replicated Queue Manager Improvements (Final Plan)

This document outlines the tasks required to make the `ReplicatedQueueManager` robust and consistent in a multi-node environment, based on our collaborative analysis.

---

### Part 1: Foundational Components

**Goal:** Create a new common module for interfaces and classes shared by different `extras` modules.

1.  **Task: Create `extras/common` Module**
    *   **Action:** Create a new Maven module at `extras/common`. Update the `extras/pom.xml` and root `pom.xml` to include this new module.
    *   **Verification:** The project builds successfully (`mvn clean install`).

2.  **Task: Define `ReplicatedTaskStateProvider` Interface**
    *   **Action:** Inside `extras/common`, define the public interface `ReplicatedTaskStateProvider` with a single method to start: `boolean isTaskActive(String taskId);`.
    *   **Verification:** The project builds successfully.

---

### Part 2: State Synchronization Logic

**Goal:** Implement the logic to check the authoritative state of a task from the shared database.

1.  **Task: Update `JpaTask` Entity and Add Idempotent Setter**
    *   **Action:** In `extras/task-store-database-jpa`, modify the `JpaTask` entity to include a `finalizedAt` timestamp field.
    *   **Action:** Implement the logic to set this timestamp idempotently (only on the *first* transition to a final state) as a defense-in-depth measure.
    *   **Note:** A formal database migration script is not required at this stage, as there are no production releases of this module.
    *   **Verification:** Tests within `task-store-database-jpa` should be updated and continue to pass.

2.  **Task: Implement `ReplicatedTaskStateProvider` in `JpaDatabaseTaskStore`**
    *   **Action:** Modify `JpaDatabaseTaskStore` to implement `ReplicatedTaskStateProvider`.
    *   **Action:** Implement `isTaskActive(taskId)` to return `true` if the task is not final OR if `now() < finalizedAt + gracePeriod`.
    *   **Action:** Make the grace period duration configurable via MicroProfile Config.
    *   **Verification:** Add a new unit test to `JpaDatabaseTaskStoreTest` covering the `isTaskActive` method.

3.  **Task: Integrate State Provider into `ReplicatedQueueManager`**
    *   **Action:** In `extras/queue-manager-replicated-core`, inject `ReplicatedTaskStateProvider`.
    *   **Action:** In the `onReplicatedEvent` method, use `isTaskActive(taskId)` to decide whether to process a late-arriving event.
    *   **Verification:** Add a unit test mocking the provider to verify that an event for an inactive task is ignored.

---

### Part 3: Polymorphic Event Queue and Replication Handling

**Goal:** Use a common interface for queue items to cleanly distinguish between locally-generated and replicated events, removing the need for a `ThreadLocal`.

1.  **Task: Define `EventQueueItem` Interface and `LocalEventQueueItem` Class**
    *   **Action:** In `server-common`, create a public interface `EventQueueItem` with methods `Event getEvent();` and `boolean isReplicated();`. Create a package-private implementation `LocalEventQueueItem`.
    *   **Verification:** Project builds successfully.

2.  **Task: Refactor `EventQueue` and `EventQueueTest`**
    *   **Action:** Change `EventQueue` to use `BlockingQueue<EventQueueItem>` internally, while preserving its public API by wrapping/unwrapping items.
    *   **Verification:** Fix all broken tests *only* within `EventQueueTest.java` until they pass.

3.  **Task: Refactor `EventConsumer`, `ResultAggregator`, and their tests**
    *   **Action:** Update `EventConsumer` and `ResultAggregator` to handle the `EventQueueItem` wrapper internally.
    *   **Verification:** Fix all broken tests *only* within `EventConsumerTest.java` and `ResultAggregatorTest.java`.

4.  **Task: Refactor `InMemoryQueueManager` and its tests**
    *   **Action:** Ensure `InMemoryQueueManager`'s logic is sound after the `EventQueue` changes.
    *   **Verification:** Fix any failing tests in `InMemoryQueueManagerTest.java`. At this point, all tests in the `server-common` module should pass.

5.  **Task: Refactor Test Helper Endpoints and `AbstractA2AServerTest`**
    *   **Action:** Update the test-only REST/gRPC endpoints (`A2ATestRoutes`, `A2ATestResource`) that directly enqueue events. They must now wrap events in a `LocalEventQueueItem`.
    *   **Verification:** With the helper endpoints fixed, run the tests in `AbstractA2AServerTest` and fix any that fail due to these changes.

6.  **Task: Adapt `ReplicatedQueueManager` and Remove `ThreadLocal`**
    *   **Action:** In `extras/queue-manager-replicated-core`, rename `ReplicatedEvent` to `ReplicatedEventQueueItem` and have it implement the `EventQueueItem` interface. Its `isReplicated()` method will return `true`.
    *   **Action:** Refactor the `ReplicationHook` to check `!item.isReplicated()` instead of using the `ThreadLocal`.
    *   **Action:** Update `ReplicatedQueueManager.onReplicatedEvent` to deserialize the Kafka message into a `ReplicatedEventQueueItem` and enqueue it directly.
    *   **Action:** Delete the `isHandlingReplicatedEvent` `ThreadLocal` entirely.
    *   **Verification:** Update tests in `ReplicatedQueueManagerTest` to verify the new logic and the removal of the `ThreadLocal`.

7.  **Task: Implement Conditional Processing in `ResultAggregator`**
    *   **Action:** Implement the final piece of logic in `ResultAggregator` to check `item.isReplicated()` and conditionally call `TaskManager.process()`.
    *   **Verification:** Add a new unit test to `ResultAggregatorTest` to verify this conditional logic. Add integration tests to verify that replicated events do not cause duplicate database writes.

---

### Part 4: Distributed Cleanup and Documentation

**Goal:** Ensure consumers on all nodes are gracefully terminated and the new system requirements are documented.

1.  **Task: Retrofit `ReplicatedTaskStateProvider` for Finalization Check**
    *   **Action:** Modify the `ReplicatedTaskStateProvider` interface in `extras/common` to add a new method: `boolean isTaskFinalized(String taskId);`.
    *   **Action:** Update `JpaDatabaseTaskStore` to implement this new method. This implementation should check if the task is in a final state, ignoring the grace period.
    *   **Verification:** Update the unit tests for `JpaDatabaseTaskStore` to cover the new `isTaskFinalized` method.

2.  **Task: Implement Authoritative "Poison Pill" Logic**
    *   **Action:** Define a `QueueClosedEvent` class in `io.a2a.server.events` that implements `StreamingEventKind`.
    *   **Action:** Update `EventConsumer` to recognize this event and throw an `EventQueueClosedException` to terminate the stream.
    *   **Action (The Fix):** The `onClose` callback on the `MainQueue` must be modified. To solve the race condition between the queue closing and the database commit, the check must be delayed. When the callback is triggered, it should schedule a delayed task (e.g., using a `ScheduledExecutorService`). This task, after a short, configurable delay (e.g., 500ms), will perform the `isTaskFinalized()` check and then publish the `QueueClosedEvent` if the check passes.
    *   **Verification:** The two integration tests in `KafkaReplicationIntegrationTest` (for pill receipt and generation) should be re-enabled and must now pass reliably.

3.  **Task: Update Documentation**
    *   **Action:** Edit `extras/queue-manager-replicated/README.md`.
    *   **Action:** Add a prominent section on **Kafka Partitioning Strategy**. It should explain both the simple (single partition) and scalable (partitioning by `taskId` key) approaches, recommending the latter for production while noting that examples/tests may use the former for simplicity.
    *   **Action:** Add a section on **Grace Period Configuration**, explaining how to tune the value (`grace_period > max_kafka_lag + safety_margin`) and the importance of monitoring consumer lag.
    *   **Verification:** Manual review of the `README.md` file.