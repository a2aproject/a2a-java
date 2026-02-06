package io.a2a.server.events;

import static io.a2a.jsonrpc.common.json.JsonUtil.fromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.spec.A2AError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventConsumerTest {

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};
    private static final String TASK_ID = "123";  // Must match MINIMAL_TASK id

    private EventQueue eventQueue;
    private EventConsumer eventConsumer;
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
        eventConsumer = new EventConsumer(eventQueue);
    }

    @AfterEach
    public void cleanup() {
        if (mainEventBusProcessor != null) {
            mainEventBusProcessor.setCallback(null);  // Clear any test callbacks
            EventQueueUtil.stop(mainEventBusProcessor);
        }
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
    public void testConsumeOneTaskEvent() throws Exception {
        Task event = fromJson(MINIMAL_TASK, Task.class);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneMessageEvent() throws Exception {
        Event event = fromJson(MESSAGE_PAYLOAD, Message.class);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneA2AErrorEvent() throws Exception {
        Event event = new A2AError(-1, "", null);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneJsonRpcErrorEvent() throws Exception {
        Event event = new A2AError(123, "Some Error", null);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneQueueEmpty() throws A2AServerException {
        assertThrows(A2AServerException.class, () -> eventConsumer.consumeOne());
    }

    @Test
    public void testConsumeAllMultipleEvents() throws JsonProcessingException {
        List<Event> events = List.of(
                fromJson(MINIMAL_TASK, Task.class),
                TaskArtifactUpdateEvent.builder()
                        .taskId(TASK_ID)
                        .contextId("session-xyz")
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(TASK_ID)
                        .contextId("session-xyz")
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<EventQueueItem> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(getSubscriber(receivedEvents, error));

        assertNull(error.get());
        assertEquals(events.size(), receivedEvents.size());
        for (int i = 0; i < events.size(); i++) {
            assertSame(events.get(i), receivedEvents.get(i));
        }
    }

    @Test
    public void testConsumeUntilMessage() throws Exception {
        List<Event> events = List.of(
                fromJson(MINIMAL_TASK, Task.class),
                TaskArtifactUpdateEvent.builder()
                        .taskId(TASK_ID)
                        .contextId("session-xyz")
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(TASK_ID)
                        .contextId("session-xyz")
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<EventQueueItem> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(getSubscriber(receivedEvents, error));

        assertNull(error.get());
        assertEquals(3, receivedEvents.size());
        for (int i = 0; i < 3; i++) {
            assertSame(events.get(i), receivedEvents.get(i));
        }
    }

    @Test
    public void testConsumeMessageEvents() throws Exception {
        Message message = fromJson(MESSAGE_PAYLOAD, Message.class);
        Message message2 = Message.builder(message).build();

        List<Event> events = List.of(message, message2);

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<EventQueueItem> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(getSubscriber(receivedEvents, error));

        assertNull(error.get());
        // The stream is closed after the first Message
        assertEquals(1, receivedEvents.size());
        assertSame(message, receivedEvents.get(0));
    }

    @Test
    public void testConsumeTaskInputRequired() {
        Task task = Task.builder()
            .id(TASK_ID)
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.INPUT_REQUIRED))
            .build();
        List<Event> events = List.of(
            task,
            TaskArtifactUpdateEvent.builder()
                .taskId(TASK_ID)
                .contextId("session-xyz")
                .artifact(Artifact.builder()
                    .artifactId("11")
                    .parts(new TextPart("text"))
                    .build())
                .build(),
            TaskStatusUpdateEvent.builder()
                .taskId(TASK_ID)
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build());
        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<EventQueueItem> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(getSubscriber(receivedEvents, error));

        assertNull(error.get());
        // The stream is closed after the input_required task
        assertEquals(1, receivedEvents.size());
        assertSame(task, receivedEvents.get(0));
    }

    private Flow.Subscriber<EventQueueItem> getSubscriber(List<Event> receivedEvents, AtomicReference<Throwable> error) {
        return new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(EventQueueItem item) {
                receivedEvents.add(item.getEvent());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        };
    }

    @Test
    public void testCreateAgentRunnableDoneCallbackSetsError() {
        EnhancedRunnable mockRunnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Mock implementation
            }
        };

        Throwable testError = new RuntimeException("Test error");
        mockRunnable.setError(testError);

        EnhancedRunnable.DoneCallback callback = eventConsumer.createAgentRunnableDoneCallback();
        callback.done(mockRunnable);

        // The error should be stored in the event consumer
        assertEquals(testError, getEventConsumerError());
    }

    @Test
    public void testCreateAgentRunnableDoneCallbackNoError() {
        EnhancedRunnable mockRunnable = new EnhancedRunnable() {
            @Override
            public void run() {
                // Mock implementation
            }
        };

        // No error set on runnable
        assertNull(mockRunnable.getError());

        EnhancedRunnable.DoneCallback callback = eventConsumer.createAgentRunnableDoneCallback();
        callback.done(mockRunnable);

        // The error should remain null
        assertNull(getEventConsumerError());
    }

    @Test
    public void testConsumeAllRaisesStoredException() throws InterruptedException {
        // Set an error in the event consumer
        setEventConsumerError(new RuntimeException("Stored error"));

        Flow.Publisher<EventQueueItem> publisher = eventConsumer.consumeAll();
        final AtomicReference<Throwable> receivedError = new AtomicReference<>();

        final CountDownLatch errorLatch = new CountDownLatch(1);


        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(EventQueueItem item) {
                // Should not be called
                errorLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                receivedError.set(throwable);
                errorLatch.countDown();
            }

            @Override
            public void onComplete() {
                // Should not be called
                errorLatch.countDown();
            }
        });

        // Wait for error callback with timeout
        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Test timed out waiting for onError callback.");

        assertNotNull(receivedError.get());
        assertEquals("Stored error", receivedError.get().getMessage());
    }

    @Test
    public void testConsumeAllStopsOnQueueClosed() throws Exception {
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .mainEventBus(mainEventBus)
                .build().tap();
        EventConsumer consumer = new EventConsumer(queue);

        // Close the queue immediately
        queue.close();

        Flow.Publisher<EventQueueItem> publisher = consumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(EventQueueItem item) {
                receivedEvents.add(item.getEvent());
            }

            @Override
            public void onError(Throwable throwable) {
                // Should not be called
                completionLatch.countDown();
            }

            @Override
            public void onComplete() {
                completed.set(true);
                completionLatch.countDown();
            }
        });

        // Wait for completion with timeout
        assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "Test timed out waiting for onComplete callback.");

        // Should complete immediately with no events
        assertTrue(completed.get());
        assertEquals(0, receivedEvents.size());
    }


    @Test
    public void testConsumeAllHandlesQueueClosedException() throws Exception {
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .mainEventBus(mainEventBus)
                .build().tap();
        EventConsumer consumer = new EventConsumer(queue);

        // Add a message event (which will complete the stream)
        Event message = fromJson(MESSAGE_PAYLOAD, Message.class);

        // Use callback to wait for event processing
        waitForEventProcessing(() -> queue.enqueueEvent(message));

        // Close the queue before consuming
        queue.close();

        Flow.Publisher<EventQueueItem> publisher = consumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(EventQueueItem item) {
                receivedEvents.add(item.getEvent());
            }

            @Override
            public void onError(Throwable throwable) {
                // Should not be called
                completionLatch.countDown();
            }

            @Override
            public void onComplete() {
                completed.set(true);
                completionLatch.countDown();
            }
        });

        // Wait for completion with timeout
        assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "Test timed out waiting for onComplete callback.");

        // Should have received the message and completed
        assertTrue(completed.get());
        assertEquals(1, receivedEvents.size());
        assertSame(message, receivedEvents.get(0));
    }

    @Test
    public void testConsumeAllTerminatesOnQueueClosedEvent() throws Exception {
        EventQueue queue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .mainEventBus(mainEventBus)
                .build().tap();
        EventConsumer consumer = new EventConsumer(queue);

        // Enqueue a QueueClosedEvent (poison pill)
        QueueClosedEvent queueClosedEvent = new QueueClosedEvent(TASK_ID);
        queue.enqueueEvent(queueClosedEvent);

        Flow.Publisher<EventQueueItem> publisher = consumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch completionLatch = new CountDownLatch(1);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(EventQueueItem item) {
                receivedEvents.add(item.getEvent());
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                completionLatch.countDown();
            }

            @Override
            public void onComplete() {
                completed.set(true);
                completionLatch.countDown();
            }
        });

        // Wait for completion with timeout
        assertTrue(completionLatch.await(5, TimeUnit.SECONDS), "Test timed out waiting for completion callback.");

        // Should complete gracefully without error
        assertTrue(completed.get(), "Stream should complete normally");
        assertNull(error.get(), "Stream should not error");

        // The poison pill should not be delivered to subscribers
        assertEquals(0, receivedEvents.size(), "QueueClosedEvent should be intercepted, not delivered");
    }

    private void enqueueAndConsumeOneEvent(Event event) throws Exception {
        // Use callback to wait for event processing
        waitForEventProcessing(() -> eventQueue.enqueueEvent(event));

        // Event is now available, consume it directly
        Event result = eventConsumer.consumeOne();
        assertNotNull(result, "Event should be available");
        assertSame(event, result);
    }

    // Helper methods to access private error field via reflection
    private Throwable getEventConsumerError() {
        try {
            java.lang.reflect.Field errorField = EventConsumer.class.getDeclaredField("error");
            errorField.setAccessible(true);
            return (Throwable) errorField.get(eventConsumer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access error field", e);
        }
    }

    private void setEventConsumerError(Throwable error) {
        try {
            java.lang.reflect.Field errorField = EventConsumer.class.getDeclaredField("error");
            errorField.setAccessible(true);
            errorField.set(eventConsumer, error);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set error field", e);
        }
    }
}
