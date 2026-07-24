package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentExtensionTest {

    @Test
    void testConstruction_defensivelyCopiesParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("authType", "bearer");

        AgentExtension extension = new AgentExtension("Custom auth", params, true, "https://example.com/ext");
        params.put("mutated", true);

        assertEquals(Map.of("authType", "bearer"), extension.params());
        assertThrows(UnsupportedOperationException.class, () -> extension.params().put("x", "y"));
    }

    @Test
    void testConstruction_preservesNullParamsValues() {
        Map<String, Object> params = new HashMap<>();
        params.put("foo", null);

        AgentExtension extension = new AgentExtension("Custom auth", params, true, "https://example.com/ext");

        assertEquals(1, extension.params().size());
        assertTrue(extension.params().containsKey("foo"));
        assertNull(extension.params().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> extension.params().put("bar", "baz"));
    }
}
