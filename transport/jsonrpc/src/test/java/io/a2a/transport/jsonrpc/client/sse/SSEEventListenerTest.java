package io.a2a.transport.jsonrpc.client.sse;

import io.a2a.spec.Artifact;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.transport.jsonrpc.client.JsonStreamingMessages;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class SSEEventListenerTest {

    @Test
    public void testOnEventWithTaskResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the task event JSON
        String eventData = JsonStreamingMessages.STREAMING_TASK_EVENT.substring(
                JsonStreamingMessages.STREAMING_TASK_EVENT.indexOf("{"));
        
        // Call the onEvent method directly
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task);
        Task task = (Task) receivedEvent.get();
        assertEquals("task-123", task.getId());
        assertEquals("context-456", task.getContextId());
        assertEquals(TaskState.WORKING, task.getStatus().state());
    }

    @Test
    public void testOnEventWithMessageResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_MESSAGE_EVENT.substring(
                JsonStreamingMessages.STREAMING_MESSAGE_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Message);
        Message message = (Message) receivedEvent.get();
        assertEquals(Message.Role.AGENT, message.getRole());
        assertEquals("msg-123", message.getMessageId());
        assertEquals("context-456", message.getContextId());
        assertEquals(1, message.getParts().size());
        assertTrue(message.getParts().get(0) instanceof TextPart);
        assertEquals("Hello, world!", ((TextPart) message.getParts().get(0)).getText());
    }

    @Test
    public void testOnEventWithTaskStatusUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_STATUS_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent);
        TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.getTaskId());
        assertEquals("2", taskStatusUpdateEvent.getContextId());
        assertFalse(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState.SUBMITTED, taskStatusUpdateEvent.getStatus().state());
    }

    @Test
    public void testOnEventWithTaskArtifactUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

        // Parse the message event JSON
        String eventData = JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.substring(
                JsonStreamingMessages.STREAMING_ARTIFACT_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskArtifactUpdateEvent);

        TaskArtifactUpdateEvent taskArtifactUpdateEvent = (TaskArtifactUpdateEvent) receivedEvent.get();
        assertEquals("1", taskArtifactUpdateEvent.getTaskId());
        assertEquals("2", taskArtifactUpdateEvent.getContextId());
        assertFalse(taskArtifactUpdateEvent.isAppend());
        assertTrue(taskArtifactUpdateEvent.isLastChunk());
        Artifact artifact = taskArtifactUpdateEvent.getArtifact();
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals(1, artifact.parts().size());
        assertEquals(Part.Kind.TEXT, artifact.parts().get(0).getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) artifact.parts().get(0)).getText());
    }

    @Test
    public void testOnEventWithError() throws Exception {
        // Set up event handler
        AtomicReference<JSONRPCError> receivedError = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> receivedError.set(error),
                () -> {});

        // Parse the error event JSON
        String eventData = JsonStreamingMessages.STREAMING_ERROR_EVENT.substring(
                JsonStreamingMessages.STREAMING_ERROR_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the error was processed correctly
        assertNotNull(receivedError.get());
        assertEquals(-32602, receivedError.get().getCode());
        assertEquals("Invalid parameters", receivedError.get().getMessage());
        assertEquals("Missing required field", receivedError.get().getData());
    }

    @Test
    public void testOnFailure() {
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);
        SSEEventListener listener = new SSEEventListener(
                event -> {},
                error -> {},
                () -> failureHandlerCalled.set(true));

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
        TaskStatusUpdateEvent tsue = new TaskStatusUpdateEvent.Builder()
                .taskId("1234")
                .contextId("xyz")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();

        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});


    }

    @Test
    public void testOnEventWithFinalTaskStatusUpdateEventEventCancels() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        SSEEventListener listener = new SSEEventListener(
                event -> receivedEvent.set(event),
                error -> {},
                () -> {});

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
        assertEquals("1", taskStatusUpdateEvent.getTaskId());
        assertEquals("2", taskStatusUpdateEvent.getContextId());
        assertTrue(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState.COMPLETED, taskStatusUpdateEvent.getStatus().state());

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