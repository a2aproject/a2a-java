package io.a2a.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DeviceCodeOAuthFlow}.
 * <p>
 * Tests cover construction with valid parameters, null validation of required
 * fields, and handling of the optional {@code refreshUrl} field.
 *
 * @see DeviceCodeOAuthFlow
 */
class DeviceCodeOAuthFlowTest {

    private static final String DEVICE_AUTH_URL = "https://auth.example.com/device/code";
    private static final String TOKEN_URL = "https://auth.example.com/token";
    private static final String REFRESH_URL = "https://auth.example.com/refresh";
    private static final Map<String, String> SCOPES = Map.of("read", "Read access", "write", "Write access");

    @Test
    void testConstruction_withAllFields() {
        DeviceCodeOAuthFlow flow = new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, REFRESH_URL, SCOPES);

        assertEquals(DEVICE_AUTH_URL, flow.deviceAuthorizationUrl());
        assertEquals(TOKEN_URL, flow.tokenUrl());
        assertEquals(REFRESH_URL, flow.refreshUrl());
        assertEquals(SCOPES, flow.scopes());
    }

    @Test
    void testConstruction_withNullRefreshUrl() {
        DeviceCodeOAuthFlow flow = new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, null, SCOPES);

        assertEquals(DEVICE_AUTH_URL, flow.deviceAuthorizationUrl());
        assertEquals(TOKEN_URL, flow.tokenUrl());
        assertNull(flow.refreshUrl());
        assertEquals(SCOPES, flow.scopes());
    }

    @Test
    void testConstruction_withEmptyScopes() {
        DeviceCodeOAuthFlow flow = new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, null, Map.of());

        assertNotNull(flow.scopes());
        assertEquals(0, flow.scopes().size());
    }

    @Test
    void testConstruction_nullDeviceAuthorizationUrl_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceCodeOAuthFlow(null, TOKEN_URL, REFRESH_URL, SCOPES));
    }

    @Test
    void testConstruction_nullTokenUrl_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, null, REFRESH_URL, SCOPES));
    }

    @Test
    void testConstruction_nullScopes_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, REFRESH_URL, null));
    }

    @Test
    void testEquality_sameValues() {
        DeviceCodeOAuthFlow flow1 = new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, REFRESH_URL, SCOPES);
        DeviceCodeOAuthFlow flow2 = new DeviceCodeOAuthFlow(DEVICE_AUTH_URL, TOKEN_URL, REFRESH_URL, SCOPES);

        assertEquals(flow1, flow2);
        assertEquals(flow1.hashCode(), flow2.hashCode());
    }
}
