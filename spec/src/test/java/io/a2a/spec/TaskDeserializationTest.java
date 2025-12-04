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

        assertNotNull(task.history(), "history should not be null");
        assertNotNull(task.artifacts(), "artifacts should not be null");

        assertTrue(task.history().isEmpty(), "history should be empty list when not provided");
        assertTrue(task.artifacts().isEmpty(), "artifacts should be empty list when not provided");
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
        assertNotNull(task.history(), "history should not be null even when JSON contains null");
        assertNotNull(task.artifacts(), "artifacts should not be null even when JSON contains null");

        assertTrue(task.history().isEmpty());
        assertTrue(task.artifacts().isEmpty());
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

        assertNotNull(task.history());
        assertEquals(1, task.history().size());

        assertNotNull(task.artifacts());
        assertTrue(task.artifacts().isEmpty());
    }
}
