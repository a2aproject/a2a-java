package io.a2a.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TaskDeserializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testTaskWithMissingHistoryAndArtifacts() throws Exception {
        // JSON without history and artifacts fields (common server response)
        String json = """
            {
                "id": "task-123",
                "contextId": "context-456",
                "status": {
                    "state": "completed"
                },
                "kind": "task"
            }
            """;

        Task task = objectMapper.readValue(json, Task.class);

        assertNotNull(task.getHistory(), "history should not be null");
        assertNotNull(task.getArtifacts(), "artifacts should not be null");

        assertTrue(task.getHistory().isEmpty(), "history should be empty list when not provided");
        assertTrue(task.getArtifacts().isEmpty(), "artifacts should be empty list when not provided");
    }

    @Test
    void testTaskWithExplicitNullValues() throws Exception {
        // JSON with explicit null values
        String json = """
            {
                "id": "task-123",
                "contextId": "context-456",
                "status": {
                    "state": "completed"
                },
                "history": null,
                "artifacts": null,
                "kind": "task"
            }
            """;

        Task task = objectMapper.readValue(json, Task.class);

        // Should never be null even with explicit null in JSON
        assertNotNull(task.getHistory(), "history should not be null even when JSON contains null");
        assertNotNull(task.getArtifacts(), "artifacts should not be null even when JSON contains null");

        assertTrue(task.getHistory().isEmpty());
        assertTrue(task.getArtifacts().isEmpty());
    }

    @Test
    void testTaskWithPopulatedArrays() throws Exception {
        String json = """
            {
                "id": "task-123",
                "contextId": "context-456",
                "status": {
                    "state": "completed"
                },
                "history": [
                    {
                        "role": "user",
                        "parts": [{"kind": "text", "text": "hello"}],
                        "messageId": "msg-1",
                        "kind": "message"
                    }
                ],
                "artifacts": [],
                "kind": "task"
            }
            """;

        Task task = objectMapper.readValue(json, Task.class);

        assertNotNull(task.getHistory());
        assertEquals(1, task.getHistory().size());

        assertNotNull(task.getArtifacts());
        assertTrue(task.getArtifacts().isEmpty());
    }
}
