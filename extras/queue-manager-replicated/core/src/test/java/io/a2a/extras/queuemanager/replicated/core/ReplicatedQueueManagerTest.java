package io.a2a.extras.queuemanager.replicated.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.a2a.extras.common.events.TaskFinalizedEvent;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueClosedException;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.EventQueueTestHelper;
import io.a2a.server.events.EventQueueUtil;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.events.QueueClosedEvent;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.Event;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplicatedQueueManagerTest {

    private ReplicatedQueueManager queueManager;
    private StreamingEventKind testEvent;
    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;
    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    @BeforeEach
    void setUp() {
        // Create MainEventBus and MainEventBusProcessor for tests
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER);
        EventQueueUtil.start(mainEventBusProcessor);

        queueManager = new ReplicatedQueueManager(
            new NoOpReplicationStrategy(),
            new MockTaskStateProvider(true),
            mainEventBus
        );

        testEvent = TaskStatusUpdateEvent.builder()
                .taskId("test-task")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();
    }

    @Test
    void testReplicationStrategyTriggeredOnNormalEnqueue() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "test-task-1";
        EventQueue queue = queueManager.createOrTap(taskId);

        queue.enqueueEvent(testEvent);

        assertEquals(1, strategy.getCallCount());
        assertEquals(taskId, strategy.getLastTaskId());
        assertEquals(testEvent, strategy.getLastEvent());
    }

    @Test
    void testReplicationStrategyNotTriggeredOnReplicatedEvent() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "test-task-2";
        EventQueue queue = queueManager.createOrTap(taskId);

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        assertEquals(0, strategy.getCallCount());
    }

    @Test
    void testReplicationStrategyWithCountingImplementation() throws InterruptedException {
        CountingReplicationStrategy countingStrategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(countingStrategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "test-task-3";
        EventQueue queue = queueManager.createOrTap(taskId);

        queue.enqueueEvent(testEvent);
        queue.enqueueEvent(testEvent);

        assertEquals(2, countingStrategy.getCallCount());
        assertEquals(taskId, countingStrategy.getLastTaskId());
        assertEquals(testEvent, countingStrategy.getLastEvent());

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        assertEquals(2, countingStrategy.getCallCount());
    }

    @Test
    void testReplicatedEventDeliveredToCorrectQueue() throws InterruptedException {
        String taskId = "test-task-4";
        EventQueue queue = queueManager.createOrTap(taskId);

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        Event dequeuedEvent;
        try {
            dequeuedEvent = queue.dequeueEventItem(100).getEvent();
        } catch (EventQueueClosedException e) {
            fail("Queue should not be closed");
            return;
        }
        assertEquals(testEvent, dequeuedEvent);
    }

    @Test
    void testReplicatedEventCreatesQueueIfNeeded() throws InterruptedException {
        String taskId = "non-existent-task";

        // Verify no queue exists initially
        assertNull(queueManager.get(taskId));

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);

        // Process the replicated event
        assertDoesNotThrow(() -> queueManager.onReplicatedEvent(replicatedEvent));

        // Verify that a queue was created and the event was enqueued
        EventQueue queue = queueManager.get(taskId);
        assertNotNull(queue, "Queue should be created when processing replicated event for non-existent task");

        // Verify the event was enqueued by dequeuing it
        // Need to tap() the MainQueue to get a ChildQueue for consumption
        EventQueue childQueue = queue.tap();
        Event dequeuedEvent;
        try {
            dequeuedEvent = childQueue.dequeueEventItem(100).getEvent();
        } catch (EventQueueClosedException e) {
            fail("Queue should not be closed");
            return;
        }
        assertEquals(testEvent, dequeuedEvent, "The replicated event should be enqueued in the newly created queue");
    }

    @Test
    void testBasicQueueManagerFunctionality() throws InterruptedException {
        String taskId = "test-task-5";

        assertNull(queueManager.get(taskId));
        assertNull(queueManager.tap(taskId));

        EventQueue queue = queueManager.createOrTap(taskId);
        assertNotNull(queue);

        // createOrTap now returns ChildQueue, get returns MainQueue
        EventQueue retrievedQueue = queueManager.get(taskId);
        assertNotNull(retrievedQueue);
        // queue should be a ChildQueue (cannot be tapped)
        assertThrows(IllegalStateException.class, () -> EventQueueTestHelper.tapQueue(queue));

        EventQueue tappedQueue = queueManager.tap(taskId);
        assertNotNull(tappedQueue);
        assertNotEquals(queue, tappedQueue);

        queueManager.close(taskId);
        assertNull(queueManager.get(taskId));
    }

    @Test
    void testQueueToTaskIdMappingMaintained() throws InterruptedException {
        String taskId = "test-task-6";
        CountingReplicationStrategy countingStrategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(countingStrategy, new MockTaskStateProvider(true), mainEventBus);

        EventQueue queue = queueManager.createOrTap(taskId);
        queue.enqueueEvent(testEvent);

        assertEquals(taskId, countingStrategy.getLastTaskId());

        queueManager.close(taskId);  // Task is active, so NO poison pill is sent

        EventQueue newQueue = queueManager.createOrTap(taskId);
        newQueue.enqueueEvent(testEvent);

        assertEquals(taskId, countingStrategy.getLastTaskId());
        // 2 replication calls: 1 testEvent, 1 testEvent (no QueueClosedEvent because task is active)
        assertEquals(2, countingStrategy.getCallCount());
    }

    @Test
    void testReplicatedEventJsonSerialization() throws Exception {
        // Test that ReplicatedEventQueueItem can be properly serialized and deserialized with StreamingEventKind
        TaskStatusUpdateEvent originalEvent = TaskStatusUpdateEvent.builder()
                .taskId("json-test-task")
                .contextId("json-test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();
        ReplicatedEventQueueItem original = new ReplicatedEventQueueItem("json-test-task", originalEvent);

        // Serialize to JSON
        String json = JsonUtil.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains("json-test-task"));
        assertTrue(json.contains("\"event\":{"));
        assertTrue(json.contains("\"statusUpdate\""));

        // Deserialize back
        ReplicatedEventQueueItem deserialized = JsonUtil.fromJson(json, ReplicatedEventQueueItem.class);
        assertNotNull(deserialized);
        assertEquals("json-test-task", deserialized.getTaskId());
        assertNotNull(deserialized.getEvent());
        assertTrue(deserialized.hasEvent());
        assertFalse(deserialized.hasError());
    }

    @Test
    void testParallelReplicationBehavior() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "parallel-test-task";
        EventQueue queue = queueManager.createOrTap(taskId);

        int numThreads = 10;
        int eventsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        // Launch threads that will enqueue events normally (should trigger replication)
        for (int i = 0; i < numThreads / 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < eventsPerThread; j++) {
                        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                                .taskId("normal-" + threadId + "-" + j)
                                .contextId("test-context")
                                .status(new TaskStatus(TaskState.WORKING))
                                .isFinal(false)
                                .build();
                        queue.enqueueEvent(event);
                        Thread.sleep(1); // Small delay to interleave operations
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Launch threads that will send replicated events (should NOT trigger replication)
        for (int i = numThreads / 2; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < eventsPerThread; j++) {
                        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                                .taskId("replicated-" + threadId + "-" + j)
                                .contextId("test-context")
                                .status(new TaskStatus(TaskState.COMPLETED))
                                .isFinal(true)
                                .build();
                        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, event);
                        queueManager.onReplicatedEvent(replicatedEvent);
                        Thread.sleep(1); // Small delay to interleave operations
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should complete within 10 seconds");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should shutdown within 5 seconds");

        // Only the normal enqueue operations should have triggered replication
        // numThreads/2 threads * eventsPerThread events each = total expected replication calls
        int expectedReplicationCalls = (numThreads / 2) * eventsPerThread;
        assertEquals(expectedReplicationCalls, strategy.getCallCount(),
                "Only normal enqueue operations should trigger replication, not replicated events");
    }

    @Test
    void testReplicatedEventSkippedWhenTaskInactive() throws InterruptedException {
        // Create a task state provider that returns false (task is inactive)
        MockTaskStateProvider stateProvider = new MockTaskStateProvider(false);
        queueManager = new ReplicatedQueueManager(new CountingReplicationStrategy(), stateProvider, mainEventBus);

        String taskId = "inactive-task";

        // Verify no queue exists initially
        assertNull(queueManager.get(taskId));

        // Process a replicated event for an inactive task
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        // Queue should NOT be created because task is inactive
        assertNull(queueManager.get(taskId), "Queue should not be created for inactive task");
    }

    @Test
    void testReplicatedEventProcessedWhenTaskActive() throws InterruptedException {
        // Create a task state provider that returns true (task is active)
        MockTaskStateProvider stateProvider = new MockTaskStateProvider(true);
        queueManager = new ReplicatedQueueManager(new CountingReplicationStrategy(), stateProvider, mainEventBus);

        String taskId = "active-task";

        // Verify no queue exists initially
        assertNull(queueManager.get(taskId));

        // Process a replicated event for an active task
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        // Queue should be created and event should be enqueued
        EventQueue queue = queueManager.get(taskId);
        assertNotNull(queue, "Queue should be created for active task");

        // Verify the event was enqueued
        // Need to tap() the MainQueue to get a ChildQueue for consumption
        EventQueue childQueue = queue.tap();
        Event dequeuedEvent;
        try {
            dequeuedEvent = childQueue.dequeueEventItem(100).getEvent();
        } catch (EventQueueClosedException e) {
            fail("Queue should not be closed");
            return;
        }
        assertEquals(testEvent, dequeuedEvent, "Event should be enqueued for active task");
    }


    @Test
    void testReplicatedEventToExistingQueueWhenTaskBecomesInactive() throws InterruptedException {
        // Create a task state provider that returns true initially
        MockTaskStateProvider stateProvider = new MockTaskStateProvider(true);
        queueManager = new ReplicatedQueueManager(new CountingReplicationStrategy(), stateProvider, mainEventBus);

        String taskId = "task-becomes-inactive";

        // Create queue and enqueue an event
        EventQueue queue = queueManager.createOrTap(taskId);
        queue.enqueueEvent(testEvent);

        // Dequeue to clear the queue
        try {
            queue.dequeueEventItem(100);
        } catch (EventQueueClosedException e) {
            fail("Queue should not be closed");
        }

        // Now mark task as inactive
        stateProvider.setActive(false);

        // Process a replicated event - should be skipped
        TaskStatusUpdateEvent newEvent = TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId("test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, newEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        // Try to dequeue with a short timeout - should timeout (no new event)
        try {
            EventQueueItem item = queue.dequeueEventItem(100);
            assertNull(item, "No event should be enqueued for inactive task");
        } catch (EventQueueClosedException e) {
            fail("Queue should not be closed");
        }
    }

    @Test
    void testPoisonPillSentViaTransactionAwareEvent() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "poison-pill-test";
        EventQueue queue = queueManager.createOrTap(taskId);

        // Enqueue a normal event first
        queue.enqueueEvent(testEvent);

        // In the new architecture, QueueClosedEvent (poison pill) is sent via CDI events
        // when JpaDatabaseTaskStore.save() persists a final task and the transaction commits
        // ReplicatedQueueManager.onTaskFinalized() observes AFTER_SUCCESS and sends the poison pill

        // Simulate the CDI event observer being called (what happens in real execution)
        TaskFinalizedEvent taskFinalizedEvent = new TaskFinalizedEvent(taskId);

        // Call the observer method directly (simulating CDI event delivery)
        queueManager.onTaskFinalized(taskFinalizedEvent);

        // Verify that QueueClosedEvent was replicated
        // strategy.getCallCount() should be 2: one for testEvent, one for QueueClosedEvent
        assertEquals(2, strategy.getCallCount(), "Should have replicated both normal event and QueueClosedEvent");

        Event lastEvent = strategy.getLastEvent();
        assertTrue(lastEvent instanceof QueueClosedEvent, "Last replicated event should be QueueClosedEvent");
        assertEquals(taskId, ((QueueClosedEvent) lastEvent).getTaskId());
    }

    @Test
    void testQueueClosedEventJsonSerialization() throws Exception {
        // Test that ReplicatedEventQueueItem can serialize/deserialize QueueClosedEvent
        String taskId = "closed-event-json-test";
        QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);
        ReplicatedEventQueueItem original = new ReplicatedEventQueueItem(taskId, closedEvent);

        // Verify the item is marked as closed event
        assertTrue(original.isClosedEvent(), "Should be marked as closed event");
        assertFalse(original.hasEvent(), "Should not have regular event");
        assertFalse(original.hasError(), "Should not have error");

        // Serialize to JSON
        String json = JsonUtil.toJson(original);
        assertNotNull(json);
        assertTrue(json.contains(taskId), "JSON should contain taskId");
        assertTrue(json.contains("\"closedEvent\":true"), "JSON should contain closedEvent flag");

        // Deserialize back
        ReplicatedEventQueueItem deserialized = JsonUtil.fromJson(json, ReplicatedEventQueueItem.class);
        assertNotNull(deserialized);
        assertEquals(taskId, deserialized.getTaskId());
        assertTrue(deserialized.isClosedEvent(), "Deserialized should be marked as closed event");
        assertFalse(deserialized.hasEvent(), "Deserialized should not have regular event");
        assertFalse(deserialized.hasError(), "Deserialized should not have error");

        // Verify getEvent() returns QueueClosedEvent
        Event reconstructedEvent = deserialized.getEvent();
        assertNotNull(reconstructedEvent);
        assertTrue(reconstructedEvent instanceof QueueClosedEvent,
                "getEvent() should return QueueClosedEvent");
        assertEquals(taskId, ((QueueClosedEvent) reconstructedEvent).getTaskId());
    }

    @Test
    void testReplicatedQueueClosedEventTerminatesConsumer() throws InterruptedException {
        String taskId = "remote-close-test";
        EventQueue queue = queueManager.createOrTap(taskId);

        // Enqueue a normal event
        queue.enqueueEvent(testEvent);

        // Simulate receiving QueueClosedEvent from remote node
        QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);
        ReplicatedEventQueueItem replicatedClosedEvent = new ReplicatedEventQueueItem(taskId, closedEvent);
        queueManager.onReplicatedEvent(replicatedClosedEvent);

        // Dequeue the normal event first
        EventQueueItem item1;
        try {
            item1 = queue.dequeueEventItem(100);
        } catch (EventQueueClosedException e) {
            fail("Should not throw on first dequeue");
            return;
        }
        assertNotNull(item1);
        assertEquals(testEvent, item1.getEvent());

        // Next dequeue should get the QueueClosedEvent
        EventQueueItem item2;
        try {
            item2 = queue.dequeueEventItem(100);
        } catch (EventQueueClosedException e) {
            fail("Should not throw on second dequeue, should return the event");
            return;
        }
        assertNotNull(item2);
        assertTrue(item2.getEvent() instanceof QueueClosedEvent,
                "Second event should be QueueClosedEvent");
    }

    private static class NoOpReplicationStrategy implements ReplicationStrategy {
        @Override
        public void send(String taskId, Event event) {
            // No-op for tests that don't care about replication
        }
    }

    private static class CountingReplicationStrategy implements ReplicationStrategy {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private volatile String lastTaskId;
        private volatile Event lastEvent;

        @Override
        public void send(String taskId, Event event) {
            callCount.incrementAndGet();
            this.lastTaskId = taskId;
            this.lastEvent = event;
        }

        public int getCallCount() {
            return callCount.get();
        }

        public String getLastTaskId() {
            return lastTaskId;
        }

        public Event getLastEvent() {
            return lastEvent;
        }
    }


    private static class MockTaskStateProvider implements io.a2a.server.tasks.TaskStateProvider {
        private volatile boolean active;

        public MockTaskStateProvider(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isTaskActive(String taskId) {
            return active;
        }

        @Override
        public boolean isTaskFinalized(String taskId) {
            return !active;  // If task is inactive, it's finalized; if active, not finalized
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}