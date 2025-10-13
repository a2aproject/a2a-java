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

import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueClosedException;
import io.a2a.server.events.EventQueueTestHelper;
import io.a2a.spec.Event;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReplicatedQueueManagerTest {

    private ReplicatedQueueManager queueManager;
    private StreamingEventKind testEvent;

    @BeforeEach
    void setUp() {
        queueManager = new ReplicatedQueueManager();
        testEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("test-task")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();
    }

    @Test
    void testReplicationStrategyTriggeredOnNormalEnqueue() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy);

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
        queueManager = new ReplicatedQueueManager(strategy);

        String taskId = "test-task-2";
        EventQueue queue = queueManager.createOrTap(taskId);

        ReplicatedEvent replicatedEvent = new ReplicatedEvent(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        assertEquals(0, strategy.getCallCount());
    }

    @Test
    void testReplicationStrategyWithCountingImplementation() throws InterruptedException {
        CountingReplicationStrategy countingStrategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(countingStrategy);

        String taskId = "test-task-3";
        EventQueue queue = queueManager.createOrTap(taskId);

        queue.enqueueEvent(testEvent);
        queue.enqueueEvent(testEvent);

        assertEquals(2, countingStrategy.getCallCount());
        assertEquals(taskId, countingStrategy.getLastTaskId());
        assertEquals(testEvent, countingStrategy.getLastEvent());

        ReplicatedEvent replicatedEvent = new ReplicatedEvent(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        assertEquals(2, countingStrategy.getCallCount());
    }

    @Test
    void testReplicatedEventDeliveredToCorrectQueue() throws InterruptedException {
        String taskId = "test-task-4";
        EventQueue queue = queueManager.createOrTap(taskId);

        ReplicatedEvent replicatedEvent = new ReplicatedEvent(taskId, testEvent);
        queueManager.onReplicatedEvent(replicatedEvent);

        Event dequeuedEvent;
        try {
            dequeuedEvent = queue.dequeueEvent(100);
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

        ReplicatedEvent replicatedEvent = new ReplicatedEvent(taskId, testEvent);

        // Process the replicated event
        assertDoesNotThrow(() -> queueManager.onReplicatedEvent(replicatedEvent));

        // Verify that a queue was created and the event was enqueued
        EventQueue queue = queueManager.get(taskId);
        assertNotNull(queue, "Queue should be created when processing replicated event for non-existent task");

        // Verify the event was enqueued by dequeuing it
        Event dequeuedEvent;
        try {
            dequeuedEvent = queue.dequeueEvent(100);
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
        queueManager = new ReplicatedQueueManager(countingStrategy);

        EventQueue queue = queueManager.createOrTap(taskId);
        queue.enqueueEvent(testEvent);

        assertEquals(taskId, countingStrategy.getLastTaskId());

        queueManager.close(taskId);

        EventQueue newQueue = queueManager.createOrTap(taskId);
        newQueue.enqueueEvent(testEvent);

        assertEquals(taskId, countingStrategy.getLastTaskId());
        assertEquals(2, countingStrategy.getCallCount());
    }

    @Test
    void testReplicatedEventJsonSerialization() throws Exception {
        // Test that ReplicatedEvent can be properly serialized and deserialized with StreamingEventKind
        TaskStatusUpdateEvent originalEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("json-test-task")
                .contextId("json-test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();
        ReplicatedEvent original = new ReplicatedEvent("json-test-task", originalEvent);

        // Serialize to JSON
        String json = Utils.OBJECT_MAPPER.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("json-test-task"));
        assertTrue(json.contains("\"event\":{"));
        assertTrue(json.contains("\"kind\":\"status-update\""));

        // Deserialize back
        ReplicatedEvent deserialized = Utils.OBJECT_MAPPER.readValue(json, ReplicatedEvent.class);
        assertNotNull(deserialized);
        assertEquals("json-test-task", deserialized.getTaskId());
        assertNotNull(deserialized.getEvent());
        assertTrue(deserialized.hasEvent());
        assertFalse(deserialized.hasError());
    }

    @Test
    void testParallelReplicationBehavior() throws InterruptedException {
        CountingReplicationStrategy strategy = new CountingReplicationStrategy();
        queueManager = new ReplicatedQueueManager(strategy);

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
                        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
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
                        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                                .taskId("replicated-" + threadId + "-" + j)
                                .contextId("test-context")
                                .status(new TaskStatus(TaskState.COMPLETED))
                                .isFinal(true)
                                .build();
                        ReplicatedEvent replicatedEvent = new ReplicatedEvent(taskId, event);
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

}