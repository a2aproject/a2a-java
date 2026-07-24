package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentCapabilitiesTest {

    @Test
    void testConstruction_defensivelyCopiesExtensions() {
        List<AgentExtension> extensions = new ArrayList<>();
        extensions.add(new AgentExtension("Custom auth", Map.of("authType", "bearer"), true, "https://example.com/ext"));

        AgentCapabilities capabilities = new AgentCapabilities(true, false, true, extensions);
        extensions.add(new AgentExtension("Another", Map.of("x", "y"), false, "https://example.com/ext2"));

        assertEquals(1, capabilities.extensions().size());
        assertThrows(UnsupportedOperationException.class, () -> capabilities.extensions().add(
                new AgentExtension("Mutated", Map.of(), false, "https://example.com/ext3")));
    }
}
