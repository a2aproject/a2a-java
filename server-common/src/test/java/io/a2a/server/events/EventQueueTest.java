package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueueTest {

    private EventQueue eventQueue;

    private static final String MINIMAL_TASK = """
            {
                "id": "123",
                "contextId": "session-xyz",
                "status": {"state": "submitted"},
                "kind": "task"
            }
            """;

    private static final String MESSAGE_PAYLOAD = """
            {
                "role": "agent",
                "parts": [{"kind": "text", "text": "test message"}],
                "messageId": "111",
                "kind": "message"
            }
            """;


    @BeforeEach
    public void init() {
        eventQueue = EventQueue.create();

    }

    @Test
    public void testConstructorDefaultQueueSize() {
        EventQueue queue = EventQueue.create();
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, queue.getQueueSize());
    }

    @Test
    public void testConstructorCustomQueueSize() {
        int customSize = 500;
        EventQueue queue = EventQueue.create(customSize);
        assertEquals(customSize, queue.getQueueSize());
    }

    @Test
    public void testConstructorInvalidQueueSize() {
        // Test zero queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueue.create(0));

        // Test negative queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueue.create(-10));
    }

    @Test
    public void testTapCreatesChildQueue() {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue = parentQueue.tap();

        assertNotNull(childQueue);
        assertNotSame(parentQueue, childQueue);
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, childQueue.getQueueSize());
    }

    @Test
    public void testTapOnChildQueueThrowsException() {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue = parentQueue.tap();

        assertThrows(IllegalStateException.class, () -> childQueue.tap());
    }

    @Test
    public void testEnqueueEventPropagagesToChildren() throws Exception {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue = parentQueue.tap();

        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        parentQueue.enqueueEvent(event);

        // Event should be available in both parent and child queues
        Event parentEvent = parentQueue.dequeueEvent(-1);
        Event childEvent = childQueue.dequeueEvent(-1);

        assertSame(event, parentEvent);
        assertSame(event, childEvent);
    }

    @Test
    public void testMultipleChildQueuesReceiveEvents() throws Exception {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue1 = parentQueue.tap();
        EventQueue childQueue2 = parentQueue.tap();

        Event event1 = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        Event event2 = Utils.unmarshalFrom(MESSAGE_PAYLOAD, Message.TYPE_REFERENCE);

        parentQueue.enqueueEvent(event1);
        parentQueue.enqueueEvent(event2);

        // All queues should receive both events
        assertSame(event1, parentQueue.dequeueEvent(-1));
        assertSame(event2, parentQueue.dequeueEvent(-1));

        assertSame(event1, childQueue1.dequeueEvent(-1));
        assertSame(event2, childQueue1.dequeueEvent(-1));

        assertSame(event1, childQueue2.dequeueEvent(-1));
        assertSame(event2, childQueue2.dequeueEvent(-1));
    }

    @Test
    public void testChildQueueDequeueIndependently() throws Exception {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue1 = parentQueue.tap();
        EventQueue childQueue2 = parentQueue.tap();

        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        parentQueue.enqueueEvent(event);

        // Dequeue from child1 first
        Event child1Event = childQueue1.dequeueEvent(-1);
        assertSame(event, child1Event);

        // child2 should still have the event available
        Event child2Event = childQueue2.dequeueEvent(-1);
        assertSame(event, child2Event);

        // Parent should still have the event available
        Event parentEvent = parentQueue.dequeueEvent(-1);
        assertSame(event, parentEvent);
    }


    @Test
    public void testCloseImmediatePropagationToChildren() throws Exception {
        EventQueue parentQueue = EventQueue.create();
        EventQueue childQueue = parentQueue.tap();

        // Add events to both parent and child
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        parentQueue.enqueueEvent(event);

        assertFalse(childQueue.isClosed());
        try {
            assertNotNull(childQueue.dequeueEvent(-1)); // Child has the event
        } catch (EventQueueClosedException e) {
            // This is fine if queue closed before dequeue
        }

        // Add event again for immediate close test
        parentQueue.enqueueEvent(event);

        // Close with immediate=true
        parentQueue.close(true);

        assertTrue(parentQueue.isClosed());
        assertTrue(childQueue.isClosed());

        // Child queue should be cleared due to immediate close
        try {
            Event result = childQueue.dequeueEvent(-1);
            assertNull(result);
        } catch (EventQueueClosedException e) {
            // This is expected when queue is closed
        }
    }

    @Test
    public void testEnqueueEventWhenClosed() throws Exception {
        EventQueue queue = EventQueue.create();
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);

        queue.close(); // Close the queue first
        assertTrue(queue.isClosed());

        // Attempt to enqueue should be ignored (no exception thrown)
        queue.enqueueEvent(event);

        // Verify the queue is still empty
        try {
            Event dequeuedEvent = queue.dequeueEvent(-1);
            assertNull(dequeuedEvent);
        } catch (EventQueueClosedException e) {
            // This is expected when queue is closed and empty
        }
    }

    @Test
    public void testDequeueEventWhenClosedAndEmpty() throws Exception {
        EventQueue queue = EventQueue.create();
        queue.close();
        assertTrue(queue.isClosed());

        // Dequeue from closed empty queue should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEvent(-1));
    }

    @Test
    public void testDequeueEventWhenClosedButHasEvents() throws Exception {
        EventQueue queue = EventQueue.create();
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        queue.enqueueEvent(event);

        queue.close(); // Graceful close - events should remain
        assertTrue(queue.isClosed());

        // Should still be able to dequeue existing events
        Event dequeuedEvent = queue.dequeueEvent(-1);
        assertSame(event, dequeuedEvent);

        // Now queue is closed and empty, should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEvent(-1));
    }

    @Test
    public void testEnqueueAndDequeueEvent() throws Exception {
        Event event = Utils.unmarshalFrom(MESSAGE_PAYLOAD, Message.TYPE_REFERENCE);
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEvent(200);
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testDequeueEventNoWait() throws Exception {
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEvent(-1);
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testDequeueEventEmptyQueueNoWait() throws Exception {
        Event dequeuedEvent = eventQueue.dequeueEvent(-1);
        assertNull(dequeuedEvent);
    }

    @Test
    public void testDequeueEventWait() throws Exception {
        Event event = new TaskStatusUpdateEvent.Builder()
                .taskId("task-123")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(true)
                .build();

        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEvent(1000);
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testTaskDone() throws Exception {
        Event event = new TaskArtifactUpdateEvent.Builder()
                .taskId("task-123")
                .contextId("session-xyz")
                .artifact(new Artifact.Builder()
                        .artifactId("11")
                        .parts(new TextPart("text"))
                        .build())
                .build();
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEvent(1000);
        assertSame(event, dequeuedEvent);
        eventQueue.taskDone();
    }

    @Test
    public void testEnqueueDifferentEventTypes() throws Exception {
        List<Event> events = List.of(
                new TaskNotFoundError(),
                new JSONRPCError(111, "rpc error", null));

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
            Event dequeuedEvent = eventQueue.dequeueEvent(100);
            assertSame(event, dequeuedEvent);
        }
    }

    /**
     * Test close behavior sets flag and handles graceful close.
     * Backported from Python test: test_close_sets_flag_and_handles_internal_queue_old_python
     */
    @Test
    public void testCloseGracefulSetsFlag() throws Exception {
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        eventQueue.enqueueEvent(event);

        eventQueue.close(false); // Graceful close
        assertTrue(eventQueue.isClosed());
    }

    /**
     * Test immediate close behavior.
     * Backported from Python test behavior
     */
    @Test
    public void testCloseImmediateClearsQueue() throws Exception {
        Event event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        eventQueue.enqueueEvent(event);

        eventQueue.close(true); // Immediate close
        assertTrue(eventQueue.isClosed());

        // After immediate close, queue should be cleared
        // Attempting to dequeue should return null or throw exception
        try {
            Event dequeuedEvent = eventQueue.dequeueEvent(-1);
            // If we get here, the event should be null (queue was cleared)
            assertNull(dequeuedEvent);
        } catch (EventQueueClosedException e) {
            // This is also acceptable - queue is closed
        }
    }

    /**
     * Test that close is idempotent.
     * Backported from Python test: test_close_idempotent
     */
    @Test
    public void testCloseIdempotent() throws Exception {
        eventQueue.close();
        assertTrue(eventQueue.isClosed());

        // Calling close again should not cause issues
        eventQueue.close();
        assertTrue(eventQueue.isClosed());

        // Test with immediate close as well
        EventQueue eventQueue2 = EventQueue.create();
        eventQueue2.close(true);
        assertTrue(eventQueue2.isClosed());

        eventQueue2.close(true);
        assertTrue(eventQueue2.isClosed());
    }

    /**
     * Test that child queues are closed when parent closes.
     */
    @Test
    public void testCloseChildQueues() throws Exception {
        EventQueue childQueue = eventQueue.tap();
        assertTrue(childQueue != null);

        eventQueue.close();
        assertTrue(eventQueue.isClosed());
        assertTrue(childQueue.isClosed());
    }
}
