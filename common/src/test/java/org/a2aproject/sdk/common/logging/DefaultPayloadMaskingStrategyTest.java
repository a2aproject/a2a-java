package org.a2aproject.sdk.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class DefaultPayloadMaskingStrategyTest {

    @Test
    public void testMaskSensitiveFields() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"token\":\"secret123\",\"user\":\"john\",\"credentials\":\"mycreds\"}";
        String masked = strategy.mask(json);
        assertTrue(masked.contains("\"token\":\"***\""));
        assertTrue(masked.contains("\"credentials\":\"***\""));
        assertFalse(masked.contains("secret123"));
        assertFalse(masked.contains("mycreds"));
        assertTrue(masked.contains("\"user\":\"john\""));
    }

    @Test
    public void testMaskNestedObject() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"config\":{\"url\":\"https://example.com\",\"token\":\"abc\"},\"data\":\"value\"}";
        String masked = strategy.mask(json);
        assertTrue(masked.contains("\"token\":\"***\""));
        assertTrue(masked.contains("\"url\":\"***\""));
        assertFalse(masked.contains("abc"));
        assertFalse(masked.contains("https://example.com"));
        assertTrue(masked.contains("\"data\":\"value\""));
    }

    @Test
    public void testMaskAuthenticationObject() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"authentication\":{\"scheme\":\"bearer\",\"credentials\":\"supersecret\"}}";
        String masked = strategy.mask(json);
        // authentication key is sensitive, so whole object should be masked
        assertTrue(masked.contains("\"authentication\":\"***\""));
        assertFalse(masked.contains("supersecret"));
    }

    @Test
    public void testMaskArray() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"items\":[{\"token\":\"t1\"},{\"token\":\"t2\"}]}";
        String masked = strategy.mask(json);
        assertTrue(masked.contains("\"token\":\"***\""));
        assertFalse(masked.contains("t1"));
        assertFalse(masked.contains("t2"));
    }

    @Test
    public void testCustomSensitiveKeys() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy(Set.of("myCustomField"));
        String json = "{\"myCustomField\":\"sensitive\",\"other\":\"value\"}";
        String masked = strategy.mask(json);
        assertTrue(masked.contains("\"myCustomField\":\"***\""));
        assertTrue(masked.contains("\"other\":\"value\""));
    }

    @Test
    public void testNoSensitiveData() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"name\":\"test\",\"value\":123}";
        String masked = strategy.mask(json);
        assertEquals(json, masked);
    }

    @Test
    public void testNullInput() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        assertEquals(null, strategy.mask(null));
    }

    @Test
    public void testBlankInput() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String blank = "   ";
        assertEquals(blank, strategy.mask(blank));
    }

    @Test
    public void testNoOpStrategy() {
        NoOpPayloadMaskingStrategy strategy = new NoOpPayloadMaskingStrategy();
        String json = "{\"token\":\"secret\"}";
        assertEquals(json, strategy.mask(json));
    }

    @Test
    public void testPushNotificationConfigMasking() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy();
        String json = "{\"taskId\":\"123\",\"url\":\"https://webhook.example.com\",\"token\":\"push-token-xyz\",\"authentication\":{\"credentials\":\"auth-creds\"}}";
        String masked = strategy.mask(json);
        assertTrue(masked.contains("\"url\":\"***\""));
        assertTrue(masked.contains("\"token\":\"***\""));
        // authentication object is sensitive, should be masked wholly
        assertTrue(masked.contains("\"authentication\":\"***\""));
        assertFalse(masked.contains("push-token-xyz"));
    }

    @Test
    public void testUrlFieldMasking() {
        DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy(Set.of("url"));
        String json = "{\"pushUrl\":\"https://example.com/hook\"}";
        String masked = strategy.mask(json);
        // contains 'url' substring, should be masked
        assertTrue(masked.contains("\"pushUrl\":\"***\""));
    }
}
