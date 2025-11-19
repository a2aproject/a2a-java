package io.a2a.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default configuration provider that loads values from {@code META-INF/a2a-defaults.properties}
 * files on the classpath.
 * <p>
 * Each module (server-common, extras, etc.) can contribute a {@code META-INF/a2a-defaults.properties}
 * file with default configuration values. All files are discovered and merged at startup.
 * <p>
 * If duplicate keys are found across different properties files, initialization will fail with
 * an exception to prevent ambiguous configuration.
 */
@ApplicationScoped
public class DefaultValuesConfigProvider implements A2AConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultValuesConfigProvider.class);
    private static final String DEFAULTS_RESOURCE = "META-INF/a2a-defaults.properties";

    private final Map<String, String> defaults = new HashMap<>();

    @PostConstruct
    void init() {
        loadDefaultsFromClasspath();
    }

    private void loadDefaultsFromClasspath() {
        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources(DEFAULTS_RESOURCE);

            Map<String, String> sourceTracker = new HashMap<>(); // Track which file each key came from

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                LOGGER.debug("Loading A2A defaults from: {}", url);

                Properties props = new Properties();
                try (InputStream is = url.openStream()) {
                    props.load(is);

                    // Check for duplicates and merge
                    for (String key : props.stringPropertyNames()) {
                        String value = props.getProperty(key);
                        String existingSource = sourceTracker.get(key);

                        if (existingSource != null) {
                            throw new IllegalStateException(String.format(
                                    "Duplicate configuration key '%s' found in multiple a2a-defaults.properties files: %s and %s",
                                    key, existingSource, url));
                        }

                        defaults.put(key, value);
                        sourceTracker.put(key, url.toString());
                        LOGGER.trace("Loaded default: {} = {}", key, value);
                    }
                }
            }

            LOGGER.info("Loaded {} A2A default configuration values from {} resource(s)",
                    defaults.size(), sourceTracker.values().stream().distinct().count());

        } catch (IOException e) {
            throw new RuntimeException("Failed to load A2A default configuration from classpath", e);
        }
    }

    @Override
    public String getValue(String name) {
        String value = defaults.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No default configuration value found for: " + name);
        }
        return value;
    }

    @Override
    public Optional<String> getOptionalValue(String name) {
        return Optional.ofNullable(defaults.get(name));
    }
}
