package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SecurityRequirementTest {

    @Test
    void testConstruction_defensivelyCopiesNestedScopes() {
        List<String> scopes = new ArrayList<>();
        scopes.add("read");
        Map<String, List<String>> schemes = new HashMap<>();
        schemes.put("oauth2", scopes);

        SecurityRequirement requirement = new SecurityRequirement(schemes);
        scopes.add("write");
        schemes.put("apiKey", List.of());

        assertEquals(List.of("read"), requirement.schemes().get("oauth2"));
        assertThrows(UnsupportedOperationException.class, () -> requirement.schemes().put("x", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> requirement.schemes().get("oauth2").add("x"));
    }
}
