package io.a2a.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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

/**
 * Tests for StreamingEventKind serialization and deserialization.
 * <p>
 * Verifies that StreamingEventKind types (Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent)
 * serialize using wrapper member names (e.g., {"task": {...}}) and do not contain "kind" fields.
 */
class StreamingEventKindSerializationTest {

    @Test
    void testTaskSerialization() throws JsonProcessingException {
        // Create a Task
        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        // Serialize as StreamingEventKind
        String json = JsonUtil.toJson((StreamingEventKind) task);

        // Verify JSON contains task wrapper, not "kind" field
        assertNotNull(json);
        assertTrue(json.contains("\"task\""));
        assertTrue(json.contains("\"id\":\"task-123\""));
        assertTrue(json.contains("\"state\":\"submitted\""));
        assertFalse(json.contains("\"kind\""));

        // Deserialize back to StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Verify it's a Task
        assertInstanceOf(Task.class, deserialized);
        Task deserializedTask = (Task) deserialized;
        assertEquals(task.id(), deserializedTask.id());
        assertEquals(task.contextId(), deserializedTask.contextId());
        assertEquals(task.status().state(), deserializedTask.status().state());
    }

    @Test
    void testMessageSerialization() throws JsonProcessingException {
        // Create a Message
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Hello, agent!")))
                .taskId("task-789")
                .messageId("msg-123")
                .contextId("context-456")
                .build();

        // Serialize as StreamingEventKind
        String json = JsonUtil.toJson((StreamingEventKind) message);

        // Verify JSON contains message wrapper, not "kind" field
        assertNotNull(json);
        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("\"taskId\":\"task-789\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("Hello, agent!"));
        assertFalse(json.contains("\"kind\""));

        // Deserialize back to StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Verify it's a Message
        assertInstanceOf(Message.class, deserialized);
        Message deserializedMessage = (Message) deserialized;
        assertEquals(message.taskId(), deserializedMessage.taskId());
        assertEquals(message.role(), deserializedMessage.role());
        assertEquals(message.parts().size(), deserializedMessage.parts().size());
    }

    @Test
    void testTaskStatusUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskStatusUpdateEvent
        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId("task-abc")
                .contextId("context-def")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        // Serialize as StreamingEventKind
        String json = JsonUtil.toJson((StreamingEventKind) statusEvent);

        // Verify JSON contains statusUpdate wrapper, not "kind" field
        assertNotNull(json);
        assertTrue(json.contains("\"statusUpdate\""));
        assertTrue(json.contains("\"taskId\":\"task-abc\""));
        assertTrue(json.contains("\"state\":\"working\""));
        assertTrue(json.contains("\"final\":false"));
        assertFalse(json.contains("\"kind\""));

        // Deserialize back to StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Verify it's a TaskStatusUpdateEvent
        assertInstanceOf(TaskStatusUpdateEvent.class, deserialized);
        TaskStatusUpdateEvent deserializedEvent = (TaskStatusUpdateEvent) deserialized;
        assertEquals(statusEvent.taskId(), deserializedEvent.taskId());
        assertEquals(statusEvent.status().state(), deserializedEvent.status().state());
        assertEquals(statusEvent.isFinal(), deserializedEvent.isFinal());
    }

