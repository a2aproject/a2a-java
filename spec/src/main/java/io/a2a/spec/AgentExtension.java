package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a protocol extension supported by an agent.
 * <p>
 * AgentExtension declares optional or required capabilities beyond the core A2A Protocol
 * specification. Extensions allow agents to advertise support for additional features,
 * behaviors, or custom protocol enhancements identified by a unique URI.
 * <p>
 * Extensions may include parameters for configuration and can be marked as required,
 * indicating that clients must support the extension to interact with the agent successfully.
 * <p>
 * This class is immutable. Use the {@link Builder} for construction.
 *
 * @param description a human-readable description of the extension's purpose (optional)
 * @param params configuration parameters for the extension (optional)
 * @param required whether support for this extension is mandatory for clients (defaults to false)
 * @param uri the unique identifier URI for this extension (required)
 * @see AgentCard
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentExtension (@Nullable String description, @Nullable Map<String, Object> params, boolean required, String uri) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param description the description parameter (see class-level JavaDoc)
     * @param params the params parameter (see class-level JavaDoc)
     * @param required the required parameter (see class-level JavaDoc)
     * @param uri the uri parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if uri is null
     */
    public AgentExtension {
        Assert.checkNotNullParam("uri", uri);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link AgentExtension} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * AgentExtension ext = AgentExtension.builder()
     *     .uri("https://example.com/extensions/custom-auth")
     *     .description("Custom authentication extension")
     *     .required(true)
     *     .params(Map.of("authType", "bearer"))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        @Nullable  String description;
        @Nullable Map<String, Object> params;
        boolean required;
        @Nullable String uri;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets a human-readable description of the extension's purpose.
         *
         * @param description the extension description (optional)
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets configuration parameters for the extension.
         *
         * @param params map of parameter key-value pairs (optional)
         * @return this builder for method chaining
         */
        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        /**
         * Sets whether support for this extension is mandatory.
         *
         * @param required true if clients must support this extension (defaults to false)
         * @return this builder for method chaining
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets the unique identifier URI for this extension.
         *
         * @param uri the extension URI (required)
         * @return this builder for method chaining
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Builds a new immutable {@link AgentExtension} from the current builder state.
         *
         * @return a new AgentExtension instance
         * @throws IllegalArgumentException if uri is null
         */
        public AgentExtension build() {
            return new AgentExtension(description, params, required, Assert.checkNotNullParam("uri", uri));
        }
    }

}
