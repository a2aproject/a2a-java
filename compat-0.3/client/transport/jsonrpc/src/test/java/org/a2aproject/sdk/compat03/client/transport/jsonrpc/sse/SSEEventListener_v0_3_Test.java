package org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse;

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

import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonStreamingMessages_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

public class SSEEventListener_v0_3_Test {

    @Test
    public void testOnEventWithTaskResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the task event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_TASK_EVENT.substring(
                JsonStreamingMessages_v0_3.STREAMING_TASK_EVENT.indexOf("{"));
        
        // Call the onEvent method directly
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Task_v0_3);
        Task_v0_3 task = (Task_v0_3) receivedEvent.get();
        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState_v0_3.WORKING, task.status().state());
    }

    @Test
    public void testOnEventWithMessageResult() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_MESSAGE_EVENT.substring(
                JsonStreamingMessages_v0_3.STREAMING_MESSAGE_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof Message_v0_3);
        Message_v0_3 message = (Message_v0_3) receivedEvent.get();
        assertEquals(Message_v0_3.Role.AGENT, message.role());
        assertEquals("msg-123", message.messageId());
        assertEquals("context-456", message.contextId());
        assertEquals(1, message.parts().size());
        assertTrue(message.parts().get(0) instanceof TextPart_v0_3);
        assertEquals("Hello, world!", ((TextPart_v0_3) message.parts().get(0)).text());
    }

    @Test
    public void testOnEventWithTaskStatusUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_STATUS_UPDATE_EVENT.substring(
                JsonStreamingMessages_v0_3.STREAMING_STATUS_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent_v0_3);
        TaskStatusUpdateEvent_v0_3 taskStatusUpdateEvent = (TaskStatusUpdateEvent_v0_3) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.taskId());
        assertEquals("2", taskStatusUpdateEvent.contextId());
        assertFalse(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState_v0_3.SUBMITTED, taskStatusUpdateEvent.status().state());
    }

    @Test
    public void testOnEventWithTaskArtifactUpdateEventEvent() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_ARTIFACT_UPDATE_EVENT.substring(
                JsonStreamingMessages_v0_3.STREAMING_ARTIFACT_UPDATE_EVENT.indexOf("{"));

        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskArtifactUpdateEvent_v0_3);

        TaskArtifactUpdateEvent_v0_3 taskArtifactUpdateEvent = (TaskArtifactUpdateEvent_v0_3) receivedEvent.get();
        assertEquals("1", taskArtifactUpdateEvent.taskId());
        assertEquals("2", taskArtifactUpdateEvent.contextId());
        assertFalse(taskArtifactUpdateEvent.append());
        assertTrue(taskArtifactUpdateEvent.lastChunk());
        Artifact_v0_3 artifact = taskArtifactUpdateEvent.artifact();
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals(1, artifact.parts().size());
        assertEquals(Part_v0_3.Kind.TEXT, ((TextPart_v0_3) artifact.parts().get(0)).kind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) artifact.parts().get(0)).text());
    }

    @Test
    public void testOnEventWithError() throws Exception {
        // Set up event handler
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> {},
                error -> receivedError.set(error)
        );

        // Parse the error event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_ERROR_EVENT.substring(
                JsonStreamingMessages_v0_3.STREAMING_ERROR_EVENT.indexOf("{"));
        
        // Call onEvent method
        listener.onMessage(eventData, null);

        // Verify the error was processed correctly
        assertNotNull(receivedError.get());
        assertInstanceOf(JSONRPCError_v0_3.class, receivedError.get());
        JSONRPCError_v0_3 jsonrpcError = (JSONRPCError_v0_3) receivedError.get();
        assertEquals(-32602, jsonrpcError.getCode());
        assertEquals("Invalid parameters", jsonrpcError.getMessage());
        assertEquals("Missing required field", jsonrpcError.getData());
    }

    @Test
    public void testOnFailure() {
        AtomicBoolean failureHandlerCalled = new AtomicBoolean(false);
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
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
        TaskStatusUpdateEvent_v0_3 tsue = new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("1234")
                .contextId("xyz")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .isFinal(true)
                .build();

        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );


    }

    @Test
    public void testOnEventWithFinalTaskStatusUpdateEventEventCancels() throws Exception {
        // Set up event handler
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        SSEEventListener_v0_3 listener = new SSEEventListener_v0_3(
                event -> receivedEvent.set(event),
                error -> {}
        );

        // Parse the message event JSON
        String eventData = JsonStreamingMessages_v0_3.STREAMING_STATUS_UPDATE_EVENT_FINAL.substring(
                JsonStreamingMessages_v0_3.STREAMING_STATUS_UPDATE_EVENT_FINAL.indexOf("{"));

        // Call onEvent method
        CancelCapturingFuture future = new CancelCapturingFuture();
        listener.onMessage(eventData, future);

        // Verify the event was processed correctly
        assertNotNull(receivedEvent.get());
        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent_v0_3);
        TaskStatusUpdateEvent_v0_3 taskStatusUpdateEvent = (TaskStatusUpdateEvent_v0_3) receivedEvent.get();
        assertEquals("1", taskStatusUpdateEvent.taskId());
        assertEquals("2", taskStatusUpdateEvent.contextId());
        assertTrue(taskStatusUpdateEvent.isFinal());
        assertEquals(TaskState_v0_3.COMPLETED, taskStatusUpdateEvent.status().state());

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