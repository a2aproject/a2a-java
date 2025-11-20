package io.a2a.server.config;

import java.util.Optional;

/**
 * Configuration provider interface for A2A SDK configuration values.
 * <p>
 * Implementations can obtain configuration from various sources:
 * <ul>
 *   <li>{@link DefaultValuesConfigProvider} - Loads from META-INF/a2a-defaults.properties on classpath</li>
 *   <li>MicroProfileConfigProvider - Delegates to MicroProfile Config (reference implementations)</li>
 *   <li>Custom implementations - Can integrate with any configuration system</li>
 * </ul>
 * <p>
 * All configuration values are returned as strings. Consumers are responsible for type conversion.
 */
public interface A2AConfigProvider {

    /**
     * Get a required configuration value.
     *
     * @param name the configuration property name
     * @return the configuration value
     * @throws IllegalArgumentException if the configuration value is not found
     */
    String getValue(String name);

    /**
     * Get an optional configuration value.
     *
     * @param name the configuration property name
     * @return an Optional containing the value if present, empty otherwise
     */
    Optional<String> getOptionalValue(String name);
}
