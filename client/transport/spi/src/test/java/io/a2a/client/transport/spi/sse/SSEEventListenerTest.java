package io.a2a.client.transport.spi.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.a2a.spec.Message;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for BaseSSEEventListener abstract class.
 * Tests the common functionality provided by the base class for handling events and errors.
 */
public class SSEEventListenerTest {

    private static final String TEST_TASK_ID = "task-123";
    private static final String TEST_CONTEXT_ID = "ctx-456";
    private static final String TEST_MESSAGE_ID = "msg-123";
    private static final String TEST_TEXT = "test";

    /**
     * Concrete implementation of BaseSSEEventListener for testing.
     * Simulates parsing by directly calling handleEvent with provided test data.
     */
    private static class TestSSEEventListener extends AbstractSSEEventListener {
        private StreamingEventKind eventToHandle;

        public TestSSEEventListener(Consumer<StreamingEventKind> eventHandler,
                                    @Nullable Consumer<Throwable> errorHandler) {
            super(eventHandler, errorHandler);
        }

        @Override
        public void onMessage(String message, @Nullable Future<Void> completableFuture) {
            if (eventToHandle != null) {
                handleEvent(eventToHandle, completableFuture);
            }
        }

        public void setEventToHandle(StreamingEventKind event) {
            this.eventToHandle = event;
        }
    }

    /**
     * Mock Future implementation that captures cancel calls.
     */
    private static class CancelCapturingFuture implements Future<Void> {
        private boolean cancelHandlerCalled = false;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelHandlerCalled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelHandlerCalled;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Void get() {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) {
            return null;
        }

        public boolean wasCancelled() {
            return cancelHandlerCalled;
        }
    }

    // Helper methods for creating test objects

    private static Message createMessage(Message.Role role) {
        return Message.builder()
                .role(role)
                .messageId(TEST_MESSAGE_ID)
                .contextId(TEST_CONTEXT_ID)
                .parts(java.util.List.of(new TextPart(TEST_TEXT)))
                .build();
    }

    private static Task createTask(TaskState state) {
        return Task.builder()
                .id(TEST_TASK_ID)
                .contextId(TEST_CONTEXT_ID)
                .status(new TaskStatus(state))
                .build();
    }

    private static TaskStatusUpdateEvent createTaskStatusUpdateEvent(TaskState state, boolean isFinal) {
        // Use constructor since Builder doesn't have isFinal method
        return new TaskStatusUpdateEvent(
                TEST_TASK_ID,
                new TaskStatus(state),
                TEST_CONTEXT_ID,
                isFinal,
                null
        );
    }

    private static TestSSEEventListener createListenerWithEventCapture(AtomicReference<StreamingEventKind> eventCapture) {
        return new TestSSEEventListener(eventCapture::set, null);
    }

    private static TestSSEEventListener createListenerWithErrorCapture(AtomicReference<Throwable> errorCapture) {
        return new TestSSEEventListener(event -> {}, errorCapture::set);
    }

    private static TestSSEEventListener createBasicListener() {
        return new TestSSEEventListener(event -> {}, null);
    }

    // Tests

    @Test
    public void testHandleEventCallsEventHandler() {
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithEventCapture(receivedEvent);

        Message message = createMessage(Message.Role.USER);

        listener.setEventToHandle(message);
        listener.onMessage(TEST_TEXT, null);

        assertNotNull(receivedEvent.get());
        assertEquals(message, receivedEvent.get());
    }

    @Test
    public void testHandleEventWithNullErrorHandler() {
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithEventCapture(receivedEvent);

        Task task = createTask(TaskState.WORKING);

        listener.setEventToHandle(task);
        listener.onMessage(TEST_TEXT, null);

        assertNotNull(receivedEvent.get());
    }

    @Test
    public void testOnErrorCallsErrorHandler() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithErrorCapture(receivedError);

        IllegalStateException testError = new IllegalStateException("Test error");

        listener.onError(testError, null);

        assertNotNull(receivedError.get());
        assertEquals(testError, receivedError.get());
    }

    @Test
    public void testOnErrorWithNullErrorHandler() {
        TestSSEEventListener listener = createBasicListener();

        // Should not throw even with null error handler
        listener.onError(new RuntimeException("Test error"), null);
    }

    @Test
    public void testOnErrorCancelsFuture() {
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        TestSSEEventListener listener = new TestSSEEventListener(
                event -> {},
                error -> errorHandlerCalled.set(true)
        );

        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onError(new RuntimeException("Test error"), future);

        assertTrue(errorHandlerCalled.get());
        assertTrue(future.wasCancelled());
    }

    @Test
    public void testShouldAutoCloseWithFinalTaskStatusUpdateEvent() {
        TestSSEEventListener listener = createBasicListener();
        TaskStatusUpdateEvent finalEvent = createTaskStatusUpdateEvent(TaskState.COMPLETED, true);

        assertTrue(listener.shouldAutoClose(finalEvent));
    }

    @Test
    public void testShouldAutoCloseWithNonFinalTaskStatusUpdateEvent() {
        TestSSEEventListener listener = createBasicListener();
        TaskStatusUpdateEvent nonFinalEvent = createTaskStatusUpdateEvent(TaskState.WORKING, false);

        assertFalse(listener.shouldAutoClose(nonFinalEvent));
    }

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"COMPLETED", "FAILED", "CANCELED"})
    public void testShouldAutoCloseWithFinalTaskStates(TaskState finalState) {
        TestSSEEventListener listener = createBasicListener();
        Task task = createTask(finalState);

        assertTrue(listener.shouldAutoClose(task), 
                "Task with state " + finalState + " should trigger auto-close");
    }

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"WORKING", "SUBMITTED"})
    public void testShouldAutoCloseWithNonFinalTaskStates(TaskState nonFinalState) {
        TestSSEEventListener listener = createBasicListener();
        Task task = createTask(nonFinalState);

        assertFalse(listener.shouldAutoClose(task), 
                "Task with state " + nonFinalState + " should not trigger auto-close");
    }

    @Test
    public void testShouldAutoCloseWithMessage() {
        TestSSEEventListener listener = createBasicListener();
        Message message = createMessage(Message.Role.AGENT);

        assertFalse(listener.shouldAutoClose(message));
    }

    @Test
    public void testAutoCloseCancelsFutureForFinalEvent() {
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithEventCapture(receivedEvent);

        TaskStatusUpdateEvent finalEvent = createTaskStatusUpdateEvent(TaskState.COMPLETED, true);
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.setEventToHandle(finalEvent);
        listener.onMessage(TEST_TEXT, future);

        assertNotNull(receivedEvent.get());
        assertEquals(finalEvent, receivedEvent.get());
        assertTrue(future.wasCancelled());
    }

    @Test
    public void testAutoCloseDoesNotCancelFutureForNonFinalEvent() {
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithEventCapture(receivedEvent);

        Message message = createMessage(Message.Role.AGENT);
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.setEventToHandle(message);
        listener.onMessage(TEST_TEXT, future);

        assertNotNull(receivedEvent.get());
        assertFalse(future.wasCancelled());
    }

    @Test
    public void testAutoCloseWithNullFuture() {
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        TestSSEEventListener listener = createListenerWithEventCapture(receivedEvent);

        TaskStatusUpdateEvent finalEvent = createTaskStatusUpdateEvent(TaskState.COMPLETED, true);

        // Should not throw with null future
        listener.setEventToHandle(finalEvent);
        listener.onMessage(TEST_TEXT, null);

        assertNotNull(receivedEvent.get());
    }
}