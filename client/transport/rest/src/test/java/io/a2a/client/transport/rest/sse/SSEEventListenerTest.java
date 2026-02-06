package io.a2a.client.transport.rest.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.spec.Message;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

/**
 * Tests for REST transport SSEEventListener.
 * Tests JSON parsing of protobuf messages and event handling.
 */
public class SSEEventListenerTest {

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

    @Test
    public void testOnMessageWithTaskResult() {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // JSON format from REST SSE stream (protobuf as JSON)
        String jsonMessage = """
            {
              "task": {
                "id": "task-123",
                "contextId": "context-456",
                "status": {
                  "state": "TASK_STATE_WORKING"
                }
              }
            }
            """;

        // Call the onMessage method
        listener.onMessage(jsonMessage, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task);
        Task task = (Task) receivedEvent.get();
        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState.WORKING, task.status().state());
    }

    @Test
    public void testOnMessageWithMessageResult() {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // JSON format from REST SSE stream
        String jsonMessage = """
            {
              "message": {
                "role": "ROLE_AGENT",
                "messageId": "msg-123",
                "contextId": "context-456",
                "parts": [
                  {
                    "text": "Hello, world!"
                  }
                ]
              }
            }
            """;

        // Call onMessage method
        listener.onMessage(jsonMessage, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Message);
        Message message = (Message) receivedEvent.get();
        assertEquals(Message.Role.AGENT, message.role());
        assertEquals("msg-123", message.messageId());
        assertEquals("context-456", message.contextId());
        assertEquals(1, message.parts().size());
        assertTrue(message.parts().get(0) instanceof TextPart);
        assertEquals("Hello, world!", ((TextPart) message.parts().get(0)).text());
    }

    @Test
    public void testOnMessageWithStatusUpdateEvent() {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // JSON format from REST SSE stream
        String jsonMessage = """
            {
              "statusUpdate": {
                "taskId": "1",
                "contextId": "2",
                "status": {
                  "state": "TASK_STATE_SUBMITTED"
                }
              }
            }
            """;

        // Call onMessage method
        listener.onMessage(jsonMessage, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.taskId());
        assertEquals("2", taskStatusUpdateEvent.contextId());
        assertEquals(TaskState.SUBMITTED, taskStatusUpdateEvent.status().state());
        assertEquals(false, taskStatusUpdateEvent.isFinal());
    }

    @Test
    public void testOnMessageWithFinalStatusUpdateEventCancels() {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // JSON format from REST SSE stream with final status
        String jsonMessage = """
            {
              "statusUpdate": {
                "taskId": "1",
                "contextId": "2",
                "status": {
                  "state": "TASK_STATE_COMPLETED"
                }
              }
            }
            """;

        // Call onMessage with a cancellable future
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onMessage(jsonMessage, future);

        // Verify the event was received and processed
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent received = (TaskStatusUpdateEvent) receivedEvent.get();
        assertTrue(received.isFinal());
        assertEquals(TaskState.COMPLETED, received.status().state());

        // Verify the future was cancelled (auto-close on final event)
        assertTrue(future.wasCancelled());
    }

    @Test
    public void testOnMessageWithCompletedTaskCancels() {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // JSON format from REST SSE stream with completed task
        String jsonMessage = """
            {
              "task": {
                "id": "task-123",
                "contextId": "context-456",
                "status": {
                  "state": "TASK_STATE_COMPLETED"
                }
              }
            }
            """;

        // Call onMessage with a cancellable future
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onMessage(jsonMessage, future);

        // Verify the event was received
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task);
        Task task = (Task) receivedEvent.get();
        assertEquals(TaskState.COMPLETED, task.status().state());

        // Verify the future was cancelled (auto-close on final task state)
        assertTrue(future.wasCancelled());
    }

    @Test
    public void testOnMessageWithInvalidJsonCallsErrorHandler() {
        // Set up error handler
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error)
        );

        // Invalid JSON message
        String invalidJson = "{ invalid json }";

        // Call onMessage
        listener.onMessage(invalidJson, null);

        // Verify error handler was called
        assertNotNull(receivedError.get());
    }

    @Test
    public void testOnMessageWithInvalidPayloadCaseCallsErrorHandler() {
        // Set up error handler
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error)
        );

        // JSON with no recognized payload type (empty object)
        String jsonMessage = "{}";

        // Call onMessage
        listener.onMessage(jsonMessage, null);

        // Verify error handler was called
        assertNotNull(receivedError.get());
        assertTrue(receivedError.get() instanceof IllegalStateException);
        assertTrue(receivedError.get().getMessage().contains("Invalid stream response"));
    }

    @Test
    public void testOnErrorCallsErrorHandler() {
        // Set up error handler
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error)
        );

        // Create a test error
        IllegalStateException testError = new IllegalStateException("Test error");

        // Call onError
        listener.onError(testError, null);

        // Verify error handler was called
        assertNotNull(receivedError.get());
        assertEquals(testError, receivedError.get());
    }

    @Test
    public void testOnErrorCancelsFuture() {
        // Set up error handler
        AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> errorHandlerCalled.set(true)
        );

        // Create a cancel-capturing future
        CancelCapturingFuture future = new CancelCapturingFuture();

        // Call onError with a future
        listener.onError(new RuntimeException("Test error"), future);

        // Verify both error handler was called and future was cancelled
        assertTrue(errorHandlerCalled.get());
        assertTrue(future.wasCancelled());
    }

    @Test
    public void testOnMessageWithNullErrorHandler() {
        // Set up with null error handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                null  // Null error handler
        );

        // Invalid JSON message
        String invalidJson = "{ invalid json }";

        // Call onMessage - should not throw even with null error handler
        listener.onMessage(invalidJson, null);

        // No exception thrown means test passes
    }
}
