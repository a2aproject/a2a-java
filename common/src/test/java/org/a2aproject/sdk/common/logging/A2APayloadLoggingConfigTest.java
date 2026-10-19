package org.a2aproject.sdk.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class A2APayloadLoggingConfigTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(A2APayloadLoggingConfig.ENABLED_PROPERTY);
        System.clearProperty(A2APayloadLoggingConfig.MAX_SIZE_PROPERTY);
        System.clearProperty(A2APayloadLoggingConfig.MASKING_ENABLED_PROPERTY);
    }

    @Test
    void testDefaultConfig() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder().build();
        assertFalse(config.isEnabled());
        assertEquals(4096, config.getMaxSize());
        assertTrue(config.isMaskingEnabled());
    }

    @Test
    void testBuilder() {
        A2APayloadLoggingConfig config = A2APayloadLoggingConfig.builder()
                .enabled(true)
                .maxSize(8192)
                .maskingEnabled(false)
                .build();
        assertTrue(config.isEnabled());
        assertEquals(8192, config.getMaxSize());
        assertFalse(config.isMaskingEnabled());
    }

    @Test
    void testFromSystemPropertiesEnabled() {
        System.setProperty(A2APayloadLoggingConfig.ENABLED_PROPERTY, "true");
        A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.fromSystemProperties();
        assertTrue(cfg.isEnabled());
    }

    @Test
    void testFromSystemPropertiesMaxSize() {
        System.setProperty(A2APayloadLoggingConfig.MAX_SIZE_PROPERTY, "2048");
        A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.fromSystemProperties();
        assertEquals(2048, cfg.getMaxSize());
    }

    @Test
    void testFromSystemPropertiesMaskingDisabled() {
        System.setProperty(A2APayloadLoggingConfig.MASKING_ENABLED_PROPERTY, "false");
        A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.fromSystemProperties();
        assertFalse(cfg.isMaskingEnabled());
    }

    @Test
    void testFromSystemPropertiesInvalidNumberUsesDefault() {
        System.setProperty(A2APayloadLoggingConfig.MAX_SIZE_PROPERTY, "invalid");
        A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.fromSystemProperties();
        assertEquals(4096, cfg.getMaxSize());
    }

    @Test
    void testToBuilder() {
        A2APayloadLoggingConfig original = A2APayloadLoggingConfig.builder().enabled(true).maxSize(1234).build();
        A2APayloadLoggingConfig copy = original.toBuilder().maxSize(5678).build();
        assertTrue(copy.isEnabled());
        assertEquals(5678, copy.getMaxSize());
        assertEquals(1234, original.getMaxSize());
    }
}
