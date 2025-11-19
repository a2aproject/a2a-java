package io.a2a.integrations.microprofile;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;

import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MicroProfile Config-based implementation of {@link A2AConfigProvider}.
 * <p>
 * This provider integrates with MicroProfile Config (used by Quarkus and other Jakarta EE runtimes)
 * to allow configuration via standard sources:
 * <ul>
 *   <li>System properties (-D flags)</li>
 *   <li>Environment variables</li>
 *   <li>application.properties</li>
 *   <li>Custom ConfigSources</li>
 * </ul>
 * <p>
 * Falls back to {@link DefaultValuesConfigProvider} when a configuration value is not found
 * in MicroProfile Config, ensuring that default values from {@code META-INF/a2a-defaults.properties}
 * are always available.
 * <p>
 * This provider is automatically enabled with {@code @Priority(50)}, but can be overridden by
 * custom providers with higher priority.
 * <p>
 * To use this provider, add the {@code a2a-java-sdk-microprofile-config} dependency to your project.
 */
@ApplicationScoped
@Alternative
@Priority(50)
public class MicroProfileConfigProvider implements A2AConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileConfigProvider.class);

    @Inject
    Config mpConfig;

    @Inject
    DefaultValuesConfigProvider defaultValues;

    @Override
    public String getValue(String name) {
        Optional<String> value = mpConfig.getOptionalValue(name, String.class);
        if (value.isPresent()) {
            LOGGER.trace("Config value '{}' = '{}' (from MicroProfile Config)", name, value.get());
            return value.get();
        }

        // Fallback to defaults
        String defaultValue = defaultValues.getValue(name);
        LOGGER.trace("Config value '{}' = '{}' (from DefaultValuesConfigProvider)", name, defaultValue);
        return defaultValue;
    }

    @Override
    public Optional<String> getOptionalValue(String name) {
        Optional<String> value = mpConfig.getOptionalValue(name, String.class);
        if (value.isPresent()) {
            LOGGER.trace("Optional config value '{}' = '{}' (from MicroProfile Config)", name, value.get());
            return value;
        }

        // Fallback to defaults
        Optional<String> defaultValue = defaultValues.getOptionalValue(name);
        LOGGER.trace("Optional config value '{}' = '{}' (from DefaultValuesConfigProvider)",
                name, defaultValue.orElse("<absent>"));
        return defaultValue;
    }
}
