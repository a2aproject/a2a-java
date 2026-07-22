package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CancelTaskParamsTest {

    @Test
    void testConstruction_defensivelyCopiesMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", "timeout");

        CancelTaskParams params = new CancelTaskParams("task-1", "tenant-1", metadata);
        metadata.put("mutated", true);

        assertEquals(Map.of("reason", "timeout"), params.metadata());
        assertThrows(UnsupportedOperationException.class, () -> params.metadata().put("x", "y"));
    }

    @Test
    void testBuilderPreservesNullMetadataValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", null);

        CancelTaskParams params = CancelTaskParams.builder()
                .id("task-1")
                .tenant("tenant-1")
                .metadata(metadata)
                .build();

        assertEquals(1, params.metadata().size());
        assertTrue(params.metadata().containsKey("foo"));
        assertNull(params.metadata().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> params.metadata().put("bar", "baz"));
    }
}
