package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * A declaration of a protocol extension supported by an Agent.
 */
public record AgentExtension_v0_3(String description, Map<String, Object> params, boolean required, String uri) {

    public AgentExtension_v0_3 {
        Assert.checkNotNullParam("uri", uri);
    }

    public static class Builder {
        String description;
        Map<String, Object> params;
        boolean required;
        String uri;

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public AgentExtension_v0_3 build() {
            return new AgentExtension_v0_3(description, params, required, uri);
        }
    }

}
