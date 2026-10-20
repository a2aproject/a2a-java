package org.a2aproject.sdk.common.logging;

import org.jspecify.annotations.Nullable;

public final class A2APayloadLoggingConfig {
    public static final int DEFAULT_MAX_SIZE = 4096;
    public static final boolean DEFAULT_ENABLED = false;
    public static final boolean DEFAULT_MASKING_ENABLED = true;

    private final boolean enabled;
    private final int maxSize;
    private final boolean maskingEnabled;
    @Nullable private final PayloadMaskingStrategy maskingStrategy;

    private A2APayloadLoggingConfig(Builder b) {
        this.enabled = b.enabled; this.maxSize = b.maxSize;
        this.maskingEnabled = b.maskingEnabled; this.maskingStrategy = b.maskingStrategy;
    }

    public boolean isEnabled() { return enabled; }
    public int getMaxSize() { return maxSize; }
    public boolean isMaskingEnabled() { return maskingEnabled; }
    @Nullable public PayloadMaskingStrategy getMaskingStrategy() { return maskingStrategy; }

    public static Builder builder() { return new Builder(); }
    public static A2APayloadLoggingConfig defaults() { return builder().build(); }

    public static A2APayloadLoggingConfig fromSystemProperties() {
        Builder b = builder();
        String en = System.getProperty("a2a.logging.payload.enabled");
        if (en != null) b.enabled(Boolean.parseBoolean(en));
        String ms = System.getProperty("a2a.logging.payload.max-size");
        if (ms != null) try { b.maxSize(Integer.parseInt(ms)); } catch (NumberFormatException ignored) {}
        String me = System.getProperty("a2a.logging.payload.masking.enabled");
        if (me != null) b.maskingEnabled(Boolean.parseBoolean(me));
        String keys = System.getProperty("a2a.logging.payload.masking.sensitive-keys");
        if (keys != null && !keys.isBlank()) {
            String[] parts = keys.split(",");
            java.util.Set<String> set = new java.util.HashSet<>();
            for (String p : parts) set.add(p.trim());
            b.maskingStrategy(new DefaultPayloadMaskingStrategy(set));
        }
        String strat = System.getProperty("a2a.logging.payload.masking.strategy");
        if (strat != null && !strat.isBlank()) try {
            Class<?> cl = Class.forName(strat);
            Object inst = cl.getDeclaredConstructor().newInstance();
            if (inst instanceof PayloadMaskingStrategy) b.maskingStrategy((PayloadMaskingStrategy) inst);
        } catch (Exception ignored) {}
        return b.build();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.enabled = enabled; b.maxSize = maxSize; b.maskingEnabled = maskingEnabled; b.maskingStrategy = maskingStrategy;
        return b;
    }

    public static final class Builder {
        private boolean enabled = DEFAULT_ENABLED;
        private int maxSize = DEFAULT_MAX_SIZE;
        private boolean maskingEnabled = DEFAULT_MASKING_ENABLED;
        @Nullable private PayloadMaskingStrategy maskingStrategy = null;
        public Builder enabled(boolean v) { enabled = v; return this; }
        public Builder maxSize(int v) { maxSize = v; return this; }
        public Builder maskingEnabled(boolean v) { maskingEnabled = v; return this; }
        public Builder maskingStrategy(PayloadMaskingStrategy v) { maskingStrategy = v; return this; }
        public A2APayloadLoggingConfig build() { return new A2APayloadLoggingConfig(this); }
    }
}
