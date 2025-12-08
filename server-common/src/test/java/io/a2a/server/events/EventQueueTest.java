package io.a2a.server.events;

import static io.a2a.jsonrpc.common.json.JsonUtil.fromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.A2AError;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueueTest {

    private EventQueue eventQueue;
    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;

    private static final String MINIMAL_TASK = """
            {
                "id": "123",
                "contextId": "session-xyz",
                "status": {"state": "submitted"}
            }
            """;

    private static final String MESSAGE_PAYLOAD = """
            {
                "role": "agent",
                "parts": [{"text": "test message"}],
                "messageId": "111"
            }
            """;

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    @BeforeEach
    public void init() {
        // Set up MainEventBus and processor for production-like test environment
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER);
        EventQueueUtil.start(mainEventBusProcessor);

        eventQueue = EventQueue.builder()
                .taskId("test-task")
                .mainEventBus(mainEventBus)
                .build();
    }

    @AfterEach
    public void cleanup() {
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    /**
     * Helper to create a queue with MainEventBus configured (for tests that need event distribution).
     */
    private EventQueue createQueueWithEventBus(String taskId) {
        return EventQueue.builder()
                .taskId(taskId)
                .mainEventBus(mainEventBus)
                .build();
    }

    @Test
    public void testConstructorDefaultQueueSize() {
        EventQueue queue = EventQueue.builder().build();
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, queue.getQueueSize());
    }

    @Test
    public void testConstructorCustomQueueSize() {
        int customSize = 500;
        EventQueue queue = EventQueue.builder().queueSize(customSize).build();
        assertEquals(customSize, queue.getQueueSize());
    }

    @Test
    public void testConstructorInvalidQueueSize() {
        // Test zero queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueue.builder().queueSize(0).build());

        // Test negative queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueue.builder().queueSize(-10).build());
    }

    @Test
    public void testTapCreatesChildQueue() {
        EventQueue parentQueue = EventQueue.builder().build();
        EventQueue childQueue = parentQueue.tap();

        assertNotNull(childQueue);
        assertNotSame(parentQueue, childQueue);
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, childQueue.getQueueSize());
    }

    @Test
    public void testTapOnChildQueueThrowsException() {
        EventQueue parentQueue = EventQueue.builder().build();
        EventQueue childQueue = parentQueue.tap();

        assertThrows(IllegalStateException.class, () -> childQueue.tap());
    }

    @Test
    public void testEnqueueEventPropagagesToChildren() throws Exception {
        EventQueue parentQueue = createQueueWithEventBus("test-propagate");
        EventQueue childQueue = parentQueue.tap();

        Event event = fromJson(MINIMAL_TASK, Task.class);
        parentQueue.enqueueEvent(event);

        // Event should be available in both parent and child queues
        // Note: MainEventBusProcessor runs async, so we use dequeueEventItem with timeout
        Event parentEvent = parentQueue.dequeueEventItem(5000).getEvent();
        Event childEvent = childQueue.dequeueEventItem(5000).getEvent();

        assertSame(event, parentEvent);
        assertSame(event, childEvent);
    }

    @Test
    public void testMultipleChildQueuesReceiveEvents() throws Exception {
        EventQueue parentQueue = createQueueWithEventBus("test-multiple");
        EventQueue childQueue1 = parentQueue.tap();
        EventQueue childQueue2 = parentQueue.tap();

        Event event1 = fromJson(MINIMAL_TASK, Task.class);
        Event event2 = fromJson(MESSAGE_PAYLOAD, Message.class);

        parentQueue.enqueueEvent(event1);
        parentQueue.enqueueEvent(event2);

        // All queues should receive both events
        // Note: Use timeout for async processing
        assertSame(event1, parentQueue.dequeueEventItem(5000).getEvent());
        assertSame(event2, parentQueue.dequeueEventItem(5000).getEvent());

        assertSame(event1, childQueue1.dequeueEventItem(5000).getEvent());
        assertSame(event2, childQueue1.dequeueEventItem(5000).getEvent());

        assertSame(event1, childQueue2.dequeueEventItem(5000).getEvent());
        assertSame(event2, childQueue2.dequeueEventItem(5000).getEvent());
    }

    @Test
    public void testChildQueueDequeueIndependently() throws Exception {
        EventQueue parentQueue = createQueueWithEventBus("test-independent");
        EventQueue childQueue1 = parentQueue.tap();
        EventQueue childQueue2 = parentQueue.tap();

        Event event = fromJson(MINIMAL_TASK, Task.class);
        parentQueue.enqueueEvent(event);

        // Dequeue from child1 first (use timeout for async processing)
        Event child1Event = childQueue1.dequeueEventItem(5000).getEvent();
        assertSame(event, child1Event);

        // child2 should still have the event available
        Event child2Event = childQueue2.dequeueEventItem(5000).getEvent();
        assertSame(event, child2Event);

        // Parent should still have the event available
        Event parentEvent = parentQueue.dequeueEventItem(5000).getEvent();
        assertSame(event, parentEvent);
    }


    @Test
    public void testCloseImmediatePropagationToChildren() throws Exception {
        EventQueue parentQueue = createQueueWithEventBus("test-close");
        EventQueue childQueue = parentQueue.tap();

        // Add events to both parent and child
        Event event = fromJson(MINIMAL_TASK, Task.class);
        parentQueue.enqueueEvent(event);

        assertFalse(childQueue.isClosed());
        try {
            assertNotNull(childQueue.dequeueEventItem(5000)); // Child has the event (use timeout)
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
        // Child queue should be cleared and closed, so dequeueing should throw
        assertThrows(EventQueueClosedException.class, () -> childQueue.dequeueEventItem(-1));
    }

    @Test
    public void testEnqueueEventWhenClosed() throws Exception {
        EventQueue queue = EventQueue.builder().build();
        Event event = fromJson(MINIMAL_TASK, Task.class);

        queue.close(); // Close the queue first
        assertTrue(queue.isClosed());

        // MainQueue accepts events even when closed (for replication support)
        // This ensures late-arriving replicated events can be enqueued to closed queues
        queue.enqueueEvent(event);

        // Event should be available for dequeuing
        Event dequeuedEvent = queue.dequeueEventItem(-1).getEvent();
        assertSame(event, dequeuedEvent);

        // Now queue is closed and empty, should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEventItem(-1));
    }

    @Test
    public void testDequeueEventWhenClosedAndEmpty() throws Exception {
        EventQueue queue = EventQueue.builder().build();
        queue.close();
        assertTrue(queue.isClosed());

        // Dequeue from closed empty queue should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEventItem(-1));
    }

    @Test
    public void testDequeueEventWhenClosedButHasEvents() throws Exception {
        EventQueue queue = EventQueue.builder().build();
        Event event = fromJson(MINIMAL_TASK, Task.class);
        queue.enqueueEvent(event);

        queue.close(); // Graceful close - events should remain
        assertTrue(queue.isClosed());

        // Should still be able to dequeue existing events
        Event dequeuedEvent = queue.dequeueEventItem(-1).getEvent();
        assertSame(event, dequeuedEvent);

        // Now queue is closed and empty, should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEventItem(-1));
    }

    @Test
    public void testEnqueueAndDequeueEvent() throws Exception {
        Event event = fromJson(MESSAGE_PAYLOAD, Message.class);
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEventItem(200).getEvent();
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testDequeueEventNoWait() throws Exception {
        Event event = fromJson(MINIMAL_TASK, Task.class);
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEventItem(-1).getEvent();
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testDequeueEventEmptyQueueNoWait() throws Exception {
        EventQueueItem item = eventQueue.dequeueEventItem(-1);
        assertNull(item);
    }

    @Test
    public void testDequeueEventWait() throws Exception {
        Event event = TaskStatusUpdateEvent.builder()
                .taskId("task-123")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(true)
                .build();

        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEventItem(1000).getEvent();
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testTaskDone() throws Exception {
        Event event = TaskArtifactUpdateEvent.builder()
                .taskId("task-123")
                .contextId("session-xyz")
                .artifact(Artifact.builder()
                        .artifactId("11")
                        .parts(new TextPart("text"))
                        .build())
                .build();
        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEventItem(1000).getEvent();
        assertSame(event, dequeuedEvent);
        eventQueue.taskDone();
    }

    @Test
    public void testEnqueueDifferentEventTypes() throws Exception {
        List<Event> events = List.of(
                new TaskNotFoundError(),
                new A2AError(111, "rpc error", null));

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
            Event dequeuedEvent = eventQueue.dequeueEventItem(100).getEvent();
            assertSame(event, dequeuedEvent);
        }
    }

    /**
     * Test close behavior sets flag and handles graceful close.
     * Backported from Python test: test_close_sets_flag_and_handles_internal_queue_old_python
     */
    @Test
    public void testCloseGracefulSetsFlag() throws Exception {
        Event event = fromJson(MINIMAL_TASK, Task.class);
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
        Event event = fromJson(MINIMAL_TASK, Task.class);
        eventQueue.enqueueEvent(event);

        eventQueue.close(true); // Immediate close
        assertTrue(eventQueue.isClosed());

        // After immediate close, queue should be cleared
        // Attempting to dequeue should return null or throw exception
        try {
            EventQueueItem item = eventQueue.dequeueEventItem(-1);
            // If we get here, the item should be null (queue was cleared)
            assertNull(item);
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
        EventQueue eventQueue2 = EventQueue.builder().build();
        eventQueue2.close(true);
        assertTrue(eventQueue2.isClosed());

        eventQueue2.close(true);
        assertTrue(eventQueue2.isClosed());
    }

    /**
     * Test that child queues are NOT automatically closed when parent closes gracefully.
     * Children must close themselves, which then notifies parent via reference counting.
     */
    @Test
    public void testCloseChildQueues() throws Exception {
        EventQueue childQueue = eventQueue.tap();
        assertTrue(childQueue != null);

        // Graceful close - parent closes but children remain open
        eventQueue.close();
        assertTrue(eventQueue.isClosed());
        assertFalse(childQueue.isClosed());  // Child NOT closed on graceful parent close

        // Immediate close - parent force-closes all children
        EventQueue parentQueue2 = EventQueue.builder().build();
        EventQueue childQueue2 = parentQueue2.tap();
        parentQueue2.close(true);  // immediate=true
        assertTrue(parentQueue2.isClosed());
        assertTrue(childQueue2.isClosed());  // Child IS closed on immediate parent close
    }

    /**
     * Test reference counting: MainQueue stays open while children are active,
     * closes automatically when last child closes.
     */
    @Test
    public void testMainQueueReferenceCountingStaysOpenWithActiveChildren() throws Exception {
        EventQueue mainQueue = EventQueue.builder().build();
        EventQueue child1 = mainQueue.tap();
        EventQueue child2 = mainQueue.tap();

        // Close child1
        child1.close();

        // MainQueue should still be open (child2 active)
        assertFalse(mainQueue.isClosed());
        assertTrue(child1.isClosed());
        assertFalse(child2.isClosed());

        // Close child2
        child2.close();

        // Now MainQueue should auto-close (no children left)
        assertTrue(mainQueue.isClosed());
        assertTrue(child2.isClosed());
    }
}
