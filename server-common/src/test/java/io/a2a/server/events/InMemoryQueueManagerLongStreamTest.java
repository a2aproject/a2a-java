package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Queue-only regression tests; no agent, transport, or network call is started. */
@Timeout(10)
class InMemoryQueueManagerLongStreamTest {

    private final AtomicBoolean finalized = new AtomicBoolean();
    private final TaskStateProvider provider = new TaskStateProvider() {
        @Override
        public boolean isTaskActive(String taskId) {
            return !finalized.get();
        }

        @Override
        public boolean isTaskFinalized(String taskId) {
            return finalized.get();
        }
    };

    @Test
    void childConsumersCanReadBeyondMainQueueCapacity() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            InMemoryQueueManager manager = new InMemoryQueueManager(provider);
            EventQueue first = manager.createOrTap("long-stream");
            EventQueue second = manager.tap("long-stream");
            try {
                for (int i = 0; i < 10000; i++) {
                    TaskStatusUpdateEvent event = event("long-stream", i);
                    first.enqueueEvent(event);
                    assertSame(event, first.dequeueEventItem(0).getEvent());
                    assertSame(event, second.dequeueEventItem(0).getEvent());
                }
                EventQueue main = manager.get("long-stream");
                assertTrue(main.queue.isEmpty(), "The dispatch-only queue must not retain copies");
                assertEquals(main.getQueueSize(), main.semaphore.availablePermits());
            } finally {
                manager.close("long-stream");
            }
        });
    }

    @Test
    void enqueueDoesNotSignalPollerStart() throws Exception {
        InMemoryQueueManager manager = new InMemoryQueueManager(provider);
        EventQueue child = manager.createOrTap("startup");
        CountDownLatch waiting = new CountDownLatch(1);
        CompletableFuture<Void> started = new CompletableFuture<>();
        Thread waiter = new Thread(() -> {
            waiting.countDown();
            try {
                child.awaitQueuePollerStart();
                started.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.setDaemon(true);
        waiter.start();
        try {
            assertTrue(waiting.await(1, TimeUnit.SECONDS));
            child.enqueueEvent(event("startup", 1));
            assertThrows(TimeoutException.class, () -> started.get(200, TimeUnit.MILLISECONDS));
            assertNotNull(child.dequeueEventItem(0));
            started.get(1, TimeUnit.SECONDS);
        } finally {
            waiter.interrupt();
            waiter.join(1000);
            manager.close("startup");
        }
    }

    @Test
    void slowSubscriberStillAppliesBackpressure() throws Exception {
        InMemoryQueueManager manager = new InMemoryQueueManager(provider);
        EventQueue fast = manager.createOrTap("slow");
        EventQueue slow = manager.tap("slow");
        for (int i = 0; i < EventQueue.DEFAULT_QUEUE_SIZE; i++) {
            fast.enqueueEvent(event("slow", i));
            assertNotNull(fast.dequeueEventItem(0));
        }
        CompletableFuture<Void> sent = new CompletableFuture<>();
        CountDownLatch attempting = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            attempting.countDown();
            try {
                fast.enqueueEvent(event("slow", EventQueue.DEFAULT_QUEUE_SIZE));
                sent.complete(null);
            } catch (Throwable e) {
                sent.completeExceptionally(e);
            }
        });
        producer.setDaemon(true);
        producer.start();
        try {
            assertTrue(attempting.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> sent.get(200, TimeUnit.MILLISECONDS));
            assertNotNull(slow.dequeueEventItem(0));
            sent.get(1, TimeUnit.SECONDS);
            assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, slow.queue.size());
        } finally {
            producer.interrupt();
            producer.join(1000);
            manager.close("slow");
        }
    }

    @Test
    void activeTaskCanResubscribeAndFinalizedTaskCleansUp() throws Exception {
        InMemoryQueueManager manager = new InMemoryQueueManager(provider);
        EventQueue first = manager.createOrTap("resume");
        EventQueue main = manager.get("resume");
        first.close();
        assertFalse(main.isClosed());
        assertSame(main, manager.get("resume"));
        // There is no historical-event replay on tap. Events with no subscribers must not
        // accumulate unreachable copies and block subsequent live delivery.
        for (int i = 0; i < 10000; i++) {
            main.enqueueEvent(event("resume", i));
        }
        EventQueue resumed = manager.createOrTap("resume");
        TaskStatusUpdateEvent live = event("resume", 10000);
        resumed.enqueueEvent(live);
        assertSame(live, resumed.dequeueEventItem(0).getEvent());
        assertNull(resumed.dequeueEventItem(0));
        finalized.set(true);
        resumed.close();
        assertTrue(main.isClosed());
        assertNull(manager.get("resume"));
    }

    @Test
    void directAndCustomFactoryQueuesStillRetainMainEvents() throws Exception {
        EventQueue direct = EventQueue.builder().build();
        TaskStatusUpdateEvent event = event("direct", 0);
        direct.enqueueEvent(event);
        assertSame(event, direct.dequeueEventItem(0).getEvent());
        direct.close();

        InMemoryQueueManager manager = new InMemoryQueueManager(
                taskId -> EventQueue.builder().queueSize(4), provider);
        EventQueue child = manager.createOrTap("custom");
        child.enqueueEvent(event);
        assertSame(event, child.dequeueEventItem(0).getEvent());
        assertSame(event, manager.get("custom").dequeueEventItem(0).getEvent());
        manager.close("custom");
    }

    private static TaskStatusUpdateEvent event(String taskId, int sequence) {
        return new TaskStatusUpdateEvent(taskId, new TaskStatus(TaskState.WORKING),
                "synthetic-context", false, Map.of("sequence", sequence));
    }
}
