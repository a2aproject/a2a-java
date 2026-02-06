package io.a2a.client.transport.jsonrpc.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.client.transport.jsonrpc.JsonStreamingMessages;
import io.a2a.spec.A2AError;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

public class SSEEventListenerTest {

    @Test
    public void testOnEventWithTaskResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the task event JSON
        String eventData = JsonStreamingMessages.STREAMING_TASK_EVENT.substring(
                JsonStreamingMessages.STREAMING_TASK_EVENT.indexOf("{"));
        
        // Call the onEvent method directly
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task);
        Task task = (Task) receivedEvent.get();
        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState.WORKING, task.status().state());
    }

    @Test
    public void testOnEventWithMessageResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_MESSAGE_EVENT.substring(
                JsonStreamingMessages.STREAMING_MESSAGE_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

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
    public void testOnEventWithTaskStatusUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.taskId());
        assertEquals("2", taskStatusUpdateEvent.contextId());
        assertFalse(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState.SUBMITTED, taskStatusUpdateEvent.status().state());
    }

    @Test
    public void testOnEventWithTaskArtifactUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskArtifactUpdateEvent);

        TaskArtifactUpdateEvent taskArtifactUpdateEvent = (TaskArtifactUpdateEvent) receivedEvent.get();
        assertEquals("1", taskArtifactUpdateEvent.taskId());
        assertEquals("2", taskArtifactUpdateEvent.contextId());
        assertFalse(taskArtifactUpdateEvent.append());
        assertTrue(taskArtifactUpdateEvent.lastChunk());
        Artifact artifact = taskArtifactUpdateEvent.artifact();
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals(1, artifact.parts().size());
        assertTrue(artifact.parts().get(0) instanceof TextPart);
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) artifact.parts().get(0)).text());
    }

    @Test
    public void testOnEventWithError() throws Exception {
        // Set up event handler
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error)
        );

        // Parse the error event JSON
        String eventData = JsonStreamingMessages.STREAMING_ERROR_EVENT.substring(
                JsonStreamingMessages.STREAMING_ERROR_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the error was processed correctly
        assertNotNull(receivedError.get());
        assertInstanceOf(A2AError.class, receivedError.get());
        A2AError jsonrpcError = (A2AError) receivedError.get();
        assertEquals(-32602, jsonrpcError.getCode());
        assertEquals("Invalid parameters", jsonrpcError.getMessage());
        assertEquals("\"Missing required field\"", jsonrpcError.getData());
    }

    @Test
    public void testOnFailure() {
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> failureHandlerCalled.set(true)
        );

        // Simulate a failure
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onError(new RuntimeException("Test exception"), future);

        // Verify the failure handler was called
        assertTrue(failureHandlerCalled.get());
        // Verify it got cancelled
        assertTrue(future.cancelHandlerCalled);
    }

    @Test
    public void testFinalTaskStatusUpdateEventCancels() {
        TaskStatus completedStatus = new TaskStatus(TaskState.COMPLETED);
        // Use constructor since Builder doesn't have isFinal method
        TaskStatusUpdateEvent tsue = new TaskStatusUpdateEvent(
                "1234",
                completedStatus,
                "xyz",
                completedStatus.state().isFinal(),  // Derive from state
                null
        );

        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT_FINAL.substring(
                JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT_FINAL.indexOf("{"));

        // Call onMessage with a cancellable future
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onMessage(eventData, future);

        // Verify the event was received and processed
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent received = (TaskStatusUpdateEvent) receivedEvent.get();
        assertTrue(received.isFinal());

        // Verify the future was cancelled (auto-close on final event)
        assertTrue(future.cancelHandlerCalled);
    }

    @Test
    public void testOnEventWithFinalTaskStatusUpdateEventEventCancels() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT_FINAL.substring(
                JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT_FINAL.indexOf("{"));

        // Call onEvent method
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onMessage(eventData, future);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.taskId());
        assertEquals("2", taskStatusUpdateEvent.contextId());
        assertTrue(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState.COMPLETED, taskStatusUpdateEvent.status().state());

        assertTrue(future.cancelHandlerCalled);
    }


    private static class CancelCapturingFuture implements Future<Void> {
        private boolean cancelHandlerCalled;

        public CancelCapturingFuture() {
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelHandlerCalled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}