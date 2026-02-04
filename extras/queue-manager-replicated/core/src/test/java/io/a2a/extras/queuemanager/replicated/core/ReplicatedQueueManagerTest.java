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
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.AfterEach;
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
        // Create MainEventBus first
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();

        // Create QueueManager before MainEventBusProcessor (processor needs it as parameter)
        queueManager = new ReplicatedQueueManager(
            new NoOpReplicationStrategy(),
            new MockTaskStateProvider(true),
            mainEventBus
        );

        // Create MainEventBusProcessor with QueueManager
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        testEvent = TaskStatusUpdateEvent.builder()
                .taskId("test-task")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();
    }

    /**
     * Helper to create a test event with the specified taskId.
     * This ensures taskId consistency between queue creation and event creation.
     */
    private TaskStatusUpdateEvent createEventForTask(String taskId) {
        return TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId("test-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (mainEventBusProcessor != null) {
            mainEventBusProcessor.setCallback(null);  // Clear any test callbacks
            EventQueueUtil.stop(mainEventBusProcessor);
        }
        mainEventBusProcessor = null;
        mainEventBus = null;
        queueManager = null;
    }

    /**
     * Helper to wait for MainEventBusProcessor to process an event.
     * Replaces polling patterns with deterministic callback-based waiting.
     *
     * @param action the action that triggers event processing
     * @throws InterruptedException if waiting is interrupted
     * @throws AssertionError if processing doesn't complete within timeout
     */
    private void waitForEventProcessing(Runnable action) throws InterruptedException {
        CountDownLatch processingLatch = new CountDownLatch(1);
        mainEventBusProcessor.setCallback(new io.a2a.server.events.MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, io.a2a.spec.Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // Not needed for basic event processing wait
            }
        });

        try {
            action.run();
            assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "MainEventBusProcessor should have processed the event within timeout");
        } finally {
            mainEventBusProcessor.setCallback(null);
        }
    }

    @Test
    void testReplicationStrategyTriggeredOnNormalEnqueue() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy, new MockTaskStateProvider(true), mainEventBus);

        String taskId = "test-task-1";
        EventQueue queue = queueManager.createOrTap(taskId);
        TaskStatusUpdateEvent event = createEventForTask(taskId);

        // Wait for MainEventBusProcessor to process the event and trigger replication
        waitForEventProcessing(() -> queue.enqueueEvent(event));

        assertEquals(1, strategy.getCallCount());
        assertEquals(taskId, strategy.getLastTaskId());
        assertEquals(event, strategy.getLastEvent());
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
        TaskStatusUpdateEvent event = createEventForTask(taskId);

        // Wait for MainEventBusProcessor to process each event
        waitForEventProcessing(() -> queue.enqueueEvent(event));
        waitForEventProcessing(() -> queue.enqueueEvent(event));

        assertEquals(2, countingStrategy.getCallCount());
        assertEquals(taskId, countingStrategy.getLastTaskId());
        assertEquals(event, countingStrategy.getLastEvent());

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        assertEquals(2, countingStrategy.getCallCount());
    }

    @Test
    void testReplicatedEventDeliveredToCorrectQueue() throws InterruptedException {
        String taskId = "test-task-4";
        TaskStatusUpdateEvent eventForTask = createEventForTask(taskId);  // Use matching taskId
        EventQueue queue = queueManager.createOrTap(taskId);

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, eventForTask);

        // Use callback to wait for event processing
        EventQueueItem item = dequeueEventWithRetry(queue, () -> queueManager.onReplicatedEvent(replicatedEvent));
        assertNotNull(item, "Event should be available in queue");
        Event dequeuedEvent = item.getEvent();
        assertEquals(eventForTask, dequeuedEvent);
    }

    @Test
    void testReplicatedEventCreatesQueueIfNeeded() throws InterruptedException {
        String taskId = "non-existent-task";
        TaskStatusUpdateEvent eventForTask = createEventForTask(taskId);  // Use matching taskId

        // Verify no queue exists initially
        assertNull(queueManager.get(taskId));

        // Create a ChildQueue BEFORE processing the replicated event
        // This ensures the ChildQueue exists when MainEventBusProcessor distributes the event
        EventQueue childQueue = queueManager.createOrTap(taskId);
        assertNotNull(childQueue, "ChildQueue should be created");

        // Verify MainQueue was created
        EventQueue mainQueue = queueManager.get(taskId);
        assertNotNull(mainQueue, "MainQueue should exist after createOrTap");

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, eventForTask);

        // Process the replicated event and wait for distribution
        // Use callback to wait for event processing
        EventQueueItem item = dequeueEventWithRetry(childQueue, () -> {
            assertDoesNotThrow(() -> queueManager.onReplicatedEvent(replicatedEvent));
        });
        assertNotNull(item, "Event should be available in queue");
        Event dequeuedEvent = item.getEvent();
        assertEquals(eventForTask, dequeuedEvent, "The replicated event should be enqueued in the newly created queue");
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
        TaskStatusUpdateEvent event = createEventForTask(taskId);

        EventQueue queue = queueManager.createOrTap(taskId);
        waitForEventProcessing(() -> queue.enqueueEvent(event));

        assertEquals(taskId, countingStrategy.getLastTaskId());

        queueManager.close(taskId);  // Task is active, so NO poison pill is sent

        EventQueue newQueue = queueManager.createOrTap(taskId);
        waitForEventProcessing(() -> newQueue.enqueueEvent(event));

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
        int expectedEventCount = (numThreads / 2) * eventsPerThread; // Only normal enqueues
        int totalEventCount = numThreads * eventsPerThread; // All events (normal + replicated)
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        
        // Use CyclicBarrier for better thread synchronization
        // This ensures all threads start their work at approximately the same time
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(numThreads);

        // Track processed events for better diagnostics on failure
        java.util.concurrent.CopyOnWriteArrayList<io.a2a.spec.Event> processedEvents = 
            new java.util.concurrent.CopyOnWriteArrayList<>();

        // Set up callback to wait for ALL events to be processed by MainEventBusProcessor
        // Must wait for all 50 events (25 normal + 25 replicated) to ensure all normal events
        // have triggered replication before we check the count
        CountDownLatch processingLatch = new CountDownLatch(totalEventCount);
        mainEventBusProcessor.setCallback(new io.a2a.server.events.MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String tid, io.a2a.spec.Event event) {
                processedEvents.add(event);
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String tid) {
                // Not needed for this test
            }
        });

        // Launch threads that will enqueue events normally (should trigger replication)
        for (int i = 0; i < numThreads / 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    barrier.await(); // Synchronize thread starts for better interleaving
                    for (int j = 0; j < eventsPerThread; j++) {
                        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                                .taskId(taskId)  // Use same taskId as queue
                                .contextId("test-context")
                                .status(new TaskStatus(TaskState.WORKING))
                                .isFinal(false)
                                .build();
                        queue.enqueueEvent(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.BrokenBarrierException e) {
                    throw new RuntimeException("Barrier broken", e);
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
                    barrier.await(); // Synchronize thread starts for better interleaving
                    for (int j = 0; j < eventsPerThread; j++) {
                        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                                .taskId(taskId)  // Use same taskId as queue
                                .contextId("test-context")
                                .status(new TaskStatus(TaskState.COMPLETED))
                                .isFinal(true)
                                .build();
                        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, event);
                        queueManager.onReplicatedEvent(replicatedEvent);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.BrokenBarrierException e) {
                    throw new RuntimeException("Barrier broken", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete with explicit timeout
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), 
                "All " + numThreads + " threads should complete within 10 seconds");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), 
                "Executor should shutdown within 5 seconds");

        // Wait for MainEventBusProcessor to process all events
        try {
            boolean allProcessed = processingLatch.await(10, TimeUnit.SECONDS);
            assertTrue(allProcessed,
                    String.format("MainEventBusProcessor should have processed all %d events within timeout. " +
                            "Processed: %d, Remaining: %d",
                            totalEventCount, processedEvents.size(), processingLatch.getCount()));
        } finally {
            mainEventBusProcessor.setCallback(null);
            queue.close(true, true);
        }

        // Verify we processed the expected number of events
        assertEquals(totalEventCount, processedEvents.size(),
                "Should have processed exactly " + totalEventCount + " events (normal + replicated)");

        // Only the normal enqueue operations should have triggered replication
        // numThreads/2 threads * eventsPerThread events each = total expected replication calls
        int expectedReplicationCalls = (numThreads / 2) * eventsPerThread;
        assertEquals(expectedReplicationCalls, strategy.getCallCount(),
                String.format("Only normal enqueue operations should trigger replication, not replicated events. " +
                        "Expected: %d, Actual: %d", expectedReplicationCalls, strategy.getCallCount()));
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
        TaskStatusUpdateEvent eventForTask = createEventForTask(taskId);  // Use matching taskId

        // Verify no queue exists initially
        assertNull(queueManager.get(taskId));

        // Create a ChildQueue BEFORE processing the replicated event
        // This ensures the ChildQueue exists when MainEventBusProcessor distributes the event
        EventQueue childQueue = queueManager.createOrTap(taskId);
        assertNotNull(childQueue, "ChildQueue should be created");

        // Verify MainQueue was created
        EventQueue mainQueue = queueManager.get(taskId);
        assertNotNull(mainQueue, "MainQueue should exist after createOrTap");

        // Process a replicated event for an active task
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, eventForTask);

        // Verify the event was enqueued and distributed to our ChildQueue
        // Use callback to wait for event processing
        EventQueueItem item = dequeueEventWithRetry(childQueue, () -> queueManager.onReplicatedEvent(replicatedEvent));
        assertNotNull(item, "Event should be available in queue");
        Event dequeuedEvent = item.getEvent();
        assertEquals(eventForTask, dequeuedEvent, "Event should be enqueued for active task");
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
        TaskStatusUpdateEvent event = createEventForTask(taskId);

        // Enqueue a normal event first and wait for processing
        waitForEventProcessing(() -> queue.enqueueEvent(event));

        // In the new architecture, QueueClosedEvent (poison pill) is sent via CDI events
        // when JpaDatabaseTaskStore.save() persists a final task and the transaction commits
        // ReplicatedQueueManager.onTaskFinalized() observes AFTER_SUCCESS and sends the poison pill

        // Simulate the CDI event observer being called (what happens in real execution)
        // Create a final task for the event
        Task finalTask = Task.builder()
                .id(taskId)
                .contextId("test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();
        TaskFinalizedEvent taskFinalizedEvent = new TaskFinalizedEvent(taskId, finalTask);

        // Call the observer method directly (simulating CDI event delivery)
        queueManager.onTaskFinalized(taskFinalizedEvent);

        // Verify that final Task and QueueClosedEvent were replicated
        // strategy.getCallCount() should be 3: testEvent, final Task, then QueueClosedEvent (poison pill)
        assertEquals(3, strategy.getCallCount(), "Should have replicated testEvent, final Task, and QueueClosedEvent");

        // Verify the last event is QueueClosedEvent (poison pill)
        Event lastEvent = strategy.getLastEvent();
        assertTrue(lastEvent instanceof QueueClosedEvent, "Last replicated event should be QueueClosedEvent (poison pill)");
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
        TaskStatusUpdateEvent eventForTask = createEventForTask(taskId);  // Use matching taskId
        EventQueue queue = queueManager.createOrTap(taskId);

        // Simulate receiving QueueClosedEvent from remote node
        QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);
        ReplicatedEventQueueItem replicatedClosedEvent = new ReplicatedEventQueueItem(taskId, closedEvent);

        // Dequeue the normal event first (use callback to wait for async processing)
        EventQueueItem item1 = dequeueEventWithRetry(queue, () -> queue.enqueueEvent(eventForTask));
        assertNotNull(item1, "First event should be available");
        assertEquals(eventForTask, item1.getEvent());

        // Next dequeue should get the QueueClosedEvent (use callback to wait for async processing)
        EventQueueItem item2 = dequeueEventWithRetry(queue, () -> queueManager.onReplicatedEvent(replicatedClosedEvent));
        assertNotNull(item2, "QueueClosedEvent should be available");
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

    /**
     * Helper method to dequeue an event after waiting for MainEventBusProcessor distribution.
     * Uses callback-based waiting instead of polling for deterministic synchronization.
     *
     * @param queue the queue to dequeue from
     * @param enqueueAction the action that enqueues the event (triggers event processing)
     * @return the dequeued EventQueueItem, or null if queue is closed
     */
    private EventQueueItem dequeueEventWithRetry(EventQueue queue, Runnable enqueueAction) throws InterruptedException {
        // Wait for event to be processed and distributed
        waitForEventProcessing(enqueueAction);

        // Event is now available, dequeue directly
        try {
            return queue.dequeueEventItem(100);
        } catch (EventQueueClosedException e) {
            // Queue closed, return null
            return null;
        }
    }
}