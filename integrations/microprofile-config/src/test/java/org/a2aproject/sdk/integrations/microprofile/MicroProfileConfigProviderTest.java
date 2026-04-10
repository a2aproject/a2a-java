package org.a2aproject.sdk.integrations.microprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import jakarta.inject.Inject;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * CDI-based test to verify that MicroProfileConfigProvider is properly selected
 * and works correctly with MicroProfile Config and fallback to defaults.
 */
@QuarkusTest
public class MicroProfileConfigProviderTest {

    private static final String A2A_EXECUTOR_CORE_POOL_SIZE = "a2a.executor.core-pool-size";
    private static final String A2A_EXECUTOR_MAX_POOL_SIZE = "a2a.executor.max-pool-size";
    private static final String A2A_EXECUTOR_KEEP_ALIVE_SECONDS = "a2a.executor.keep-alive-seconds";

    @Inject
    A2AConfigProvider configProvider;

    @Test
    public void testIsMicroProfileConfigProvider() {
        // Verify that when microprofile-config module is on classpath,
        // the injected A2AConfigProvider is the MicroProfile implementation
        assertInstanceOf(MicroProfileConfigProvider.class, configProvider,
                "A2AConfigProvider should be MicroProfileConfigProvider when module is present");
    }

    @Test
    public void testGetValueFromMicroProfileConfig() {
        // Test that values from application.properties override defaults
        // The test application.properties sets a2a.executor.core-pool-size=15
        String value = configProvider.getValue(A2A_EXECUTOR_CORE_POOL_SIZE);
        assertEquals("15", value, "Should get value from MicroProfile Config (application.properties)");
    }

    @Test
    public void testGetValueFallbackToDefaults() {
        // Test that values not in application.properties fall back to META-INF/a2a-defaults.properties
        // a2a.executor.max-pool-size is not in test application.properties, so should use default
        String value = configProvider.getValue(A2A_EXECUTOR_MAX_POOL_SIZE);
        assertEquals("50", value, "Should fall back to default value from META-INF/a2a-defaults.properties");
    }

    @Test
    public void testGetValueAnotherDefault() {
        // Test another default property to ensure fallback works
        String value = configProvider.getValue(A2A_EXECUTOR_KEEP_ALIVE_SECONDS);
        assertEquals("60", value, "Should fall back to default value");
    }

    @Test
    public void testGetOptionalValueFromMicroProfileConfig() {
        // Test optional value that exists in application.properties
        Optional<String> value = configProvider.getOptionalValue(A2A_EXECUTOR_CORE_POOL_SIZE);
        assertTrue(value.isPresent(), "Optional value should be present");
        assertEquals("15", value.get(), "Should get overridden value from MicroProfile Config");
    }

    @Test
    public void testGetOptionalValueFallbackToDefaults() {
        // Test optional value that falls back to defaults
        Optional<String> value = configProvider.getOptionalValue(A2A_EXECUTOR_MAX_POOL_SIZE);
        assertTrue(value.isPresent(), "Optional value should be present from defaults");
        assertEquals("50", value.get(), "Should get default value");
    }

    @Test
    public void testGetOptionalValueNotFound() {
        // Test optional value that doesn't exist anywhere
        Optional<String> value = configProvider.getOptionalValue("non.existent.property");
        assertFalse(value.isPresent(), "Optional value should be empty for non-existent property");
    }

    @Test
    public void testGetValueThrowsForNonExistent() {
        // Test that required getValue() throws for non-existent property
        assertThrows(IllegalArgumentException.class,
                () -> configProvider.getValue("non.existent.property"),
                "Should throw IllegalArgumentException for non-existent required property");
    }

    @Test
    public void testSystemPropertyOverride() {
        // System properties should have higher priority than application.properties
        // Set a system property and verify it's used
        String originalValue = System.getProperty("a2a.test.system.property");
        try {
            System.setProperty("a2a.test.system.property", "from-system-property");
            String value = configProvider.getValue("a2a.test.system.property");
            assertEquals("from-system-property", value,
                    "System property should override application.properties");
        } finally {
            if (originalValue != null) {
                System.setProperty("a2a.test.system.property", originalValue);
            } else {
                System.clearProperty("a2a.test.system.property");
            }
        }
    }
}
