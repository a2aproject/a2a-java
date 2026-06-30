package org.a2aproject.sdk.integrations.springboot.server.autoconfigure;

import java.util.Optional;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * {@link A2AConfigProvider} backed by the Spring {@link Environment}.
 *
 * <p>Spring property sources win first. If a key is absent there, this provider falls back to
 * the default values bundled with the SDK via {@link DefaultValuesConfigProvider}. That keeps the
 * runtime behavior identical to the core server while still letting Spring Boot override values
 * from {@code application.yml}, environment variables, command-line args, or any other Spring
 * property source.
 */
final class SpringEnvironmentA2AConfigProvider implements A2AConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringEnvironmentA2AConfigProvider.class);

    private final Environment environment;
    private final DefaultValuesConfigProvider defaultValuesConfigProvider;

    SpringEnvironmentA2AConfigProvider(Environment environment, DefaultValuesConfigProvider defaultValuesConfigProvider) {
        this.environment = environment;
        this.defaultValuesConfigProvider = defaultValuesConfigProvider;
    }

    @Override
    public String getValue(String name) {
        String value = environment.getProperty(name);
        if (value != null) {
            LOGGER.trace("Config value '{}' = '{}' (from Spring Environment)", name, value);
            return value;
        }

        String defaultValue = defaultValuesConfigProvider.getValue(name);
        LOGGER.trace("Config value '{}' = '{}' (from DefaultValuesConfigProvider)", name, defaultValue);
        return defaultValue;
    }

    @Override
    public Optional<String> getOptionalValue(String name) {
        String value = environment.getProperty(name);
        if (value != null) {
            LOGGER.trace("Optional config value '{}' = '{}' (from Spring Environment)", name, value);
            return Optional.of(value);
        }

        Optional<String> defaultValue = defaultValuesConfigProvider.getOptionalValue(name);
        LOGGER.trace("Optional config value '{}' = '{}' (from DefaultValuesConfigProvider)",
                name, defaultValue.orElse("<absent>"));
        return defaultValue;
    }
}
