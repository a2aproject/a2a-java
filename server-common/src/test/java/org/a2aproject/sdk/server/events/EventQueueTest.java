package org.a2aproject.sdk.server.events;

import static org.a2aproject.sdk.jsonrpc.common.json.JsonUtil.fromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.MockTaskStateProvider;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueueTest {

    private EventQueue eventQueue;
    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;

    private static final String TASK_ID = "123";  // Must match MINIMAL_TASK id

    private static final String MINIMAL_TASK = """
            {
                "id": "123",
                "contextId": "session-xyz",
                "status": {"state": "TASK_STATE_SUBMITTED"}
            }
            """;

    private static final String MESSAGE_PAYLOAD = """
            {
                "role": "ROLE_AGENT",
                "parts": [{"text": "test message"}],
                "messageId": "111"
            }
            """;

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = (event, snapshot) -> {};

    @BeforeEach
    public void init() {
        // Set up MainEventBus and processor for production-like test environment
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        InMemoryQueueManager queueManager = new InMemoryQueueManager(taskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        eventQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(TASK_ID)
                .mainEventBus(mainEventBus)
                .build().tap();
    }

    @AfterEach
    public void cleanup() {
        if (mainEventBusProcessor != null) {
            mainEventBusProcessor.setCallback(null);  // Clear any test callbacks
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    /**
     * Helper to create a queue with MainEventBus configured (for tests that need event distribution).
     */
    private EventQueue createQueueWithEventBus(String taskId) {
        return createQueueWithEventBus(taskId, null);
    }

    private EventQueue createQueueWithEventBus(String taskId, @Nullable TaskStreamLifecycleHook hook) {
        return EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(taskId)
                .streamLifecycleHook(hook)
                .build();
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
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
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
    public void testConstructorDefaultQueueSize() {
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, queue.getQueueSize());
    }

    @Test
    public void testConstructorCustomQueueSize() {
        int customSize = 500;
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).queueSize(customSize).build();
        assertEquals(customSize, queue.getQueueSize());
    }

    @Test
    public void testConstructorInvalidQueueSize() {
        // Test zero queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueueUtil.getEventQueueBuilder(mainEventBus).queueSize(0).build());

        // Test negative queue size
        assertThrows(IllegalArgumentException.class, () -> EventQueueUtil.getEventQueueBuilder(mainEventBus).queueSize(-10).build());
    }

    @Test
    public void testTapCreatesChildQueue() {
        EventQueue parentQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        EventQueue childQueue = parentQueue.tap();

        assertNotNull(childQueue);
        assertNotSame(parentQueue, childQueue);
        assertEquals(EventQueue.DEFAULT_QUEUE_SIZE, childQueue.getQueueSize());
    }

    @Test
    public void testTapOnChildQueueThrowsException() {
        EventQueue parentQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        EventQueue childQueue = parentQueue.tap();

        assertThrows(IllegalStateException.class, () -> childQueue.tap());
    }

    @Test
    public void testEnqueueEventPropagagesToChildren() throws Exception {
        EventQueue mainQueue = createQueueWithEventBus(TASK_ID);
        EventQueue childQueue1 = mainQueue.tap();
        EventQueue childQueue2 = mainQueue.tap();

        Event event = fromJson(MINIMAL_TASK, Task.class);
        mainQueue.enqueueEvent(event);

        // Event should be available in all child queues
        // Note: MainEventBusProcessor runs async, so we use dequeueEventItem with timeout
        Event child1Event = childQueue1.dequeueEventItem(5000).getEvent();
        Event child2Event = childQueue2.dequeueEventItem(5000).getEvent();

        assertSame(event, child1Event);
        assertSame(event, child2Event);
    }

    @Test
    public void testMultipleChildQueuesReceiveEvents() throws Exception {
        EventQueue mainQueue = createQueueWithEventBus(TASK_ID);
        EventQueue childQueue1 = mainQueue.tap();
        EventQueue childQueue2 = mainQueue.tap();
        EventQueue childQueue3 = mainQueue.tap();

        Event event1 = fromJson(MINIMAL_TASK, Task.class);
        Event event2 = fromJson(MESSAGE_PAYLOAD, Message.class);

        mainQueue.enqueueEvent(event1);
        mainQueue.enqueueEvent(event2);

        // All child queues should receive both events
        // Note: Use timeout for async processing
        assertSame(event1, childQueue1.dequeueEventItem(5000).getEvent());
        assertSame(event2, childQueue1.dequeueEventItem(5000).getEvent());

        assertSame(event1, childQueue2.dequeueEventItem(5000).getEvent());
        assertSame(event2, childQueue2.dequeueEventItem(5000).getEvent());

        assertSame(event1, childQueue3.dequeueEventItem(5000).getEvent());
        assertSame(event2, childQueue3.dequeueEventItem(5000).getEvent());
    }

    @Test
    public void testChildQueueDequeueIndependently() throws Exception {
        EventQueue mainQueue = createQueueWithEventBus(TASK_ID);
        EventQueue childQueue1 = mainQueue.tap();
        EventQueue childQueue2 = mainQueue.tap();
        EventQueue childQueue3 = mainQueue.tap();

        Event event = fromJson(MINIMAL_TASK, Task.class);
        mainQueue.enqueueEvent(event);

        // Dequeue from child1 first (use timeout for async processing)
        Event child1Event = childQueue1.dequeueEventItem(5000).getEvent();
        assertSame(event, child1Event);

        // child2 should still have the event available
        Event child2Event = childQueue2.dequeueEventItem(5000).getEvent();
        assertSame(event, child2Event);

        // child3 should still have the event available
        Event child3Event = childQueue3.dequeueEventItem(5000).getEvent();
        assertSame(event, child3Event);
    }


    @Test
    public void testCloseImmediatePropagationToChildren() throws Exception {
        EventQueue parentQueue = createQueueWithEventBus(TASK_ID);
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
        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(TASK_ID)
                .build();
        EventQueue childQueue = mainQueue.tap();
        Event event = fromJson(MINIMAL_TASK, Task.class);

        childQueue.close(); // Close the child queue first (removes from children list)
        assertTrue(childQueue.isClosed());

        // Create a new child queue BEFORE enqueuing (ensures it's in children list for distribution)
        EventQueue newChildQueue = mainQueue.tap();

        // MainQueue accepts events even when closed (for replication support)
        // This ensures late-arriving replicated events can be enqueued to closed queues
        // Note: MainEventBusProcessor runs asynchronously, so we use dequeueEventItem with timeout
        mainQueue.enqueueEvent(event);

        // New child queue should receive the event (old closed child was removed from children list)
        EventQueueItem item = newChildQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event dequeuedEvent = item.getEvent();
        assertSame(event, dequeuedEvent);

        // Now new child queue is closed and empty, should throw exception
        newChildQueue.close();
        assertThrows(EventQueueClosedException.class, () -> newChildQueue.dequeueEventItem(-1));
    }

    @Test
    public void testDequeueEventWhenClosedAndEmpty() throws Exception {
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build().tap();
        queue.close();
        assertTrue(queue.isClosed());

        // Dequeue from closed empty queue should throw exception
        assertThrows(EventQueueClosedException.class, () -> queue.dequeueEventItem(-1));
    }

    @Test
    public void testDequeueEventWhenClosedButHasEvents() throws Exception {
        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(TASK_ID)
                .build();
        EventQueue childQueue = mainQueue.tap();
        Event event = fromJson(MINIMAL_TASK, Task.class);

        // Use callback to wait for event processing instead of polling
        waitForEventProcessing(() -> mainQueue.enqueueEvent(event));

        // At this point, event has been processed and distributed to childQueue
        childQueue.close(); // Graceful close - events should remain
        assertTrue(childQueue.isClosed());

        // Should still be able to dequeue existing events from closed queue
        EventQueueItem item = childQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event dequeuedEvent = item.getEvent();
        assertSame(event, dequeuedEvent);

        // Now queue is closed and empty, should throw exception
        assertThrows(EventQueueClosedException.class, () -> childQueue.dequeueEventItem(-1));
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
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event dequeuedEvent = item.getEvent();
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
                .taskId(TASK_ID)
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        eventQueue.enqueueEvent(event);
        Event dequeuedEvent = eventQueue.dequeueEventItem(1000).getEvent();
        assertSame(event, dequeuedEvent);
    }

    @Test
    public void testTaskDone() throws Exception {
        Event event = TaskArtifactUpdateEvent.builder()
                .taskId(TASK_ID)
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
        EventQueue eventQueue2 = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
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
        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        EventQueue childQueue = mainQueue.tap();
        assertTrue(childQueue != null);

        // Graceful close - parent closes but children remain open
        mainQueue.close();
        assertTrue(mainQueue.isClosed());
        assertFalse(childQueue.isClosed());  // Child NOT closed on graceful parent close

        // Immediate close - parent force-closes all children
        EventQueue mainQueue2 = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
        EventQueue childQueue2 = mainQueue2.tap();
        mainQueue2.close(true);  // immediate=true
        assertTrue(mainQueue2.isClosed());
        assertTrue(childQueue2.isClosed());  // Child IS closed on immediate parent close
    }

    /**
     * Test reference counting: MainQueue stays open while children are active,
     * closes automatically when last child closes.
     */
    @Test
    public void testMainQueueReferenceCountingStaysOpenWithActiveChildren() throws Exception {
        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus).build();
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

    /**
     * Helper to create a queue with a TaskStateProvider that keeps MainQueue open
     * when all children close (task is never finalized).
     */
    private EventQueue createNonFinalizedQueueWithEventBus(String taskId) {
        return EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(taskId)
                .taskStateProvider(new MockTaskStateProvider())
                .build();
    }

    @Test
    public void testCloseChildrenGracefullyClosesAllChildren() throws InterruptedException {
        EventQueue mainQueue = createNonFinalizedQueueWithEventBus("close-children-test");

        EventQueue child1 = mainQueue.tap();
        EventQueue child2 = mainQueue.tap();
        assertFalse(child1.isClosed());
        assertFalse(child2.isClosed());

        ((EventQueue.MainQueue) mainQueue).closeChildren();

        assertTrue(child1.isClosed());
        assertTrue(child2.isClosed());

        assertFalse(mainQueue.isClosed());
    }

    @Test
    public void testCloseChildrenAllowsNewSubscriptions() throws InterruptedException {
        EventQueue mainQueue = createNonFinalizedQueueWithEventBus("close-children-resubscribe-test");

        EventQueue child1 = mainQueue.tap();
        ((EventQueue.MainQueue) mainQueue).closeChildren();
        assertTrue(child1.isClosed());

        EventQueue child2 = mainQueue.tap();
        assertFalse(child2.isClosed());
        assertFalse(mainQueue.isClosed());
    }

    @Test
    public void testStreamCloseHandleCloseStreams() throws InterruptedException {
        EventQueue mainQueue = createNonFinalizedQueueWithEventBus("handle-close-test");

        EventQueue child1 = mainQueue.tap();
        EventQueue child2 = mainQueue.tap();

        StreamCloseHandle handle = ((EventQueue.MainQueue) mainQueue).getStreamCloseHandle();
        assertEquals(2, handle.getActiveSubscriberCount());

        handle.closeStreams();

        assertTrue(child1.isClosed());
        assertTrue(child2.isClosed());
        assertFalse(mainQueue.isClosed());
        assertEquals(0, handle.getActiveSubscriberCount());
    }

    @Test
    public void testStreamLifecycleHookOnEventCalledAfterDistribution() throws InterruptedException {
        List<Event> receivedEvents = new ArrayList<>();
        CountDownLatch hookLatch = new CountDownLatch(1);
        TaskStreamLifecycleHook hook = TestStreamLifecycleHook.onEvent((taskId, event, handle) -> {
            receivedEvents.add(event);
            hookLatch.countDown();
        });

        EventQueue mainQueue = createQueueWithEventBus("hook-event-test", hook);
        EventQueue child = mainQueue.tap();

        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("hook-event-test")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING, null, null))
                .build();

        mainQueue.enqueueEvent(statusEvent);

        assertTrue(hookLatch.await(5, TimeUnit.SECONDS),
                "TaskStreamLifecycleHook.onEvent should have been called");

        assertEquals(1, receivedEvents.size());
        assertSame(statusEvent, receivedEvents.get(0));
    }

    @Test
    public void testCloseStreamsFromWithinOnEventDoesNotCauseConcurrentModification() throws InterruptedException {
        CountDownLatch hookLatch = new CountDownLatch(1);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        TaskStreamLifecycleHook hook = TestStreamLifecycleHook.onEvent((taskId, event, handle) -> {
            try {
                handle.closeStreams();
            } catch (Throwable t) {
                caughtException.set(t);
            } finally {
                hookLatch.countDown();
            }
        });

        EventQueue mainQueue = createQueueWithEventBus("close-in-hook-test", hook);
        mainQueue.tap();
        mainQueue.tap();

        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("close-in-hook-test")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING, null, null))
                .build();

        mainQueue.enqueueEvent(statusEvent);

        assertTrue(hookLatch.await(5, TimeUnit.SECONDS),
                "onEvent should have been called");
        assertNull(caughtException.get(),
                "closeStreams() from within onEvent should not throw");
    }

    @Test
    public void testCloseStreamsFromOnUnsubscribeIsNoOp() {
        AtomicInteger unsubscribeCount = new AtomicInteger(0);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();
        TaskStreamLifecycleHook hook = TestStreamLifecycleHook.onUnsubscribe((taskId, handle) -> {
            unsubscribeCount.incrementAndGet();
            try {
                handle.closeStreams();
            } catch (Throwable t) {
                caughtException.set(t);
            }
        });

        EventQueue mainQueue = createQueueWithEventBus("reentrant-close-test", hook);
        mainQueue.tap();
        mainQueue.tap();
        mainQueue.tap();

        ((EventQueue.MainQueue) mainQueue).closeChildren();

        assertNull(caughtException.get(),
                "Reentrant closeStreams() from onUnsubscribe should not throw");
        assertEquals(3, unsubscribeCount.get(),
                "All three children should have triggered onUnsubscribe exactly once");
        assertEquals(0, ((EventQueue.MainQueue) mainQueue).getActiveChildCount(),
                "All children should be closed");
    }

    @Test
    public void testTapReturnsClosedQueueWhenHookCallsCloseStreams() {
        TaskStreamLifecycleHook hook = TestStreamLifecycleHook.onSubscribe((taskId, handle) -> handle.closeStreams());

        EventQueue mainQueue = createQueueWithEventBus("tap-close-test", hook);
        EventQueue child = mainQueue.tap();

        assertTrue(child.isClosed(),
                "ChildQueue returned by tap() should be closed when hook calls closeStreams() during onSubscribe");
        assertThrows(EventQueueClosedException.class,
                () -> child.dequeueEventItem(0),
                "Dequeuing from a closed ChildQueue should throw EventQueueClosedException");
    }

    @Test
    public void testOnEventNotCalledWithZeroSubscribers() throws InterruptedException {
        AtomicBoolean onEventCalled = new AtomicBoolean(false);
        TaskStreamLifecycleHook hook = TestStreamLifecycleHook.onEvent((taskId, event, handle) -> onEventCalled.set(true));

        EventQueue mainQueue = createQueueWithEventBus("no-subscribers-test", hook);

        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("no-subscribers-test")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING, null, null))
                .build();

        CountDownLatch processingLatch = new CountDownLatch(1);
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
            }
        });

        try {
            mainQueue.enqueueEvent(statusEvent);
            assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "Event should have been processed");
        } finally {
            mainEventBusProcessor.setCallback(null);
        }

        assertFalse(onEventCalled.get(),
                "onEvent should not be called when there are zero subscribers");
    }

    @Test
    public void testChildQueueIsBoundedByParentQueueSize() throws Exception {
        int customSize = 5;
        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .queueSize(customSize)
                .build();
        EventQueue childQueue = mainQueue.tap();

        Field queueField = EventQueue.ChildQueue.class.getDeclaredField("queue");
        queueField.setAccessible(true);
        BlockingQueue<?> childDeque = (BlockingQueue<?>) queueField.get(childQueue);

        // The child queue must be bounded by the parent's configured capacity:
        // an unbounded deque would let a slow subscriber grow memory without limit.
        assertEquals(customSize, childDeque.remainingCapacity());
    }

    @Test
    public void testSemaphorePermitReleasedWhenSubmitFails() {
        MainEventBus failingBus = mock(MainEventBus.class);
        doThrow(new RuntimeException("submit failed"))
                .when(failingBus).submit(anyString(), any(), any());

        EventQueue mainQueue = EventQueueUtil.getEventQueueBuilder(failingBus)
                .taskId(TASK_ID)
                .queueSize(2)
                .build();
        assertEquals(0, mainQueue.size(), "No permits should be in use before enqueue");

        assertThrows(RuntimeException.class,
                () -> mainQueue.enqueueEvent(fromJson(MINIMAL_TASK, Task.class)));

        // If the permit leaked, size() would be 1 (one permit held forever). It must be
        // released when submit() fails because MainEventBusProcessor never saw the event
        // and therefore never called releaseSemaphore().
        assertEquals(0, mainQueue.size(), "Semaphore permit must be released on submit failure");
    }
}
