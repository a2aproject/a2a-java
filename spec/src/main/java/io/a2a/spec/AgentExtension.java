package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * A declaration of a protocol extension supported by an Agent.
 */
public record AgentExtension (String description, Map<String, Object> params, boolean required, String uri) {

    public AgentExtension {
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

        public AgentExtension build() {
            return new AgentExtension(description, params, required, uri);
        }
    }

}
