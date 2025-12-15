package io.a2a.extras.queuemanager.replicated.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.a2a.json.JsonProcessingException;
import io.a2a.server.events.QueueClosedEvent;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.json.JsonUtil;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test for serialization/deserialization of all StreamingEventKind classes
 * and JSONRPCError subclasses to ensure proper type handling in replication.
 */
public class EventSerializationTest {

    @Test
    public void testTaskSerialization() throws JsonProcessingException {
        // Create a Task
        TaskStatus status = new TaskStatus(TaskState.SUBMITTED);
        Task originalTask = Task.builder()
                .id("test-task-123")
                .contextId("test-context-456")
                .status(status)
                .build();

        // Test serialization as Event
        String json = JsonUtil.toJson(originalTask);
        assertTrue(json.contains("\"kind\":\"task\""), "JSON should contain task kind");
        assertTrue(json.contains("\"id\":\"test-task-123\""), "JSON should contain task ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(Task.class, deserializedEvent, "Should deserialize to Task");

        Task deserializedTask = (Task) deserializedEvent;
        assertEquals(originalTask.getId(), deserializedTask.getId());
        assertEquals(originalTask.getKind(), deserializedTask.getKind());
        assertEquals(originalTask.getContextId(), deserializedTask.getContextId());
        assertEquals(originalTask.getStatus().state(), deserializedTask.getStatus().state());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(Task.class, deserializedAsStreaming, "Should deserialize to Task as StreamingEventKind");
    }

    @Test
    public void testMessageSerialization() throws JsonProcessingException {
        // Create a Message
        Message originalMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Hello, world!")))
                .taskId("test-task-789")
                .messageId("test-msg-456")
                .contextId("test-context-123")
                .build();

        // Test serialization as Event
        String json = JsonUtil.toJson(originalMessage);
        assertTrue(json.contains("\"kind\":\"message\""), "JSON should contain message kind");
        assertTrue(json.contains("\"taskId\":\"test-task-789\""), "JSON should contain task ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(Message.class, deserializedEvent, "Should deserialize to Message");

        Message deserializedMessage = (Message) deserializedEvent;
        assertEquals(originalMessage.getTaskId(), deserializedMessage.getTaskId());
        assertEquals(originalMessage.getKind(), deserializedMessage.getKind());
        assertEquals(originalMessage.getRole(), deserializedMessage.getRole());
        assertEquals(originalMessage.getParts().size(), deserializedMessage.getParts().size());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(Message.class, deserializedAsStreaming, "Should deserialize to Message as StreamingEventKind");
    }

    @Test
    public void testTaskStatusUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskStatusUpdateEvent
        TaskStatus status = new TaskStatus(TaskState.COMPLETED);
        TaskStatusUpdateEvent originalEvent = TaskStatusUpdateEvent.builder()
                .taskId("test-task-abc")
                .contextId("test-context-def")
                .status(status)
                .isFinal(true)
                .build();

        // Test serialization as Event
        String json = JsonUtil.toJson((Event) originalEvent);
        assertTrue(json.contains("\"kind\":\"status-update\""), "JSON should contain status-update kind");
        assertTrue(json.contains("\"taskId\":\"test-task-abc\""), "JSON should contain task ID");
        assertTrue(json.contains("\"final\":true"), "JSON should contain final flag");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(TaskStatusUpdateEvent.class, deserializedEvent, "Should deserialize to TaskStatusUpdateEvent");

        TaskStatusUpdateEvent deserializedStatusEvent = (TaskStatusUpdateEvent) deserializedEvent;
        assertEquals(originalEvent.getTaskId(), deserializedStatusEvent.getTaskId());
        assertEquals(originalEvent.getKind(), deserializedStatusEvent.getKind());
        assertEquals(originalEvent.getContextId(), deserializedStatusEvent.getContextId());
        assertEquals(originalEvent.getStatus().state(), deserializedStatusEvent.getStatus().state());
        assertEquals(originalEvent.isFinal(), deserializedStatusEvent.isFinal());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(TaskStatusUpdateEvent.class, deserializedAsStreaming, "Should deserialize to TaskStatusUpdateEvent as StreamingEventKind");
    }

    @Test
    public void testTaskArtifactUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskArtifactUpdateEvent
        List<Part<?>> parts = List.of(new TextPart("Test artifact content"));
        Artifact artifact = new Artifact("test-artifact-123", "Test Artifact", "Test description", parts, null, null);
        TaskArtifactUpdateEvent originalEvent = TaskArtifactUpdateEvent.builder()
                .taskId("test-task-xyz")
                .contextId("test-context-uvw")
                .artifact(artifact)
                .build();

        // Test serialization as Event
        String json = JsonUtil.toJson((Event) originalEvent);
        assertTrue(json.contains("\"kind\":\"artifact-update\""), "JSON should contain artifact-update kind");
        assertTrue(json.contains("\"taskId\":\"test-task-xyz\""), "JSON should contain task ID");
        assertTrue(json.contains("\"test-artifact-123\""), "JSON should contain artifact ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(TaskArtifactUpdateEvent.class, deserializedEvent, "Should deserialize to TaskArtifactUpdateEvent");

        TaskArtifactUpdateEvent deserializedArtifactEvent = (TaskArtifactUpdateEvent) deserializedEvent;
        assertEquals(originalEvent.getTaskId(), deserializedArtifactEvent.getTaskId());
        assertEquals(originalEvent.getKind(), deserializedArtifactEvent.getKind());
        assertEquals(originalEvent.getContextId(), deserializedArtifactEvent.getContextId());
        assertEquals(originalEvent.getArtifact().artifactId(), deserializedArtifactEvent.getArtifact().artifactId());
        assertEquals(originalEvent.getArtifact().name(), deserializedArtifactEvent.getArtifact().name());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = JsonUtil.fromJson(json, StreamingEventKind.class);
        assertInstanceOf(TaskArtifactUpdateEvent.class, deserializedAsStreaming, "Should deserialize to TaskArtifactUpdateEvent as StreamingEventKind");
    }

    @Test
    public void testJSONRPCErrorSubclassesSerialization() throws JsonProcessingException {
        // Test various JSONRPCError subclasses
        JSONRPCError[] errors = {
            new InvalidRequestError("Invalid request"),
            new MethodNotFoundError(),
            new InvalidParamsError("Invalid params"),
            new InternalError("Internal error"),
            new JSONParseError("Parse error"),
            new TaskNotFoundError(),
            new TaskNotCancelableError(),
            new UnsupportedOperationError(),
            new PushNotificationNotSupportedError()
            // Note: ContentTypeNotSupportedError and InvalidAgentResponseError need specific constructor parameters
        };

        for (JSONRPCError originalError : errors) {
            // Test serialization
            String json = JsonUtil.toJson(originalError);
            assertTrue(json.contains("\"message\""), "JSON should contain error message for " + originalError.getClass().getSimpleName());

            // Test deserialization - it's acceptable to deserialize as base JSONRPCError
            JSONRPCError deserializedError = JsonUtil.fromJson(json, JSONRPCError.class);
            assertNotNull(deserializedError, "Should deserialize successfully for " + originalError.getClass().getSimpleName());
            assertEquals(originalError.getMessage(), deserializedError.getMessage(), "Error message should match for " + originalError.getClass().getSimpleName());
            assertEquals(originalError.getCode(), deserializedError.getCode(), "Error code should match for " + originalError.getClass().getSimpleName());

            // The deserialized error might be the base class, which is acceptable per the requirements
        }
    }

    @Test
    public void testReplicatedEventWithStreamingEventSerialization() throws JsonProcessingException {
        // Test that ReplicatedEventQueueItem can properly handle StreamingEventKind
        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("replicated-test-task")
                .contextId("replicated-test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(false)
                .build();

        // Create ReplicatedEventQueueItem with StreamingEventKind
        ReplicatedEventQueueItem originalReplicatedEvent = new ReplicatedEventQueueItem("replicated-test-task", statusEvent);

        // Serialize the ReplicatedEventQueueItem
        String json = JsonUtil.toJson(originalReplicatedEvent);
        assertTrue(json.contains("\"taskId\":\"replicated-test-task\""), "JSON should contain task ID");
        assertTrue(json.contains("\"event\""), "JSON should contain event field");
        assertTrue(json.contains("\"kind\":\"status-update\""), "JSON should contain the event kind");
        assertFalse(json.contains("\"error\""), "JSON should not contain error field");

        // Deserialize the ReplicatedEventQueueItem
        ReplicatedEventQueueItem deserializedReplicatedEvent = JsonUtil.fromJson(json, ReplicatedEventQueueItem.class);
        assertEquals(originalReplicatedEvent.getTaskId(), deserializedReplicatedEvent.getTaskId());

        // Now we should get the proper type back!
        Event retrievedEventAsEvent = deserializedReplicatedEvent.getEvent();
        assertNotNull(retrievedEventAsEvent);
        assertInstanceOf(TaskStatusUpdateEvent.class, retrievedEventAsEvent, "Should deserialize to TaskStatusUpdateEvent");

        TaskStatusUpdateEvent retrievedStatusEvent = (TaskStatusUpdateEvent) retrievedEventAsEvent;
        assertEquals(statusEvent.getTaskId(), retrievedStatusEvent.getTaskId());
        assertEquals(statusEvent.getContextId(), retrievedStatusEvent.getContextId());
        assertEquals(statusEvent.getStatus().state(), retrievedStatusEvent.getStatus().state());
        assertEquals(statusEvent.isFinal(), retrievedStatusEvent.isFinal());

        // Test helper methods
        assertTrue(deserializedReplicatedEvent.hasEvent());
        assertFalse(deserializedReplicatedEvent.hasError());
        assertFalse(deserializedReplicatedEvent.isClosedEvent());
        assertNull(deserializedReplicatedEvent.getErrorObject());
    }

    @Test
    public void testReplicatedEventWithErrorSerialization() throws JsonProcessingException {
        // Test that ReplicatedEventQueueItem can properly handle JSONRPCError
        InvalidRequestError error = new InvalidRequestError("Invalid request for testing");

        // Create ReplicatedEventQueueItem with JSONRPCError
        ReplicatedEventQueueItem originalReplicatedEvent = new ReplicatedEventQueueItem("error-test-task", error);

        // Serialize the ReplicatedEventQueueItemQueueItem
        String json = JsonUtil.toJson(originalReplicatedEvent);
        assertTrue(json.contains("\"taskId\":\"error-test-task\""), "JSON should contain task ID");
        assertTrue(json.contains("\"error\""), "JSON should contain error field");
        assertTrue(json.contains("\"message\""), "JSON should contain error message");
        assertFalse(json.contains("\"event\""), "JSON should not contain event field");

        // Deserialize the ReplicatedEventQueueItem
        ReplicatedEventQueueItem deserializedReplicatedEvent = JsonUtil.fromJson(json, ReplicatedEventQueueItem.class);
        assertEquals(originalReplicatedEvent.getTaskId(), deserializedReplicatedEvent.getTaskId());

        // Should get the error back
        JSONRPCError retrievedError = deserializedReplicatedEvent.getErrorObject();
        assertNotNull(retrievedError);
        assertEquals(error.getMessage(), retrievedError.getMessage());
        assertEquals(error.getCode(), retrievedError.getCode());

        // Test helper methods
        assertFalse(deserializedReplicatedEvent.hasEvent());
        assertTrue(deserializedReplicatedEvent.hasError());
        assertFalse(deserializedReplicatedEvent.isClosedEvent());
        assertNull(deserializedReplicatedEvent.getStreamingEvent());
    }

    @Test
    public void testReplicatedEventBackwardCompatibility() throws JsonProcessingException {
        // Test backward compatibility with generic Event constructor
        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("backward-compat-task")
                .contextId("backward-compat-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();

        // Use the backward compatibility constructor
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem("backward-compat-task", (Event) statusEvent);

        // Should work the same as the specific constructor
        assertTrue(replicatedEvent.hasEvent());
        assertFalse(replicatedEvent.hasError());
        assertFalse(replicatedEvent.isClosedEvent());
        assertInstanceOf(TaskStatusUpdateEvent.class, replicatedEvent.getEvent());
    }

    @Test
    public void testQueueClosedEventSerialization() throws JsonProcessingException {
        // Test that QueueClosedEvent can be properly serialized and deserialized via ReplicatedEventQueueItem
        String taskId = "queue-closed-serialization-test";
        QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);

        // Create ReplicatedEventQueueItem with QueueClosedEvent
        ReplicatedEventQueueItem originalReplicatedEvent = new ReplicatedEventQueueItem(taskId, closedEvent);

        // Verify the item is marked as a closed event
        assertTrue(originalReplicatedEvent.isClosedEvent(), "Should be marked as closed event");
        assertFalse(originalReplicatedEvent.hasEvent(), "Should not have regular event");
        assertFalse(originalReplicatedEvent.hasError(), "Should not have error");

        // Serialize the ReplicatedEventQueueItem
        String json = JsonUtil.toJson(originalReplicatedEvent);
        assertTrue(json.contains("\"taskId\":\"" + taskId + "\""), "JSON should contain task ID");
        assertTrue(json.contains("\"closedEvent\":true"), "JSON should contain closedEvent flag set to true");
        assertFalse(json.contains("\"event\""), "JSON should not contain event field");
        assertFalse(json.contains("\"error\""), "JSON should not contain error field");

        // Deserialize the ReplicatedEventQueueItem
        ReplicatedEventQueueItem deserializedReplicatedEvent = JsonUtil.fromJson(json, ReplicatedEventQueueItem.class);
        assertEquals(taskId, deserializedReplicatedEvent.getTaskId());

        // Verify the deserialized item is marked as a closed event
        assertTrue(deserializedReplicatedEvent.isClosedEvent(), "Deserialized should be marked as closed event");
        assertFalse(deserializedReplicatedEvent.hasEvent(), "Deserialized should not have regular event");
        assertFalse(deserializedReplicatedEvent.hasError(), "Deserialized should not have error");

        // Verify getEvent() reconstructs the QueueClosedEvent
        Event retrievedEvent = deserializedReplicatedEvent.getEvent();
        assertNotNull(retrievedEvent, "getEvent() should return a reconstructed QueueClosedEvent");
        assertInstanceOf(QueueClosedEvent.class, retrievedEvent, "Should deserialize to QueueClosedEvent");

        QueueClosedEvent retrievedClosedEvent = (QueueClosedEvent) retrievedEvent;
        assertEquals(taskId, retrievedClosedEvent.getTaskId(), "Reconstructed event should have correct task ID");
    }
}