package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * API key security scheme for agent authentication.
 * <p>
 * This security scheme uses an API key that can be sent in a header, query parameter,
 * or cookie to authenticate requests to the agent.
 * <p>
 * Corresponds to the OpenAPI "apiKey" security scheme type.
 *
 * @param location the location where the API key is sent (required)
 * @param name the name of the API key parameter (required)
 * @param description a human-readable description (optional)
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record APIKeySecurityScheme(Location location, String name, @Nullable
        String description) implements SecurityScheme {

    /**
     * The security scheme type identifier for API key authentication.
     */
    public static final String TYPE = "apiKeySecurityScheme";

    @Override
    public String type() {
        return TYPE;
    }

    /**
     * Compact constructor with validation.
     *
     * @param location the location where the API key is sent (required)
     * @param name the name of the API key parameter (required)
     * @param description a human-readable description (optional)
     * @throws IllegalArgumentException if location or name is null
     */
    public APIKeySecurityScheme {
        Assert.checkNotNullParam("location", location);
        Assert.checkNotNullParam("name", name);
    }

    /**
     * Represents the location of the API key.
     */
    public enum Location {
        /**
         * API key sent in a cookie.
         */
        COOKIE("cookie"),
        /**
         * API key sent in an HTTP header.
         */
        HEADER("header"),
        /**
         * API key sent as a query parameter.
         */
        QUERY("query");

        private final String location;

        Location(String location) {
            this.location = location;
        }

        /**
         * Converts this location to its string representation.
         *
         * @return the string representation of this location
         */
        public String asString() {
            return location;
        }

        /**
         * Converts a string to a Location enum value.
         *
         * @param location the string location ("cookie", "header", or "query")
         * @return the corresponding Location enum value
         * @throws IllegalArgumentException if the location string is invalid
         */
        public static Location fromString(String location) {
            switch (location) {
                case "cookie" -> {
                    return COOKIE;
                }
                case "header" -> {
                    return HEADER;
                }
                case "query" -> {
                    return QUERY;
                }
                default ->
                    throw new IllegalArgumentException("Invalid API key location: " + location);
            }
        }
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
     * Builder for constructing immutable {@link APIKeySecurityScheme} instances.
     * <p>
     * Example usage:
     * <pre>{@code
     * APIKeySecurityScheme scheme = APIKeySecurityScheme.builder()
     *     .location(Location.HEADER)
     *     .name("X-API-Key")
     *     .description("API key authentication")
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private @Nullable Location location;
        private @Nullable String name;
        private @Nullable String description;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the location where the API key should be sent.
         *
         * @param location the API key location (header, query, or cookie) (required)
         * @return this builder for method chaining
         */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the name of the API key parameter.
         *
         * @param name the parameter name (required)
         * @return this builder for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the human-readable description of the security scheme.
         *
         * @param description the description (optional)
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds a new immutable {@link APIKeySecurityScheme} from the current builder state.
         *
         * @return a new APIKeySecurityScheme instance
         * @throws IllegalArgumentException if location or name is null
         */
        public APIKeySecurityScheme build() {
            return new APIKeySecurityScheme(Assert.checkNotNullParam("location", location), 
                    Assert.checkNotNullParam("name", name), description);
        }
    }
}
