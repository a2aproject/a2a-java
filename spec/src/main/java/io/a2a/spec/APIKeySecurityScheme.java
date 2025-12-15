package io.a2a.spec;

import io.a2a.util.Assert;

import static io.a2a.spec.APIKeySecurityScheme.API_KEY;

/**
 * API key security scheme for agent authentication.
 * <p>
 * This security scheme uses an API key that can be sent in a header, query parameter,
 * or cookie to authenticate requests to the agent.
 * <p>
 * Corresponds to the OpenAPI "apiKey" security scheme type.
 *
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class APIKeySecurityScheme implements SecurityScheme {

    /** The security scheme type identifier for API key authentication. */
    public static final String API_KEY = "apiKey";
    private final Location location;
    private final String name;
    private final String type;
    private final String description;

    /**
     * Represents the location of the API key.
     */
    public enum Location {
        /** API key sent in a cookie. */
        COOKIE("cookie"),

        /** API key sent in an HTTP header. */
        HEADER("header"),

        /** API key sent as a query parameter. */
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
                default -> throw new IllegalArgumentException("Invalid API key location: " + location);
            }
        }
    }

    /**
     * Creates a new APIKeySecurityScheme with the specified location, name, and description.
     *
     * @param location the location where the API key is sent (required)
     * @param name the name of the API key parameter (required)
     * @param description a human-readable description (optional)
     * @throws IllegalArgumentException if location or name is null
     */
    public APIKeySecurityScheme(Location location, String name, String description) {
        this(location, name, description, API_KEY);
    }

    /**
     * Creates a new APIKeySecurityScheme with explicit type specification.
     *
     * @param location the location where the API key is sent (required)
     * @param name the name of the API key parameter (required)
     * @param description a human-readable description (optional)
     * @param type the security scheme type (must be "apiKey")
     * @throws IllegalArgumentException if location, name, or type is null, or if type is not "apiKey"
     */
    public APIKeySecurityScheme(Location location, String name,
                                String description, String type) {
        Assert.checkNotNullParam("location", location);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
        if (! type.equals(API_KEY)) {
            throw new IllegalArgumentException("Invalid type for APIKeySecurityScheme");
        }
        this.location = location;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    /**
     * Gets the human-readable description of this security scheme.
     *
     * @return the description, or null if not provided
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Gets the location where the API key is sent.
     *
     * @return the API key location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the name of the API key parameter.
     *
     * @return the parameter name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the security scheme type.
     *
     * @return always returns "apiKey"
     */
    public String getType() {
        return type;
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
        private Location location;
        private String name;
        private String description;

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
            return new APIKeySecurityScheme(location, name, description);
        }
    }
}
