package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentCardSignatureTest {

    @Test
    void testConstruction_defensivelyCopiesHeader() {
        Map<String, Object> header = new HashMap<>();
        header.put("kid", "2024-01");

        AgentCardSignature signature = new AgentCardSignature(header, "protected", "signature");
        header.put("alg", "ES256");

        assertEquals(Map.of("kid", "2024-01"), signature.header());
        assertThrows(UnsupportedOperationException.class, () -> signature.header().put("x", "y"));
    }

    @Test
    void testConstruction_preservesNullHeaderValues() {
        Map<String, Object> header = new HashMap<>();
        header.put("foo", null);

        AgentCardSignature signature = new AgentCardSignature(header, "protected", "signature");

        assertEquals(1, signature.header().size());
        assertTrue(signature.header().containsKey("foo"));
        assertNull(signature.header().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> signature.header().put("bar", "baz"));
    }
}
