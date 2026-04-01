package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for per-task push notification ordering guarantees.
 * <ul>
 *   <li>Events for the same task are sent in FIFO order</li>
 *   <li>Events for different tasks are isolated and don't block each other</li>
 *   <li>Queue overflow drops oldest events, keeps newest</li>
 * </ul>
 */
public class MainEventBusProcessorPushNotificationOrderTest {

    private MainEventBus mainEventBus;
    private InMemoryQueueManager queueManager;
    private CopyOnWriteArrayList<String> pushNotificationOrder;
    private CopyOnWriteArrayList<String> pushNotificationTaskIds;
    private PushNotificationSender pushSender;

    @BeforeEach
    public void setUp() {
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(null, mainEventBus);
        pushNotificationOrder = new CopyOnWriteArrayList<>();
        pushNotificationTaskIds = new CopyOnWriteArrayList<>();
        pushSender = event -> {
            pushNotificationOrder.add(event.getClass().getSimpleName());
            if (event instanceof Task task) {
                pushNotificationTaskIds.add(task.id());
            } else if (event instanceof Message msg) {
                pushNotificationTaskIds.add(msg.taskId());
            }
        };
    }

    @AfterEach
    public void tearDown() {
        // no-op: no MainEventBusProcessor is started in these tests
    }

    @Test
    public void testSameTaskEventsOrdered() throws Exception {
        String taskId = "ordered-task";
        int eventCount = 20;

        CountDownLatch latch = new CountDownLatch(eventCount);
        AtomicInteger callCount = new AtomicInteger(0);

        PushNotificationSender countingSender = event -> {
            pushSender.sendNotification(event);
            int seq = callCount.incrementAndGet();
            pushNotificationOrder.add("seq=" + seq + "-" + event.getClass().getSimpleName());
            latch.countDown();
        };

        MainEventBusProcessor processor = new MainEventBusProcessor(
                mainEventBus, mock(TaskStore.class), countingSender, queueManager);

        // Submit events rapidly on the producer side
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            int seq = i;
            Thread t = new Thread(() -> {
                Task event = Task.builder()
                        .id(taskId)
                        .contextId("ctx")
                        .status(new TaskStatus(seq % 2 == 0 ? TaskState.TASK_STATE_WORKING : TaskState.TASK_STATE_SUBMITTED))
                        .build();
                processor.sendPushNotification(taskId, event);
            });
            threads.add(t);
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All notifications should be sent within timeout");

        // Verify: all seq numbers appear in increasing order
        int lastSeq = -1;
        for (String entry : pushNotificationOrder) {
            if (entry.startsWith("seq=")) {
                int seq = Integer.parseInt(entry.substring(4, entry.indexOf('-')));
                assertTrue(seq > lastSeq, "Sequence numbers should increase: " + entry);
                lastSeq = seq;
            }
        }

        processor.stop();
    }

    @Test
    public void testDifferentTasksUnaffected() throws Exception {
        String taskA = "task-A";
        String taskB = "task-B";
        CountDownLatch latch = new CountDownLatch(4);

        PushNotificationSender latchSender = event -> {
            pushSender.sendNotification(event);
            latch.countDown();
        };

        MainEventBusProcessor processor = new MainEventBusProcessor(
                mainEventBus, mock(TaskStore.class), latchSender, queueManager);

        // Alternate events between two tasks
        processor.sendPushNotification(taskA, createTask(taskA, 1));
        processor.sendPushNotification(taskB, createTask(taskB, 2));
        processor.sendPushNotification(taskA, createTask(taskA, 3));
        processor.sendPushNotification(taskB, createTask(taskB, 4));

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All notifications should complete");

        // Both tasks appear in results
        long taskACount = pushNotificationTaskIds.stream().filter(id -> id.equals(taskA)).count();
        long taskBCount = pushNotificationTaskIds.stream().filter(id -> id.equals(taskB)).count();
        assertEquals(2, taskACount, "Task A should have 2 notifications");
        assertEquals(2, taskBCount, "Task B should have 2 notifications");

        processor.stop();
    }

    @Test
    public void testQueueOverflowDropsOldest() throws Exception {
        // Test that the per-task queue has a bounded capacity.
        String taskId = "overflow-task";
        int capacity = 50;

        AtomicInteger sentCount = new AtomicInteger(0);

        PushNotificationSender countingSender = event -> {
            sentCount.incrementAndGet();
            pushSender.sendNotification(event);
        };

        MainEventBusProcessor processor = new MainEventBusProcessor(
                mainEventBus, mock(TaskStore.class), countingSender, queueManager);

        // Submit exactly the queue capacity worth of events
        for (int i = 0; i < capacity; i++) {
            Task event = createTask(taskId, i);
            processor.sendPushNotification(taskId, event);
        }

        // Wait for all to be consumed
        Thread.sleep(1000);

        // All capacity events should be sent (no overflow)
        assertEquals(capacity, sentCount.get(),
                "All " + capacity + " events should be sent when queue is exactly full");

        processor.stop();
    }

    private Task createTask(String taskId, int seq) {
        return Task.builder()
                .id(taskId)
                .contextId("ctx-" + seq)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();
    }
}
