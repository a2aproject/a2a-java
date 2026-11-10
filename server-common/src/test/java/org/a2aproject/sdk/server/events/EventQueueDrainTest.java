package org.a2aproject.sdk.server.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EventQueue#awaitDrained(long, TimeUnit)}, which the blocking
 * {@code SendMessage} path uses to make sure every event the agent enqueued has been persisted
 * before the task is read back from the {@link org.a2aproject.sdk.server.tasks.TaskStore}.
 * <p>
 * An event counts as in flight from {@link EventQueue#enqueueItem} until
 * {@link MainEventBusProcessor} is done with it, which is exactly the lifetime of the queue's
 * semaphore permit. Releasing the permit by hand stands in for the processor finishing the event,
 * so these tests do not need a running processor.
 */
class EventQueueDrainTest {

    private static Event QUEUE_USAGE_MESSAGE = Message.builder()
            .messageId("111")
            .role(Message.Role.ROLE_AGENT)
            .parts(new TextPart("test message"))
            .build();

    private static EventQueue.MainQueue newMainQueue() {
        return (EventQueue.MainQueue) EventQueueUtil.getEventQueueBuilder(new MainEventBus()).build();
    }

    @Test
    void anIdleQueueIsAlreadyDrained() throws Exception {
        assertTrue(newMainQueue().awaitDrained(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void anEventThatWasAcceptedButNotProcessedIsNotDrained() throws Exception {
        EventQueue.MainQueue queue = newMainQueue();
        queue.enqueueEvent(QUEUE_USAGE_MESSAGE);

        assertFalse(queue.awaitDrained(50, TimeUnit.MILLISECONDS),
                "an accepted event that the processor has not finished must not count as drained");

        queue.releaseSemaphore(); // what MainEventBusProcessor does once the event is persisted
        assertTrue(queue.awaitDrained(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void awaitDrainedBlocksUntilTheInFlightEventIsProcessed() throws Exception {
        EventQueue.MainQueue queue = newMainQueue();
        queue.enqueueEvent(QUEUE_USAGE_MESSAGE);

        Thread processor = new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            queue.releaseSemaphore();
        }, "stub-main-event-bus-processor");
        processor.start();

        long startNanos = System.nanoTime();
        assertTrue(queue.awaitDrained(5, TimeUnit.SECONDS));
        long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertTrue(waitedMillis >= 100,
                "awaitDrained should have waited for the in-flight event, but returned after " + waitedMillis + "ms");
        processor.join(TimeUnit.SECONDS.toMillis(5));
    }

    @Test
    void aChildQueueReportsTheInFlightEventsOfItsParent() throws Exception {
        EventQueue.MainQueue parent = newMainQueue();
        EventQueue child = parent.tap();
        parent.enqueueEvent(QUEUE_USAGE_MESSAGE);

        assertFalse(child.awaitDrained(50, TimeUnit.MILLISECONDS),
                "events still inside the parent are in flight for every child as well");

        parent.releaseSemaphore();
        assertTrue(child.awaitDrained(200, TimeUnit.MILLISECONDS));
    }
}