    @Test
    void testTaskArtifactUpdateEventSerialization() throws JsonProcessingException {
        // Create a TaskArtifactUpdateEvent
        Artifact artifact = Artifact.builder()
                .artifactId("artifact-xyz")
                .name("Test Artifact")
                .parts(List.of(new TextPart("Artifact content")))
                .build();

        TaskArtifactUpdateEvent artifactEvent = TaskArtifactUpdateEvent.builder()
                .taskId("task-123")
                .contextId("context-456")
                .artifact(artifact)
                .build();

        // Serialize as StreamingEventKind
        String json = JsonUtil.toJson((StreamingEventKind) artifactEvent);

        // Verify JSON contains artifactUpdate wrapper, not "kind" field
        assertNotNull(json);
        assertTrue(json.contains("\"artifactUpdate\""));
        assertTrue(json.contains("\"taskId\":\"task-123\""));
        assertTrue(json.contains("\"artifactId\":\"artifact-xyz\""));
        assertTrue(json.contains("Artifact content"));
        assertFalse(json.contains("\"kind\""));

        // Deserialize back to StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Verify it's a TaskArtifactUpdateEvent
        assertInstanceOf(TaskArtifactUpdateEvent.class, deserialized);
        TaskArtifactUpdateEvent deserializedEvent = (TaskArtifactUpdateEvent) deserialized;
        assertEquals(artifactEvent.taskId(), deserializedEvent.taskId());
        assertEquals(artifactEvent.artifact().artifactId(), deserializedEvent.artifact().artifactId());
    }

    @Test
    void testUnwrappedTaskDeserialization() throws JsonProcessingException {
        // Test that unwrapped Task format (direct deserialization) still works
        String json = """
            {
              "id": "task-unwrapped",
              "contextId": "context-999",
              "status": {
                "state": "completed"
              }
            }
            """;

        // Deserialize as StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Should successfully deserialize as Task
        assertInstanceOf(Task.class, deserialized);
        Task task = (Task) deserialized;
        assertEquals("task-unwrapped", task.id());
        assertEquals("context-999", task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
    }

    @Test
    void testUnwrappedMessageDeserialization() throws JsonProcessingException {
        // Test that unwrapped Message format (direct deserialization) still works
        String json = """
            {
              "role": "agent",
              "parts": [
                {
                  "text": "Unwrapped message"
                }
              ],
              "messageId": "msg-unwrapped",
              "taskId": "task-999"
            }
            """;

        // Deserialize as StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Should successfully deserialize as Message
        assertInstanceOf(Message.class, deserialized);
        Message message = (Message) deserialized;
        assertEquals("msg-unwrapped", message.messageId());
        assertEquals("task-999", message.taskId());
        assertEquals(Message.Role.AGENT, message.role());
    }

    @Test
    void testUnwrappedTaskStatusUpdateEventDeserialization() throws JsonProcessingException {
        // Test that unwrapped TaskStatusUpdateEvent format still works
        String json = """
            {
              "taskId": "task-status-unwrapped",
              "contextId": "context-999",
              "status": {
                "state": "working"
              },
              "final": false
            }
            """;

        // Deserialize as StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Should successfully deserialize as TaskStatusUpdateEvent
        assertInstanceOf(TaskStatusUpdateEvent.class, deserialized);
        TaskStatusUpdateEvent event = (TaskStatusUpdateEvent) deserialized;
        assertEquals("task-status-unwrapped", event.taskId());
        assertEquals(TaskState.WORKING, event.status().state());
        assertFalse(event.isFinal());
    }

    @Test
    void testUnwrappedTaskArtifactUpdateEventDeserialization() throws JsonProcessingException {
        // Test that unwrapped TaskArtifactUpdateEvent format still works
        String json = """
            {
              "taskId": "task-artifact-unwrapped",
              "contextId": "context-999",
              "artifact": {
                "artifactId": "artifact-unwrapped",
                "parts": [
                  {
                    "text": "Unwrapped artifact"
                  }
                ]
              }
            }
            """;

        // Deserialize as StreamingEventKind
        StreamingEventKind deserialized = JsonUtil.fromJson(json, StreamingEventKind.class);

        // Should successfully deserialize as TaskArtifactUpdateEvent
        assertInstanceOf(TaskArtifactUpdateEvent.class, deserialized);
        TaskArtifactUpdateEvent event = (TaskArtifactUpdateEvent) deserialized;
        assertEquals("task-artifact-unwrapped", event.taskId());
        assertEquals("artifact-unwrapped", event.artifact().artifactId());
    }
}
