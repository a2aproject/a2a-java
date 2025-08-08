package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Represents an extension for an A2A agent.
 * <p>
 * Agent extensions provide additional functionality or capabilities that can be
 * added to an agent. Each extension is identified by a URI and can include
 * parameters, a description, and a required flag indicating whether the
 * extension is mandatory for the agent to function properly.
 * </p>
 *
 * @param description a human-readable description of the extension
 * @param params a map of parameters specific to this extension
 * @param required whether this extension is required for the agent to function
 * @param uri the unique identifier URI for this extension
 */
public record AgentExtension (String description, Map<String, Object> params, boolean required, String uri) {

    /**
     * Compact constructor that validates the extension parameters.
     * <p>
     * This constructor ensures that the URI parameter is not null, as it is
     * required to uniquely identify the extension.
     * </p>
     *
     * @throws IllegalArgumentException if uri is null
     */
    public AgentExtension {
        Assert.checkNotNullParam("uri", uri);
    }

    /**
     * Builder class for constructing AgentExtension instances.
     */
    public static class Builder {
        String description;
        Map<String, Object> params;
        boolean required;
        String uri;

        /**
         * Sets the description of the extension.
         *
         * @param description a human-readable description of the extension
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the parameters for the extension.
         *
         * @param params a map of parameters specific to this extension
         * @return this builder instance for method chaining
         */
        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        /**
         * Sets whether this extension is required.
         *
         * @param required true if this extension is required for the agent to function
         * @return this builder instance for method chaining
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets the URI identifier for the extension.
         *
         * @param uri the unique identifier URI for this extension
         * @return this builder instance for method chaining
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Builds and returns a new AgentExtension instance.
         *
         * @return a new AgentExtension with the configured parameters
         */
        public AgentExtension build() {
            return new AgentExtension(description, params, required, uri);
        }
    }

}
