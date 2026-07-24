package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TaskArtifactUpdateEventTest {

    @Test
    void testConstruction_defensivelyCopiesMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "web");

        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent(
                "task-1",
                new Artifact("artifact-1", null, null, List.of(new TextPart("hello")), null, null),
                "context-1",
                true,
                false,
                metadata);

        metadata.put("write", "should-not-leak");

        assertEquals(Map.of("source", "web"), event.metadata());
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("x", "y"));
    }

    @Test
    void testConstruction_preservesNullMetadataValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", null);

        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent(
                "task-1",
                new Artifact("artifact-1", null, null, List.of(new TextPart("hello")), null, null),
                "context-1",
                true,
                false,
                metadata);

        assertEquals(1, event.metadata().size());
        assertTrue(event.metadata().containsKey("foo"));
        assertNull(event.metadata().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("bar", "baz"));
    }
}
