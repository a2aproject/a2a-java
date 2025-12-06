package org.springframework.a2a.server.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.a2a.jsonrpc")
public class JsonRpcProperties {

    /**
     * Whether to enable the JSON-RPC auto-configuration.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}