package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AuthorizationCodeOAuthFlowTest {

    @Test
    void testConstruction_defensivelyCopiesScopes() {
        Map<String, String> scopes = new HashMap<>();
        scopes.put("read", "Read access");

        AuthorizationCodeOAuthFlow flow = new AuthorizationCodeOAuthFlow(
                "https://auth.example.com/authorize",
                "https://auth.example.com/refresh",
                scopes,
                "https://auth.example.com/token",
                true);

        scopes.put("write", "Write access");

        assertEquals(Map.of("read", "Read access"), flow.scopes());
        assertThrows(UnsupportedOperationException.class, () -> flow.scopes().put("x", "y"));
    }
}
