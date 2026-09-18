package org.a2aproject.sdk.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for https://github.com/a2aproject/a2a-java/issues/775: push notifications
 * for the same task used to be submitted independently to the shared {@code ForkJoinPool},
 * so the client could observe them out of order relative to how the agent produced them.
 */
public class MainEventBusProcessorPushNotificationOrderTest {

    private static final String CONTEXT_ID = "test-context";

    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;
    private InMemoryTaskStore taskStore;
    private InMemoryQueueManager queueManager;

    @BeforeEach
    public void setUp() {
        taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(null, mainEventBus);
    }

    @AfterEach
    public void tearDown() {
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    @Test
    public void testEventsForSameTaskDeliveredInOrder() throws InterruptedException {
        String taskId = "order-task";
        int eventCount = 20;
        List<Integer> deliveryOrder = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(eventCount);

        // The Nth push sleeps longer the SMALLER N is, so an unordered executor would very
        // likely deliver a later, faster push before an earlier, slower one -- exactly the
        // interleaving #775 reported. A correctly-ordered chain delivers 0, 1, 2, ... regardless.
        PushNotificationSender sender = (event, snapshot) -> {
            TaskStatusUpdateEvent statusEvent = (TaskStatusUpdateEvent) event;
            int index = extractSequence(statusEvent);
            try {
                Thread.sleep(Math.max(0, (eventCount - index) * 2L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            deliveryOrder.add(index);
            latch.countDown();
        };

        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, sender, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);
        EventQueue eventQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(taskId)
                .mainEventBus(mainEventBus)
                .build().tap();

        eventQueue.enqueueEvent(Task.builder()
                .id(taskId)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build());

        for (int i = 0; i < eventCount; i++) {
            eventQueue.enqueueEvent(statusEventWithSequence(taskId, i));
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All push notifications should complete within timeout");
        assertEquals(eventCount, deliveryOrder.size());
        for (int i = 0; i < eventCount; i++) {
            assertEquals(i, deliveryOrder.get(i),
                    "Push notification " + i + " should be delivered in produced order, got " + deliveryOrder);
        }
    }

    @Test
    public void testDifferentTasksAreIndependent() throws InterruptedException {
        String slowTaskId = "slow-task";
        String fastTaskId = "fast-task";
        CountDownLatch slowLatch = new CountDownLatch(1);
        CountDownLatch fastLatch = new CountDownLatch(1);
        AtomicInteger fastDeliveredWhileSlowBlocked = new AtomicInteger(-1);

        PushNotificationSender sender = (event, snapshot) -> {
            TaskStatusUpdateEvent statusEvent = (TaskStatusUpdateEvent) event;
            if (statusEvent.taskId().equals(slowTaskId)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                slowLatch.countDown();
            } else {
                fastDeliveredWhileSlowBlocked.set(slowLatch.getCount() > 0 ? 1 : 0);
                fastLatch.countDown();
            }
        };

        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, sender, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        EventQueue slowQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(slowTaskId).mainEventBus(mainEventBus).build().tap();
        EventQueue fastQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(fastTaskId).mainEventBus(mainEventBus).build().tap();

        slowQueue.enqueueEvent(Task.builder().id(slowTaskId).contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED)).build());
        slowQueue.enqueueEvent(statusEventWithSequence(slowTaskId, 0));

        fastQueue.enqueueEvent(Task.builder().id(fastTaskId).contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED)).build());
        fastQueue.enqueueEvent(statusEventWithSequence(fastTaskId, 0));

        assertTrue(fastLatch.await(1, TimeUnit.SECONDS),
                "The fast task's push must not wait for the slow task's push");
        assertEquals(1, fastDeliveredWhileSlowBlocked.get(),
                "The fast task should have been delivered while the slow task was still blocked");

        assertTrue(slowLatch.await(5, TimeUnit.SECONDS), "The slow task's push should eventually complete");
    }

    @Test
    public void testSynchronousExecutorDoesNotLeakChainEntries() throws InterruptedException {
        // One entry per DISTINCT task leaks, not repeated pushes for the same one --
        // a single task's own map slot just gets overwritten each time. Only several
        // different task IDs reveal growth.
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        PushNotificationSender sender = (event, snapshot) -> latch.countDown();

        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, sender, queueManager);
        mainEventBusProcessor.setPushNotificationExecutor(Runnable::run);
        EventQueueUtil.start(mainEventBusProcessor);

        for (int i = 0; i < taskCount; i++) {
            String taskId = "sync-task-" + i;
            EventQueue eventQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                    .taskId(taskId)
                    .mainEventBus(mainEventBus)
                    .build().tap();
            eventQueue.enqueueEvent(Task.builder()
                    .id(taskId)
                    .contextId(CONTEXT_ID)
                    .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                    .build());
            eventQueue.enqueueEvent(statusEventWithSequence(taskId, 0));
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All push notifications should complete");

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (mainEventBusProcessor.pushNotificationChainCount() != 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(0, mainEventBusProcessor.pushNotificationChainCount(),
                "A completed task's chain entry must not be left in the map");
    }

    private TaskStatusUpdateEvent statusEventWithSequence(String taskId, int sequence) {
        return new TaskStatusUpdateEvent(taskId, new TaskStatus(TaskState.TASK_STATE_WORKING, null, null),
                CONTEXT_ID, java.util.Map.of("sequence", sequence));
    }

    private int extractSequence(TaskStatusUpdateEvent event) {
        return ((Number) event.metadata().get("sequence")).intValue();
    }
}
