package io.a2a.extras.queuemanager.replicated.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.a2a.util.Utils;
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
        Task originalTask = new Task.Builder()
                .id("test-task-123")
                .contextId("test-context-456")
                .status(status)
                .build();

        // Test serialization as Event
        String json = Utils.OBJECT_MAPPER.writeValueAsString((Event) originalTask);
        assertTrue(json.contains("\"kind\":\"task\""), "JSON should contain task kind");
        assertTrue(json.contains("\"id\":\"test-task-123\""), "JSON should contain task ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(Task.class, deserializedEvent, "Should deserialize to Task");

        Task deserializedTask = (Task) deserializedEvent;
        assertEquals(originalTask.getId(), deserializedTask.getId());
        assertEquals(originalTask.getKind(), deserializedTask.getKind());
        assertEquals(originalTask.getContextId(), deserializedTask.getContextId());
        assertEquals(originalTask.getStatus().state(), deserializedTask.getStatus().state());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(Task.class, deserializedAsStreaming, "Should deserialize to Task as StreamingEventKind");
    }

    @Test
    public void testMessageSerialization() throws JsonProcessingException {
        // Create a Message
        Message originalMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Hello, world!")))
                .taskId("test-task-789")
                .messageId("test-msg-456")
                .contextId("test-context-123")
                .build();

        // Test serialization as Event
        String json = Utils.OBJECT_MAPPER.writeValueAsString((Event) originalMessage);
        assertTrue(json.contains("\"kind\":\"message\""), "JSON should contain message kind");
        assertTrue(json.contains("\"taskId\":\"test-task-789\""), "JSON should contain task ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(Message.class, deserializedEvent, "Should deserialize to Message");

        Message deserializedMessage = (Message) deserializedEvent;
        assertEquals(originalMessage.getTaskId(), deserializedMessage.getTaskId());
        assertEquals(originalMessage.getKind(), deserializedMessage.getKind());
        assertEquals(originalMessage.getRole(), deserializedMessage.getRole());
        assertEquals(originalMessage.getParts().size(), deserializedMessage.getParts().size());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(Message.class, deserializedAsStreaming, "Should deserialize to Message as StreamingEventKind");
    }

    @Test
    public void testTaskStatusUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskStatusUpdateEvent
        TaskStatus status = new TaskStatus(TaskState.COMPLETED);
        TaskStatusUpdateEvent originalEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("test-task-abc")
                .contextId("test-context-def")
                .status(status)
                .isFinal(true)
                .build();

        // Test serialization as Event
        String json = Utils.OBJECT_MAPPER.writeValueAsString((Event) originalEvent);
        assertTrue(json.contains("\"kind\":\"status-update\""), "JSON should contain status-update kind");
        assertTrue(json.contains("\"taskId\":\"test-task-abc\""), "JSON should contain task ID");
        assertTrue(json.contains("\"final\":true"), "JSON should contain final flag");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(TaskStatusUpdateEvent.class, deserializedEvent, "Should deserialize to TaskStatusUpdateEvent");

        TaskStatusUpdateEvent deserializedStatusEvent = (TaskStatusUpdateEvent) deserializedEvent;
        assertEquals(originalEvent.getTaskId(), deserializedStatusEvent.getTaskId());
        assertEquals(originalEvent.getKind(), deserializedStatusEvent.getKind());
        assertEquals(originalEvent.getContextId(), deserializedStatusEvent.getContextId());
        assertEquals(originalEvent.getStatus().state(), deserializedStatusEvent.getStatus().state());
        assertEquals(originalEvent.isFinal(), deserializedStatusEvent.isFinal());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(TaskStatusUpdateEvent.class, deserializedAsStreaming, "Should deserialize to TaskStatusUpdateEvent as StreamingEventKind");
    }

    @Test
    public void testTaskArtifactUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskArtifactUpdateEvent
        List<Part<?>> parts = List.of(new TextPart("Test artifact content"));
        Artifact artifact = new Artifact("test-artifact-123", "Test Artifact", "Test description", parts, null);
        TaskArtifactUpdateEvent originalEvent = new TaskArtifactUpdateEvent.Builder()
                .taskId("test-task-xyz")
                .contextId("test-context-uvw")
                .artifact(artifact)
                .build();

        // Test serialization as Event
        String json = Utils.OBJECT_MAPPER.writeValueAsString((Event) originalEvent);
        assertTrue(json.contains("\"kind\":\"artifact-update\""), "JSON should contain artifact-update kind");
        assertTrue(json.contains("\"taskId\":\"test-task-xyz\""), "JSON should contain task ID");
        assertTrue(json.contains("\"test-artifact-123\""), "JSON should contain artifact ID");

        // Test deserialization back to StreamingEventKind
        StreamingEventKind deserializedEvent = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
        assertInstanceOf(TaskArtifactUpdateEvent.class, deserializedEvent, "Should deserialize to TaskArtifactUpdateEvent");

        TaskArtifactUpdateEvent deserializedArtifactEvent = (TaskArtifactUpdateEvent) deserializedEvent;
        assertEquals(originalEvent.getTaskId(), deserializedArtifactEvent.getTaskId());
        assertEquals(originalEvent.getKind(), deserializedArtifactEvent.getKind());
        assertEquals(originalEvent.getContextId(), deserializedArtifactEvent.getContextId());
        assertEquals(originalEvent.getArtifact().artifactId(), deserializedArtifactEvent.getArtifact().artifactId());
        assertEquals(originalEvent.getArtifact().name(), deserializedArtifactEvent.getArtifact().name());

        // Test as StreamingEventKind
        StreamingEventKind deserializedAsStreaming = Utils.OBJECT_MAPPER.readValue(json, StreamingEventKind.class);
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
            String json = Utils.OBJECT_MAPPER.writeValueAsString(originalError);
            assertTrue(json.contains("\"message\""), "JSON should contain error message for " + originalError.getClass().getSimpleName());

            // Test deserialization - it's acceptable to deserialize as base JSONRPCError
            JSONRPCError deserializedError = Utils.OBJECT_MAPPER.readValue(json, JSONRPCError.class);
            assertNotNull(deserializedError, "Should deserialize successfully for " + originalError.getClass().getSimpleName());
            assertEquals(originalError.getMessage(), deserializedError.getMessage(), "Error message should match for " + originalError.getClass().getSimpleName());
            assertEquals(originalError.getCode(), deserializedError.getCode(), "Error code should match for " + originalError.getClass().getSimpleName());

            // The deserialized error might be the base class, which is acceptable per the requirements
        }
    }

    @Test
    public void testReplicatedEventWithStreamingEventSerialization() throws JsonProcessingException {
        // Test that ReplicatedEvent can properly handle StreamingEventKind
        TaskStatusUpdateEvent statusEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("replicated-test-task")
                .contextId("replicated-test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(false)
                .build();

        // Create ReplicatedEvent with StreamingEventKind
        ReplicatedEvent originalReplicatedEvent = new ReplicatedEvent("replicated-test-task", statusEvent);

        // Serialize the ReplicatedEvent
        String json = Utils.OBJECT_MAPPER.writeValueAsString(originalReplicatedEvent);
        assertTrue(json.contains("\"taskId\":\"replicated-test-task\""), "JSON should contain task ID");
        assertTrue(json.contains("\"event\""), "JSON should contain event field");
        assertTrue(json.contains("\"kind\":\"status-update\""), "JSON should contain the event kind");
        assertFalse(json.contains("\"error\""), "JSON should not contain error field");

        // Deserialize the ReplicatedEvent
        ReplicatedEvent deserializedReplicatedEvent = Utils.OBJECT_MAPPER.readValue(json, ReplicatedEvent.class);
        assertEquals(originalReplicatedEvent.getTaskId(), deserializedReplicatedEvent.getTaskId());

        // Now we should get the proper type back!
        StreamingEventKind retrievedEvent = deserializedReplicatedEvent.getEvent();
        assertNotNull(retrievedEvent);
        assertInstanceOf(TaskStatusUpdateEvent.class, retrievedEvent, "Should deserialize to TaskStatusUpdateEvent");

        TaskStatusUpdateEvent retrievedStatusEvent = (TaskStatusUpdateEvent) retrievedEvent;
        assertEquals(statusEvent.getTaskId(), retrievedStatusEvent.getTaskId());
        assertEquals(statusEvent.getContextId(), retrievedStatusEvent.getContextId());
        assertEquals(statusEvent.getStatus().state(), retrievedStatusEvent.getStatus().state());
        assertEquals(statusEvent.isFinal(), retrievedStatusEvent.isFinal());

        // Test helper methods
        assertTrue(deserializedReplicatedEvent.hasEvent());
        assertFalse(deserializedReplicatedEvent.hasError());
        assertNull(deserializedReplicatedEvent.getError());
    }

    @Test
    public void testReplicatedEventWithErrorSerialization() throws JsonProcessingException {
        // Test that ReplicatedEvent can properly handle JSONRPCError
        InvalidRequestError error = new InvalidRequestError("Invalid request for testing");

        // Create ReplicatedEvent with JSONRPCError
        ReplicatedEvent originalReplicatedEvent = new ReplicatedEvent("error-test-task", error);

        // Serialize the ReplicatedEvent
        String json = Utils.OBJECT_MAPPER.writeValueAsString(originalReplicatedEvent);
        assertTrue(json.contains("\"taskId\":\"error-test-task\""), "JSON should contain task ID");
        assertTrue(json.contains("\"error\""), "JSON should contain error field");
        assertTrue(json.contains("\"message\""), "JSON should contain error message");
        assertFalse(json.contains("\"event\""), "JSON should not contain event field");

        // Deserialize the ReplicatedEvent
        ReplicatedEvent deserializedReplicatedEvent = Utils.OBJECT_MAPPER.readValue(json, ReplicatedEvent.class);
        assertEquals(originalReplicatedEvent.getTaskId(), deserializedReplicatedEvent.getTaskId());

        // Should get the error back
        JSONRPCError retrievedError = deserializedReplicatedEvent.getError();
        assertNotNull(retrievedError);
        assertEquals(error.getMessage(), retrievedError.getMessage());
        assertEquals(error.getCode(), retrievedError.getCode());

        // Test helper methods
        assertFalse(deserializedReplicatedEvent.hasEvent());
        assertTrue(deserializedReplicatedEvent.hasError());
        assertNull(deserializedReplicatedEvent.getEvent());
    }

    @Test
    public void testReplicatedEventBackwardCompatibility() throws JsonProcessingException {
        // Test backward compatibility with generic Event constructor
        TaskStatusUpdateEvent statusEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("backward-compat-task")
                .contextId("backward-compat-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();

        // Use the backward compatibility constructor
        ReplicatedEvent replicatedEvent = new ReplicatedEvent("backward-compat-task", (Event) statusEvent);

        // Should work the same as the specific constructor
        assertTrue(replicatedEvent.hasEvent());
        assertFalse(replicatedEvent.hasError());
        assertInstanceOf(TaskStatusUpdateEvent.class, replicatedEvent.getEvent());
    }
}