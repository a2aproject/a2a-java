package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.A2AError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventConsumerTest {

    private EventQueue eventQueue;
    private EventConsumer eventConsumer;


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
        eventConsumer = new EventConsumer(eventQueue);
    }

    @Test
    public void testConsumeOneTaskEvent() throws Exception {
        Task event = Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneMessageEvent() throws Exception {
        Event event = Utils.unmarshalFrom(MESSAGE_PAYLOAD, Message.TYPE_REFERENCE);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneA2AErrorEvent() throws Exception {
        Event event = new A2AError() {};
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneJsonRpcErrorEvent() throws Exception {
        Event event = new JSONRPCError(123, "Some Error", null);
        enqueueAndConsumeOneEvent(event);
    }

    @Test
    public void testConsumeOneQueueEmpty() throws A2AServerException {
        assertThrows(A2AServerException.class, () -> eventConsumer.consumeOne());
    }

    @Test
    public void testConsumeAllMultipleEvents() throws JsonProcessingException {
        List<Event> events = List.of(
                Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE),
                new TaskArtifactUpdateEvent.Builder()
                        .taskId("task-123")
                        .contextId("session-xyz")
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId("task-123")
                        .contextId("session-xyz")
                        .status(new TaskStatus(TaskState.WORKING))
                        .isFinal(true)
                        .build());

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<Event> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Event item) {
                receivedEvents.add(item);
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
        });

        assertNull(error.get());
        assertEquals(events.size(), receivedEvents.size());
        for (int i = 0; i < events.size(); i++) {
            assertSame(events.get(i), receivedEvents.get(i));
        }
    }

    @Test
    public void testConsumeUntilMessage() throws Exception {
        List<Event> events = List.of(
                Utils.unmarshalFrom(MINIMAL_TASK, Task.TYPE_REFERENCE),
                new TaskArtifactUpdateEvent.Builder()
                        .taskId("task-123")
                        .contextId("session-xyz")
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId("task-123")
                        .contextId("session-xyz")
                        .status(new TaskStatus(TaskState.WORKING))
                        .isFinal(true)
                        .build());

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<Event> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Event item) {
                receivedEvents.add(item);
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
        });

        assertNull(error.get());
        assertEquals(3, receivedEvents.size());
        for (int i = 0; i < 3; i++) {
            assertSame(events.get(i), receivedEvents.get(i));
        }
    }

    @Test
    public void testConsumeMessageEvents() throws Exception {
        Message message = Utils.unmarshalFrom(MESSAGE_PAYLOAD, Message.TYPE_REFERENCE);
        Message message2 = new Message.Builder(message).build();

        List<Event> events = List.of(message, message2);

        for (Event event : events) {
            eventQueue.enqueueEvent(event);
        }

        Flow.Publisher<Event> publisher = eventConsumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Event item) {
                receivedEvents.add(item);
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
        });

        assertNull(error.get());
        // The stream is closed after the first Message
        assertEquals(1, receivedEvents.size());
        assertSame(message, receivedEvents.get(0));
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
    public void testConsumeAllRaisesStoredException() {
        // Set an error in the event consumer
        setEventConsumerError(new RuntimeException("Stored error"));

        Flow.Publisher<Event> publisher = eventConsumer.consumeAll();
        final AtomicReference<Throwable> receivedError = new AtomicReference<>();

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(Event item) {
                // Should not be called
            }

            @Override
            public void onError(Throwable throwable) {
                receivedError.set(throwable);
            }

            @Override
            public void onComplete() {
                // Should not be called
            }
        });

        assertNotNull(receivedError.get());
        assertEquals("Stored error", receivedError.get().getMessage());
    }

    @Test
    public void testConsumeAllStopsOnQueueClosed() throws Exception {
        EventQueue queue = EventQueue.create();
        EventConsumer consumer = new EventConsumer(queue);

        // Close the queue immediately
        queue.close();

        Flow.Publisher<Event> publisher = consumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Event item) {
                receivedEvents.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                // Should not be called
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        // Should complete immediately with no events
        assertTrue(completed.get());
        assertEquals(0, receivedEvents.size());
    }


    @Test
    public void testConsumeAllHandlesQueueClosedException() throws Exception {
        EventQueue queue = EventQueue.create();
        EventConsumer consumer = new EventConsumer(queue);

        // Add a message event (which will complete the stream)
        Event message = Utils.unmarshalFrom(MESSAGE_PAYLOAD, Message.TYPE_REFERENCE);
        queue.enqueueEvent(message);

        // Close the queue before consuming
        queue.close();

        Flow.Publisher<Event> publisher = consumer.consumeAll();
        final List<Event> receivedEvents = new ArrayList<>();
        final AtomicReference<Boolean> completed = new AtomicReference<>(false);

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Event item) {
                receivedEvents.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                // Should not be called
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        // Should have received the message and completed
        assertTrue(completed.get());
        assertEquals(1, receivedEvents.size());
        assertSame(message, receivedEvents.get(0));
    }

    private void enqueueAndConsumeOneEvent(Event event) throws Exception {
        eventQueue.enqueueEvent(event);
        Event result = eventConsumer.consumeOne();
        assertSame(event, result);
    }

    // Helper methods to access private error field via reflection
    private Throwable getEventConsumerError() {
        try {
            java.lang.reflect.Field errorField = EventConsumer.class.getDeclaredField("error");
            errorField.setAccessible(true);
            return (Throwable) errorField.get(eventConsumer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access error field", e);
        }
    }

    private void setEventConsumerError(Throwable error) {
        try {
            java.lang.reflect.Field errorField = EventConsumer.class.getDeclaredField("error");
            errorField.setAccessible(true);
            errorField.set(eventConsumer, error);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set error field", e);
        }
    }
}
