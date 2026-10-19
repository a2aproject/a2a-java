package org.a2aproject.sdk.common.logging;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Configuration for structured payload logging.
 * <p>
 * Supports both system properties and programmatic configuration via builder.
 * </p>
 * <h2>System Properties</h2>
 * <ul>
 *   <li>{@code a2a.logging.payload.enabled} (boolean, default false) — enable payload logging</li>
 *   <li>{@code a2a.logging.payload.max-size} (int, default 4096) — max payload size before truncation</li>
 *   <li>{@code a2a.logging.payload.masking.enabled} (boolean, default true) — enable sensitive masking</li>
 *   <li>{@code a2a.logging.payload.masking.strategy} (class name) — custom masking strategy</li>
 * </ul>
 */
public final class A2APayloadLoggingConfig {

    public static final String ENABLED_PROPERTY = "a2a.logging.payload.enabled";
    public static final String MAX_SIZE_PROPERTY = "a2a.logging.payload.max-size";
    public static final String MASKING_ENABLED_PROPERTY = "a2a.logging.payload.masking.enabled";
    public static final String MASKING_STRATEGY_PROPERTY = "a2a.logging.payload.masking.strategy";
    public static final String MASKING_SENSITIVE_KEYS_PROPERTY = "a2a.logging.payload.masking.sensitive-keys";
    public static final int DEFAULT_MAX_SIZE = 4096;

    private final boolean enabled;
    private final int maxSize;
    private final boolean maskingEnabled;
    private final @Nullable PayloadMaskingStrategy maskingStrategy;
    private final @Nullable Set<String> sensitiveKeys;

    private A2APayloadLoggingConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.maxSize = builder.maxSize;
        this.maskingEnabled = builder.maskingEnabled;
        this.maskingStrategy = builder.maskingStrategy;
        this.sensitiveKeys = builder.sensitiveKeys;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean isMaskingEnabled() {
        return maskingEnabled;
    }

    public @Nullable PayloadMaskingStrategy getMaskingStrategy() {
        return maskingStrategy;
    }

    public @Nullable Set<String> getSensitiveKeys() {
        return sensitiveKeys;
    }

    /**
     * Creates config from system properties.
     */
    public static A2APayloadLoggingConfig fromSystemProperties() {
        Builder builder = new Builder();
        String enabledProp = System.getProperty(ENABLED_PROPERTY);
        if (enabledProp != null) {
            builder.enabled(Boolean.parseBoolean(enabledProp));
        }
        String maxSizeProp = System.getProperty(MAX_SIZE_PROPERTY);
        if (maxSizeProp != null) {
            try {
                builder.maxSize(Integer.parseInt(maxSizeProp.trim()));
            } catch (NumberFormatException ignored) {
                // use default
            }
        }
        String maskingEnabledProp = System.getProperty(MASKING_ENABLED_PROPERTY);
        if (maskingEnabledProp != null) {
            builder.maskingEnabled(Boolean.parseBoolean(maskingEnabledProp));
        }
        // Strategy loaded separately by logger via ServiceLoader / reflection;
        // we don't instantiate here to avoid class loading issues in fromSystemProperties
        String strategyClass = System.getProperty(MASKING_STRATEGY_PROPERTY);
        if (strategyClass != null && !strategyClass.isBlank()) {
            try {
                Class<?> clazz = Class.forName(strategyClass.trim());
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof PayloadMaskingStrategy pms) {
                    builder.maskingStrategy(pms);
                }
            } catch (Exception ignored) {
            }
        }
        String sensitiveKeysProp = System.getProperty(MASKING_SENSITIVE_KEYS_PROPERTY);
        if (sensitiveKeysProp != null && !sensitiveKeysProp.isBlank()) {
            Set<String> keys = Set.of(sensitiveKeysProp.split(","));
            // Trim each key
            Set<String> trimmed = new java.util.HashSet<>();
            for (String k : keys) {
                if (k != null && !k.isBlank()) {
                    trimmed.add(k.trim());
                }
            }
            if (!trimmed.isEmpty()) {
                builder.sensitiveKeys(trimmed);
            }
        }
        return builder.build();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.enabled = this.enabled;
        b.maxSize = this.maxSize;
        b.maskingEnabled = this.maskingEnabled;
        b.maskingStrategy = this.maskingStrategy;
        b.sensitiveKeys = this.sensitiveKeys;
        return b;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean enabled = false;
        private int maxSize = DEFAULT_MAX_SIZE;
        private boolean maskingEnabled = true;
        private @Nullable PayloadMaskingStrategy maskingStrategy = null;
        private @Nullable Set<String> sensitiveKeys = null;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder maskingEnabled(boolean maskingEnabled) {
            this.maskingEnabled = maskingEnabled;
            return this;
        }

        public Builder maskingStrategy(@Nullable PayloadMaskingStrategy maskingStrategy) {
            this.maskingStrategy = maskingStrategy;
            return this;
        }

        public Builder sensitiveKeys(@Nullable Set<String> sensitiveKeys) {
            this.sensitiveKeys = sensitiveKeys;
            return this;
        }

        public A2APayloadLoggingConfig build() {
            return new A2APayloadLoggingConfig(this);
        }
    }

    @Override
    public String toString() {
        return "A2APayloadLoggingConfig{"
                + "enabled=" + enabled
                + ", maxSize=" + maxSize
                + ", maskingEnabled=" + maskingEnabled
                + ", maskingStrategy=" + (maskingStrategy != null ? maskingStrategy.getClass().getName() : "default")
                + '}';
    }
}
