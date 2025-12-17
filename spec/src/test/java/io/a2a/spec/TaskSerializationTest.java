package io.a2a.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import io.a2a.json.JsonProcessingException;
import io.a2a.json.JsonUtil;
import org.junit.jupiter.api.Test;

/**
 * Tests for Task serialization and deserialization using Gson.
 */
class TaskSerializationTest {

    @Test
    void testBasicTaskSerialization() throws JsonProcessingException {
        // Create a basic task
        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        // Serialize to JSON
        String json = JsonUtil.toJson(task);

        // Verify JSON contains expected fields
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"task-123\""));
        assertTrue(json.contains("\"state\":\"submitted\""));

        // Deserialize back to Task
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify deserialized task matches original
        assertEquals(task.id(), deserialized.id());
        assertEquals(task.status().state(), deserialized.status().state());
    }

    @Test
    void testTaskWithTimestamp() throws JsonProcessingException {
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.WORKING, null, timestamp))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify OffsetDateTime timestamp is preserved
        assertNotNull(deserialized.status().timestamp());
        assertEquals(task.status().timestamp(), deserialized.status().timestamp());
    }

    @Test
    void testTaskWithArtifacts() throws JsonProcessingException {
        Artifact artifact = Artifact.builder()
                .artifactId("artifact-1")
                .name("Test Artifact")
                .description("Description of artifact")
                .parts(List.of(
                        new TextPart("Hello"),
                        new TextPart("World")
                ))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains artifact data
        assertTrue(json.contains("\"artifactId\":\"artifact-1\""));
        assertTrue(json.contains("Hello"));
        assertTrue(json.contains("World"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify artifacts are preserved
        assertNotNull(deserialized.artifacts());
        assertEquals(1, deserialized.artifacts().size());
        assertEquals("artifact-1", deserialized.artifacts().get(0).artifactId());
        assertEquals(2, deserialized.artifacts().get(0).parts().size());
    }

    @Test
    void testTaskWithHistory() throws JsonProcessingException {
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Test message")))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.WORKING))
                .history(List.of(message))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains history data
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("Test message"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify history is preserved
        assertNotNull(deserialized.history());
        assertEquals(1, deserialized.history().size());
        assertEquals(Message.Role.USER, deserialized.history().get(0).role());
        assertEquals(1, deserialized.history().get(0).parts().size());
    }

    @Test
    void testTaskWithAllFields() throws JsonProcessingException {
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-789")
                .status(new TaskStatus(TaskState.WORKING, null, timestamp))
                .history(List.of(
                        Message.builder()
                                .role(Message.Role.USER)
                                .parts(List.of(new TextPart("User message")))
                                .build(),
                        Message.builder()
                                .role(Message.Role.AGENT)
                                .parts(List.of(new TextPart("Agent response")))
                                .build()
                ))
                .artifacts(List.of(
                        Artifact.builder()
                                .artifactId("artifact-1")
                                .parts(List.of(new TextPart("Artifact content")))
                                .build()
                ))
                .metadata(Map.of("key1", "value1", "key2", 42))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

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
    void testTaskWithDifferentStates() throws JsonProcessingException {
        for (TaskState state : TaskState.values()) {
            Task task = Task.builder()
                    .id("task-" + state.asString())
                    .contextId("context-123")
                    .status(new TaskStatus(state))
                    .build();

            // Serialize
            String json = JsonUtil.toJson(task);

            // Verify state is serialized correctly
            assertTrue(json.contains("\"state\":\"" + state.asString() + "\""));

            // Deserialize
            Task deserialized = JsonUtil.fromJson(json, Task.class);

            // Verify state is preserved
            assertEquals(state, deserialized.status().state());
        }
    }

    @Test
    void testTaskWithNullOptionalFields() throws JsonProcessingException {
        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.SUBMITTED))
                // artifacts, history, metadata not set
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify required fields are present
        assertEquals("task-123", deserialized.id());
        assertEquals("context-456", deserialized.contextId());
        assertEquals(TaskState.SUBMITTED, deserialized.status().state());

        // Verify optional lists default to empty
        assertNotNull(deserialized.artifacts());
        assertEquals(0, deserialized.artifacts().size());
        assertNotNull(deserialized.history());
        assertEquals(0, deserialized.history().size());
    }

    @Test
    void testTaskWithFilePartBytes() throws JsonProcessingException {
        FilePart filePart = new FilePart(new FileWithBytes("application/pdf", "document.pdf", "base64data"));

        Artifact artifact = Artifact.builder()
                .artifactId("file-artifact")
                .parts(List.of(filePart))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains file part data
        assertTrue(json.contains("\"kind\":\"file\""));
        assertTrue(json.contains("document.pdf"));
        assertTrue(json.contains("application/pdf"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify file part is preserved
        Part<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart);
        FilePart deserializedFilePart = (FilePart) part;
        assertTrue(deserializedFilePart.file() instanceof FileWithBytes);
        FileWithBytes fileWithBytes = (FileWithBytes) deserializedFilePart.file();
        assertEquals("document.pdf", fileWithBytes.name());
        assertEquals("application/pdf", fileWithBytes.mimeType());
    }

    @Test
    void testTaskWithFilePartUri() throws JsonProcessingException {
        FilePart filePart = new FilePart(new FileWithUri("image/png", "photo.png", "https://example.com/photo.png"));

        Artifact artifact = Artifact.builder()
                .artifactId("uri-artifact")
                .parts(List.of(filePart))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains URI
        assertTrue(json.contains("https://example.com/photo.png"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify file part URI is preserved
        Part<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart);
        FilePart deserializedFilePart = (FilePart) part;
        assertTrue(deserializedFilePart.file() instanceof FileWithUri);
        FileWithUri fileWithUri = (FileWithUri) deserializedFilePart.file();
        assertEquals("https://example.com/photo.png", fileWithUri.uri());
    }

    @Test
    void testTaskWithDataPart() throws JsonProcessingException {
        DataPart dataPart = new DataPart(Map.of("temperature", 22.5, "humidity", 65));

        Artifact artifact = Artifact.builder()
                .artifactId("data-artifact")
                .parts(List.of(dataPart))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains data part
        assertTrue(json.contains("\"kind\":\"data\""));
        assertTrue(json.contains("temperature"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify data part is preserved
        Part<?> part = deserialized.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof DataPart);
        DataPart deserializedDataPart = (DataPart) part;
        assertNotNull(deserializedDataPart.data());
    }

    @Test
    void testTaskRoundTrip() throws JsonProcessingException {
        // Create a comprehensive task with all part types
        OffsetDateTime timestamp = OffsetDateTime.now();

        Task original = Task.builder()
                .id("task-123")
                .contextId("context-789")
                .status(new TaskStatus(TaskState.WORKING, null, timestamp))
                .history(List.of(
                        Message.builder()
                                .role(Message.Role.USER)
                                .parts(List.of(
                                        new TextPart("Text"),
                                        new FilePart(new FileWithBytes("text/plain", "file.txt", "data")),
                                        new DataPart(Map.of("key", "value"))
                                ))
                                .build()
                ))
                .artifacts(List.of(
                        Artifact.builder()
                                .artifactId("artifact-1")
                                .parts(List.of(new TextPart("Content")))
                                .build()
                ))
                .metadata(Map.of("meta1", "value1"))
                .build();

        // Serialize to JSON
        String json = JsonUtil.toJson(original);

        // Deserialize back to Task
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Serialize again
        String json2 = JsonUtil.toJson(deserialized);

        // Deserialize again
        Task deserialized2 = JsonUtil.fromJson(json2, Task.class);

        // Verify multiple round-trips produce identical results
        assertEquals(deserialized.id(), deserialized2.id());
        assertEquals(deserialized.contextId(), deserialized2.contextId());
        assertEquals(deserialized.status().state(), deserialized2.status().state());
        assertEquals(deserialized.history().size(), deserialized2.history().size());
        assertEquals(deserialized.artifacts().size(), deserialized2.artifacts().size());
    }

    @Test
    void testTaskStatusWithMessage() throws JsonProcessingException {
        Message statusMessage = Message.builder()
                .role(Message.Role.AGENT)
                .parts(List.of(new TextPart("Processing complete")))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED, statusMessage, null))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Verify JSON contains status message
        assertTrue(json.contains("\"state\":\"completed\""));
        assertTrue(json.contains("Processing complete"));

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify status message is preserved
        assertEquals(TaskState.COMPLETED, deserialized.status().state());
        assertNotNull(deserialized.status().message());
        assertEquals(Message.Role.AGENT, deserialized.status().message().role());
        assertTrue(deserialized.status().message().parts().get(0) instanceof TextPart);
    }

    @Test
    void testDeserializeTaskFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState.SUBMITTED, task.status().state());
        assertNull(task.status().message());
        // TaskStatus automatically sets timestamp to current time if not provided
        assertNotNull(task.status().timestamp());
    }

    @Test
    void testDeserializeTaskWithArtifactsFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertEquals(TaskState.COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        assertEquals("artifact-1", task.artifacts().get(0).artifactId());
        assertEquals("Result", task.artifacts().get(0).name());
        assertEquals(1, task.artifacts().get(0).parts().size());
        assertTrue(task.artifacts().get(0).parts().get(0) instanceof TextPart);
        assertEquals("Hello World", ((TextPart) task.artifacts().get(0).parts().get(0)).text());
    }

    @Test
    void testDeserializeTaskWithFilePartBytesFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertEquals(1, task.artifacts().size());
        Part<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart);
        FilePart filePart = (FilePart) part;
        assertTrue(filePart.file() instanceof FileWithBytes);
        FileWithBytes fileWithBytes = (FileWithBytes) filePart.file();
        assertEquals("application/pdf", fileWithBytes.mimeType());
        assertEquals("document.pdf", fileWithBytes.name());
        assertEquals("base64encodeddata", fileWithBytes.bytes());
    }

    @Test
    void testDeserializeTaskWithFilePartUriFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        Part<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof FilePart);
        FilePart filePart = (FilePart) part;
        assertTrue(filePart.file() instanceof FileWithUri);
        FileWithUri fileWithUri = (FileWithUri) filePart.file();
        assertEquals("image/png", fileWithUri.mimeType());
        assertEquals("photo.png", fileWithUri.name());
        assertEquals("https://example.com/photo.png", fileWithUri.uri());
    }

    @Test
    void testDeserializeTaskWithDataPartFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        Part<?> part = task.artifacts().get(0).parts().get(0);
        assertTrue(part instanceof DataPart);
        DataPart dataPart = (DataPart) part;
        assertNotNull(dataPart.data());
    }

    @Test
    void testDeserializeTaskWithHistoryFromJson() throws JsonProcessingException {
        String json = """
            {
              "id": "task-123",
              "contextId": "context-456",
              "status": {
                "state": "working"
              },
              "history": [
                {
                  "role": "user",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "User message"
                    }
                  ],
                  "messageId": "msg-1"
                },
                {
                  "role": "agent",
                  "parts": [
                    {
                      "kind": "text",
                      "text": "Agent response"
                    }
                  ],
                  "messageId": "msg-2"
                }
              ],
              "kind": "task"
            }
            """;

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertEquals(2, task.history().size());
        assertEquals(Message.Role.USER, task.history().get(0).role());
        assertEquals(Message.Role.AGENT, task.history().get(1).role());
        assertTrue(task.history().get(0).parts().get(0) instanceof TextPart);
        assertEquals("User message", ((TextPart) task.history().get(0).parts().get(0)).text());
    }

    @Test
    void testDeserializeTaskWithTimestampFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertEquals(TaskState.WORKING, task.status().state());
        assertNotNull(task.status().timestamp());
        assertEquals("2023-10-01T12:00:00.234-05:00", task.status().timestamp().toString());
    }

    @Test
    void testDeserializeTaskWithMetadataFromJson() throws JsonProcessingException {
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

        Task task = JsonUtil.fromJson(json, Task.class);

        assertEquals("task-123", task.id());
        assertNotNull(task.metadata());
        assertEquals("value1", task.metadata().get("key1"));
    }

    @Test
    void testTaskWithMixedPartTypes() throws JsonProcessingException {
        Artifact artifact = Artifact.builder()
                .artifactId("mixed-artifact")
                .parts(List.of(
                        new TextPart("Text content"),
                        new FilePart(new FileWithBytes("application/json", "data.json", "{}")),
                        new DataPart(Map.of("result", 42)),
                        new FilePart(new FileWithUri("image/png", "image.png", "https://example.com/img.png"))
                ))
                .build();

        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(List.of(artifact))
                .build();

        // Serialize
        String json = JsonUtil.toJson(task);

        // Deserialize
        Task deserialized = JsonUtil.fromJson(json, Task.class);

        // Verify all part types are preserved
        List<Part<?>> parts = deserialized.artifacts().get(0).parts();
        assertEquals(4, parts.size());
        assertTrue(parts.get(0) instanceof TextPart);
        assertTrue(parts.get(1) instanceof FilePart);
        assertTrue(parts.get(2) instanceof DataPart);
        assertTrue(parts.get(3) instanceof FilePart);
    }
}
