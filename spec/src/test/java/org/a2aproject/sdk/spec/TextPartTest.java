package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TextPartTest {

    @Test
    void testConstructor_withNullMetadataValuePreservesValueAndIsImmutable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("language", null);

        TextPart part = new TextPart("hello", metadata);

        assertEquals("hello", part.text());
        assertTrue(part.metadata().containsKey("language"));
        assertNull(part.metadata().get("language"));
        assertThrows(UnsupportedOperationException.class, () -> part.metadata().put("another", "value"));
    }
}
