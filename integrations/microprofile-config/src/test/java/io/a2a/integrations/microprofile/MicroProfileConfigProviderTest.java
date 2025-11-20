package io.a2a.integrations.microprofile;

import io.a2a.server.config.A2AConfigProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CDI-based test to verify that MicroProfileConfigProvider is properly selected
 * and works correctly with MicroProfile Config and fallback to defaults.
 */
@QuarkusTest
public class MicroProfileConfigProviderTest {

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
        String value = configProvider.getValue("a2a.executor.core-pool-size");
        assertEquals("15", value, "Should get value from MicroProfile Config (application.properties)");
    }

    @Test
    public void testGetValueFallbackToDefaults() {
        // Test that values not in application.properties fall back to META-INF/a2a-defaults.properties
        // a2a.executor.max-pool-size is not in test application.properties, so should use default
        String value = configProvider.getValue("a2a.executor.max-pool-size");
        assertEquals("50", value, "Should fall back to default value from META-INF/a2a-defaults.properties");
    }

    @Test
    public void testGetValueAnotherDefault() {
        // Test another default property to ensure fallback works
        String value = configProvider.getValue("a2a.executor.keep-alive-seconds");
        assertEquals("60", value, "Should fall back to default value");
    }

    @Test
    public void testGetOptionalValueFromMicroProfileConfig() {
        // Test optional value that exists in application.properties
        Optional<String> value = configProvider.getOptionalValue("a2a.executor.core-pool-size");
        assertTrue(value.isPresent(), "Optional value should be present");
        assertEquals("15", value.get(), "Should get overridden value from MicroProfile Config");
    }

    @Test
    public void testGetOptionalValueFallbackToDefaults() {
        // Test optional value that falls back to defaults
        Optional<String> value = configProvider.getOptionalValue("a2a.executor.max-pool-size");
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
