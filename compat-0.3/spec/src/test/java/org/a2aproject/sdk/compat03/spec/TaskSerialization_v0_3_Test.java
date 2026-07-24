package org.a2aproject.sdk.compat03.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;

/**
 * Tests for Task serialization and deserialization using Gson.
 */
class TaskSerialization_v0_3_Test {

    @Test
    void testBasicTaskSerialization() throws JsonProcessingException_v0_3 {
        // Create a basic task
        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
                .build();

        // Serialize to JSON
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains expected fields
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"task-123\""));
        assertTrue(json.contains("\"state\":\"submitted\""));

        // Deserialize back to Task
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify deserialized task matches original
        assertEquals(task.id(), deserialized.id());
        assertEquals(task.status().state(), deserialized.status().state());
    }

    @Test
    void testTaskWithTimestamp() throws JsonProcessingException_v0_3 {
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING, null, timestamp))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify OffsetDateTime timestamp is preserved
        assertNotNull(deserialized.status().timestamp());
        assertEquals(task.status().timestamp(), deserialized.status().timestamp());
    }

    @Test
    void testTaskWithArtifacts() throws JsonProcessingException_v0_3 {
        Artifact_v0_3 artifact = new Artifact_v0_3.Builder()
                .artifactId("artifact-1")
                .name("Test Artifact")
                .description("Description of artifact")
                .parts(List.of(
                        new TextPart_v0_3("Hello"),
                        new TextPart_v0_3("World")
                ))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains artifact data
        assertTrue(json.contains("\"artifactId\":\"artifact-1\""));
        assertTrue(json.contains("Hello"));
        assertTrue(json.contains("World"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify artifacts are preserved
        assertNotNull(deserialized.artifacts());
        assertEquals(1, deserialized.artifacts().size());
        assertEquals("artifact-1", deserialized.artifacts().get(0).artifactId());
        assertEquals(2, deserialized.artifacts().get(0).parts().size());
    }

    @Test
    void testTaskWithHistory() throws JsonProcessingException_v0_3 {
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(List.of(new TextPart_v0_3("Test message")))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING))
                .history(List.of(message))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains history data
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("Test message"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify history is preserved
        assertNotNull(deserialized.history());
        assertEquals(1, deserialized.history().size());
        assertEquals(Message_v0_3.Role.USER, deserialized.history().get(0).role());
        assertEquals(1, deserialized.history().get(0).parts().size());
    }

    @Test
    void testTaskWithAllFields() throws JsonProcessingException_v0_3 {
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-789")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING, null, timestamp))
                .history(List.of(
                        new Message_v0_3.Builder()
                                .role(Message_v0_3.Role.USER)
                                .parts(List.of(new TextPart_v0_3("User message")))
                                .build(),
                        new Message_v0_3.Builder()
                                .role(Message_v0_3.Role.AGENT)
                                .parts(List.of(new TextPart_v0_3("Agent response")))
                                .build()
                ))
                .artifacts(List.of(
                        new Artifact_v0_3.Builder()
                                .artifactId("artifact-1")
                                .parts(List.of(new TextPart_v0_3("Artifact content")))
                                .build()
                ))
                .metadata(Map.of("key1", "value1", "key2", 42))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify all fields are preserved
        assertEquals(task.id(), deserialized.id());
        assertEquals(task.contextId(), deserialized.contextId());
        assertEquals(task.status().state(), deserialized.status().state());
        assertEquals(task.status().timestamp(), deserialized.status().timestamp());
        assertEquals(task.history().size(), deserialized.history().size());
        assertEquals(task.artifacts().size(), deserialized.artifacts().size());
        assertNotNull(deserialized.metadata());
        assertEquals("value1", deserialized.metadata().get("key1"));
    }

    @Test
    void testTaskWithDifferentStates() throws JsonProcessingException_v0_3 {
        for (TaskState_v0_3 state : TaskState_v0_3.values()) {
            Task_v0_3 task = new Task_v0_3.Builder()
                    .id("task-" + state.asString())
                    .contextId("context-123")
                    .status(new TaskStatus_v0_3(state))
                    .build();

            // Serialize
            String json = JsonUtil_v0_3.toJson(task);

            // Verify state is serialized correctly
            assertTrue(json.contains("\"state\":\"" + state.asString() + "\""));

            // Deserialize
            Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

            // Verify state is preserved
            assertEquals(state, deserialized.status().state());
        }
    }

    @Test
    void testTaskWithNullOptionalFields() throws JsonProcessingException_v0_3 {
        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
                // artifacts, history, metadata not set
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify required fields are present
        assertEquals("task-123", deserialized.id());
        assertEquals("context-456", deserialized.contextId());
        assertEquals(TaskState_v0_3.SUBMITTED, deserialized.status().state());

        // Verify optional lists default to empty
        assertNotNull(deserialized.artifacts());
        assertEquals(0, deserialized.artifacts().size());
        assertNotNull(deserialized.history());
        assertEquals(0, deserialized.history().size());
    }

    @Test
    void testTaskWithFilePartBytes() throws JsonProcessingException_v0_3 {
        FilePart_v0_3 filePart = new FilePart_v0_3(new FileWithBytes_v0_3("application/pdf", "document.pdf", "base64data"));

        Artifact_v0_3 artifact = new Artifact_v0_3.Builder()
                .artifactId("file-artifact")
                .parts(List.of(filePart))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains file part data
        assertTrue(json.contains("\"kind\":\"file\""));
        assertTrue(json.contains("document.pdf"));
        assertTrue(json.contains("application/pdf"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify file part is preserved
        Part_v0_3<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart_v0_3);
        FilePart_v0_3 deserializedFilePart = (FilePart_v0_3) part;
        assertTrue(deserializedFilePart.file() instanceof FileWithBytes_v0_3);
        FileWithBytes_v0_3 fileWithBytes = (FileWithBytes_v0_3) deserializedFilePart.file();
        assertEquals("document.pdf", fileWithBytes.name());
        assertEquals("application/pdf", fileWithBytes.mimeType());
    }

    @Test
    void testTaskWithFilePartUri() throws JsonProcessingException_v0_3 {
        FilePart_v0_3 filePart = new FilePart_v0_3(new FileWithUri_v0_3("image/png", "photo.png", "https://example.com/photo.png"));

        Artifact_v0_3 artifact = new Artifact_v0_3.Builder()
                .artifactId("uri-artifact")
                .parts(List.of(filePart))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains URI
        assertTrue(json.contains("https://example.com/photo.png"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify file part URI is preserved
        Part_v0_3<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart_v0_3);
        FilePart_v0_3 deserializedFilePart = (FilePart_v0_3) part;
        assertTrue(deserializedFilePart.file() instanceof FileWithUri_v0_3);
        FileWithUri_v0_3 fileWithUri = (FileWithUri_v0_3) deserializedFilePart.file();
        assertEquals("https://example.com/photo.png", fileWithUri.uri());
    }

    @Test
    void testTaskWithDataPart() throws JsonProcessingException_v0_3 {
        DataPart_v0_3 dataPart = new DataPart_v0_3(Map.of("temperature", 22.5, "humidity", 65));

        Artifact_v0_3 artifact = new Artifact_v0_3.Builder()
                .artifactId("data-artifact")
                .parts(List.of(dataPart))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains data part
        assertTrue(json.contains("\"kind\":\"data\""));
        assertTrue(json.contains("temperature"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify data part is preserved
        Part_v0_3<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof DataPart_v0_3);
        DataPart_v0_3 deserializedDataPart = (DataPart_v0_3) part;
        assertNotNull(deserializedDataPart.data());
    }

    @Test
    void testTaskRoundTrip() throws JsonProcessingException_v0_3 {
        // Create a comprehensive task with all part types
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task_v0_3 original = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-789")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING, null, timestamp))
                .history(List.of(
                        new Message_v0_3.Builder()
                                .role(Message_v0_3.Role.USER)
                                .parts(List.of(
                                        new TextPart_v0_3("Text"),
                                        new FilePart_v0_3(new FileWithBytes_v0_3("text/plain", "file.txt", "data")),
                                        new DataPart_v0_3(Map.of("key", "value"))
                                ))
                                .build()
                ))
                .artifacts(List.of(
                        new Artifact_v0_3.Builder()
                                .artifactId("artifact-1")
                                .parts(List.of(new TextPart_v0_3("Content")))
                                .build()
                ))
                .metadata(Map.of("meta1", "value1"))
                .build();

        // Serialize to JSON
        String json = JsonUtil_v0_3.toJson(original);

        // Deserialize back to Task
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Serialize again
        String json2 = JsonUtil_v0_3.toJson(deserialized);

        // Deserialize again
        Task_v0_3 deserialized2 = JsonUtil_v0_3.fromJson(json2, Task_v0_3.class);

        // Verify multiple round-trips produce identical results
        assertEquals(deserialized.id(), deserialized2.id());
        assertEquals(deserialized.contextId(), deserialized2.contextId());
        assertEquals(deserialized.status().state(), deserialized2.status().state());
        assertEquals(deserialized.history().size(), deserialized2.history().size());
        assertEquals(deserialized.artifacts().size(), deserialized2.artifacts().size());
    }

    @Test
    void testTaskStatusWithMessage() throws JsonProcessingException_v0_3 {
        Message_v0_3 statusMessage = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.AGENT)
                .parts(List.of(new TextPart_v0_3("Processing complete")))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED, statusMessage, null))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Verify JSON contains status message
        assertTrue(json.contains("\"state\":\"completed\""));
        assertTrue(json.contains("Processing complete"));

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify status message is preserved
        assertEquals(TaskState_v0_3.COMPLETED, deserialized.status().state());
        assertNotNull(deserialized.status().message());
        assertEquals(Message_v0_3.Role.AGENT, deserialized.status().message().role());
        assertTrue(deserialized.status().message().parts().get(0) instanceof TextPart_v0_3);
    }

    @Test
    void testDeserializeTaskFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "submitted"
              },
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState_v0_3.SUBMITTED, task.status().state());
        assertNull(task.status().message());
        // TaskStatus automatically sets timestamp to current time if not provided
        assertNotNull(task.status().timestamp());
    }

    @Test
    void testDeserializeTaskWithArtifactsFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "completed"
              },
              "artifacts": [
                {
                  "artifactId": "artifact-1",
                  "name": "Result",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Hello World"
                    }
                  ]
                }
              ],
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertEquals(TaskState_v0_3.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        assertEquals("artifact-1", task.artifacts().get(0).artifactId());
        assertEquals("Result", task.artifacts().get(0).name());
        assertEquals(1, task.artifacts().get(0).parts().size());
        assertTrue(task.artifacts().get(0).parts().get(0) instanceof TextPart_v0_3);
        assertEquals("Hello World", ((TextPart_v0_3) task.artifacts().get(0).parts().get(0)).text());
    }

    @Test
    void testDeserializeTaskWithFilePartBytesFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "completed"
              },
              "artifacts": [
                {
                  "artifactId": "file-artifact",
                  "parts": [
                    {
                      "kind": "file",
                      "file": {
                        "mimeType": "application/pdf",
                        "name": "document.pdf",
                        "bytes": "base64encodeddata"
                      }
                    }
                  ]
                }
              ],
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertEquals(1, task.artifacts().size());
        Part_v0_3<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart_v0_3);
        FilePart_v0_3 filePart = (FilePart_v0_3) part;
        assertTrue(filePart.file() instanceof FileWithBytes_v0_3);
        FileWithBytes_v0_3 fileWithBytes = (FileWithBytes_v0_3) filePart.file();
        assertEquals("application/pdf", fileWithBytes.mimeType());
        assertEquals("document.pdf", fileWithBytes.name());
        assertEquals("base64encodeddata", fileWithBytes.bytes());
    }

    @Test
    void testDeserializeTaskWithFilePartUriFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "completed"
              },
              "artifacts": [
                {
                  "artifactId": "uri-artifact",
                  "parts": [
                    {
                      "kind": "file",
                      "file": {
                        "mimeType": "image/png",
                        "name": "photo.png",
                        "uri": "https://example.com/photo.png"
                      }
                    }
                  ]
                }
              ],
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        Part_v0_3<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart_v0_3);
        FilePart_v0_3 filePart = (FilePart_v0_3) part;
        assertTrue(filePart.file() instanceof FileWithUri_v0_3);
        FileWithUri_v0_3 fileWithUri = (FileWithUri_v0_3) filePart.file();
        assertEquals("image/png", fileWithUri.mimeType());
        assertEquals("photo.png", fileWithUri.name());
        assertEquals("https://example.com/photo.png", fileWithUri.uri());
    }

    @Test
    void testDeserializeTaskWithDataPartFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "completed"
              },
              "artifacts": [
                {
                  "artifactId": "data-artifact",
                  "parts": [
                    {
                      "kind": "data",
                      "data": {
                        "temperature": 22.5,
                        "humidity": 65
                      }
                    }
                  ]
                }
              ],
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        Part_v0_3<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof DataPart_v0_3);
        DataPart_v0_3 dataPart = (DataPart_v0_3) part;
        assertNotNull(dataPart.data());
    }

    @Test
    void testDeserializeTaskWithHistoryFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "working"
              },
              "history": [
                {
                  "messageId": "msg-001",
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "User message"
                    }
                  ]
                },
                {
                  "messageId": "msg-002",
                  "role": "agent",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Agent response"
                    }
                  ]
                }
              ],
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertEquals(2, task.history().size());
        assertEquals(Message_v0_3.Role.USER, task.history().get(0).role());
        assertEquals(Message_v0_3.Role.AGENT, task.history().get(1).role());
        assertTrue(task.history().get(0).parts().get(0) instanceof TextPart_v0_3);
        assertEquals("User message", ((TextPart_v0_3) task.history().get(0).parts().get(0)).text());
    }

    @Test
    void testDeserializeTaskWithTimestampFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "working",
                "timestamp": "2023-10-01T12:00:00.234-05:00"
              },
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertEquals(TaskState_v0_3.WORKING, task.status().state());
        assertNotNull(task.status().timestamp());
        assertEquals("2023-10-01T12:00:00.234-05:00", task.status().timestamp().toString());
    }

    @Test
    void testDeserializeTaskWithMetadataFromJson() throws JsonProcessingException_v0_3 {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "completed"
              },
              "metadata": {
                "key1": "value1",
                "key2": 42
              },
              "kind": "task"
            }
            """;

        Task_v0_3 task = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        assertEquals("task-123", task.id());
        assertNotNull(task.metadata());
        assertEquals("value1", task.metadata().get("key1"));
    }

    @Test
    void testTaskWithMixedPartTypes() throws JsonProcessingException_v0_3 {
        Artifact_v0_3 artifact = new Artifact_v0_3.Builder()
                .artifactId("mixed-artifact")
                .parts(List.of(
                        new TextPart_v0_3("Text content"),
                        new FilePart_v0_3(new FileWithBytes_v0_3("application/json", "data.json", "{}")),
                        new DataPart_v0_3(Map.of("result", 42)),
                        new FilePart_v0_3(new FileWithUri_v0_3("image/png", "image.png", "https://example.com/img.png"))
                ))
                .build();

        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil_v0_3.toJson(task);

        // Deserialize
        Task_v0_3 deserialized = JsonUtil_v0_3.fromJson(json, Task_v0_3.class);

        // Verify all part types are preserved
        List<Part_v0_3<?>> parts = deserialized.artifacts().get(0).parts();
        assertEquals(4, parts.size());
        assertTrue(parts.get(0) instanceof TextPart_v0_3);
        assertTrue(parts.get(1) instanceof FilePart_v0_3);
        assertTrue(parts.get(2) instanceof DataPart_v0_3);
        assertTrue(parts.get(3) instanceof FilePart_v0_3);
    }
}
