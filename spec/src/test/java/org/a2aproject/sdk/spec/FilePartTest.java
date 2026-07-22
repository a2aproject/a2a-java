package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FilePartTest {

    @Test
    void testConstructor_withNullMetadataValuePreservesValueAndIsImmutable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", null);

        FilePart part = new FilePart(new FileWithUri("text/plain", "example.txt", "https://example.com/example.txt"),
                metadata);

        assertEquals("example.txt", part.file().name());
        assertTrue(part.metadata().containsKey("source"));
        assertNull(part.metadata().get("source"));
        assertThrows(UnsupportedOperationException.class, () -> part.metadata().put("another", "value"));
    }
}
