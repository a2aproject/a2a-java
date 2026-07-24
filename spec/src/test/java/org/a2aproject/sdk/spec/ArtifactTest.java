package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ArtifactTest {

    @Test
    void testConstruction_defensivelyCopiesCollections() {
        List<Part<?>> parts = new ArrayList<>();
        parts.add(new TextPart("hello"));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "web");
        List<String> extensions = new ArrayList<>();
        extensions.add("ext-1");

        Artifact artifact = new Artifact("artifact-1", "name", "desc", parts, metadata, extensions);
        parts.add(new TextPart("mutated"));
        metadata.put("write", "should-not-leak");
        extensions.add("ext-2");

        assertEquals(1, artifact.parts().size());
        assertEquals(Map.of("source", "web"), artifact.metadata());
        assertEquals(List.of("ext-1"), artifact.extensions());
        assertThrows(UnsupportedOperationException.class, () -> artifact.parts().add(new TextPart("x")));
    }

    @Test
    void testConstruction_preservesNullMetadataValues() {
        List<Part<?>> parts = List.of(new TextPart("hello"));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", null);

        Artifact artifact = new Artifact("artifact-2", null, null, parts, metadata, null);

        assertEquals(1, artifact.metadata().size());
        assertTrue(artifact.metadata().containsKey("foo"));
        assertNull(artifact.metadata().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> artifact.metadata().put("bar", "baz"));
    }
}
