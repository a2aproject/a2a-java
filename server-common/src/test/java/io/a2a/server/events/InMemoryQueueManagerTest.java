package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.MockTaskStateProvider;
import io.a2a.server.tasks.PushNotificationSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InMemoryQueueManagerTest {

    private InMemoryQueueManager queueManager;
    private MockTaskStateProvider taskStateProvider;
    private InMemoryTaskStore taskStore;
    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;
    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    @BeforeEach
    public void setUp() {
        taskStateProvider = new MockTaskStateProvider();
        taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(taskStateProvider, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);
    }

    @AfterEach
    public void tearDown() {
        EventQueueUtil.stop(mainEventBusProcessor);
    }

    @Test
    public void testAddNewQueue() {
        String taskId = "test_task_id";
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, queue);

        EventQueue retrievedQueue = queueManager.get(taskId);
        assertSame(queue, retrievedQueue);
    }

    @Test
    public void testAddExistingQueueThrowsException() {
        String taskId = "test_task_id";
        EventQueue queue1 = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        EventQueue queue2 = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, queue1);

        assertThrows(TaskQueueExistsException.class, () -> {
            queueManager.add(taskId, queue2);
        });
    }

    @Test
    public void testGetExistingQueue() {
        String taskId = "test_task_id";
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, queue);
        EventQueue result = queueManager.get(taskId);

        assertSame(queue, result);
    }

    @Test
    public void testGetNonexistentQueue() {
        EventQueue result = queueManager.get("nonexistent_task_id");
        assertNull(result);
    }

    @Test
    public void testTapExistingQueue() {
        String taskId = "test_task_id";
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, queue);
        EventQueue tappedQueue = queueManager.tap(taskId);

        assertNotNull(tappedQueue);

        // Tapped queue should be different from original
        // (it's a child queue in Java implementation)
        assertNotSame(queue, tappedQueue, "Tapped queue should be a different instance from the original.");
    }

    @Test
    public void testTapNonexistentQueue() {
        EventQueue result = queueManager.tap("nonexistent_task_id");
        assertNull(result);
    }

    @Test
    public void testCloseExistingQueue() {
        String taskId = "test_task_id";
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, queue);
        queueManager.close(taskId);

        // Queue should be removed from manager
        EventQueue result = queueManager.get(taskId);
        assertNull(result);
    }

    @Test
    public void testCloseNonexistentQueueThrowsException() {
        assertThrows(NoTaskQueueException.class, () -> {
            queueManager.close("nonexistent_task_id");
        });
    }

    @Test
    public void testCreateOrTapNewQueue() {
        String taskId = "test_task_id";

        EventQueue result = queueManager.createOrTap(taskId);

        assertNotNull(result);
        // createOrTap now returns ChildQueue, not MainQueue
        // MainQueue should be stored in manager
        EventQueue retrievedQueue = queueManager.get(taskId);
        assertNotNull(retrievedQueue);
        // Result should be a ChildQueue (cannot be tapped)
        assertThrows(IllegalStateException.class, () -> result.tap());
    }

    @Test
    public void testCreateOrTapExistingQueue() {
        String taskId = "test_task_id";
        EventQueue originalQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();

        queueManager.add(taskId, originalQueue);
        EventQueue result = queueManager.createOrTap(taskId);

        assertNotNull(result);
        // Should be a tapped (child) queue, not the original
        // Original should still be in manager
        EventQueue retrievedQueue = queueManager.get(taskId);
        assertSame(originalQueue, retrievedQueue);
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException, ExecutionException {
        // Create 10 different task IDs
        List<String> taskIds = IntStream.range(0, 10)
                .mapToObj(i -> "task_" + i)
                .toList();

        // Add tasks concurrently
        List<CompletableFuture<String>> addFutures = taskIds.stream()
                .map(taskId -> CompletableFuture.supplyAsync(() -> {
                    EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
                    queueManager.add(taskId, queue);
                    return taskId;
                }))
                .toList();

        // Wait for all add operations to complete
        List<String> addedTaskIds = new ArrayList<>();
        for (CompletableFuture<String> future : addFutures) {
            addedTaskIds.add(future.get());
        }

        // Verify all tasks were added
        assertEquals(taskIds.size(), addedTaskIds.size());
        assertTrue(addedTaskIds.containsAll(taskIds));

        // Get tasks concurrently
        List<CompletableFuture<EventQueue>> getFutures = taskIds.stream()
                .map(taskId -> CompletableFuture.supplyAsync(() -> queueManager.get(taskId)))
                .toList();

        // Wait for all get operations to complete
        List<EventQueue> queues = new ArrayList<>();
        for (CompletableFuture<EventQueue> future : getFutures) {
            queues.add(future.get());
        }

        // Verify all queues are not null
        assertEquals(taskIds.size(), queues.size());
        for (EventQueue queue : queues) {
            assertNotNull(queue);
        }

        // Verify all tasks are in the manager
        for (String taskId : taskIds) {
            assertNotNull(queueManager.get(taskId));
        }
    }

    @Test
    public void testCreateOrTapRaceCondition() throws InterruptedException, ExecutionException {
        String taskId = "race_condition_task";

        // Try to create/tap the same task ID concurrently
        List<CompletableFuture<EventQueue>> futures = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> queueManager.createOrTap(taskId)))
                .toList();

        // Wait for all operations to complete
        List<EventQueue> results = new ArrayList<>();
        for (CompletableFuture<EventQueue> future : futures) {
            results.add(future.get());
        }

        // All results should be non-null
        for (EventQueue result : results) {
            assertNotNull(result);
        }

        // There should be exactly one MainQueue in the manager
        EventQueue managerQueue = queueManager.get(taskId);
        assertNotNull(managerQueue);

        // ALL results should be ChildQueues (cannot tap a ChildQueue)
        for (EventQueue result : results) {
            assertThrows(IllegalStateException.class, () -> result.tap());
        }

        // All ChildQueues should be distinct instances
        long distinctCount = results.stream().distinct().count();
        assertEquals(results.size(), distinctCount, "All ChildQueues should be distinct instances");
    }
}
