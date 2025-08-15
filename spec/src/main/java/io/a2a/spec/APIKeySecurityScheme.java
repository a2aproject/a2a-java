package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.a2a.util.Assert;

/**
 * Represents an API Key security scheme.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class APIKeySecurityScheme implements SecurityScheme {

    public static final String API_KEY = "apiKey";
    private final String in;
    private final String name;
    private final String type;
    private final String description;

    /**
     * Represents the location of the API key.
     */
    public enum Location {
        COOKIE("cookie"),
        HEADER("header"),
        QUERY("query");

        private final String location;

        Location(String location) {
            this.location = location;
        }

        @JsonValue
        public String asString() {
            return location;
        }

        @JsonCreator
        public static Location fromString(String location) {
            switch (location) {
                case "cookie":
                    return COOKIE;
                case "header":
                    return HEADER;
                case "query":
                    return QUERY;
                default:
                    throw new IllegalArgumentException("Invalid API key location: " + location);
            }
        }
    }

    /**
     * Constructs a new APIKeySecurityScheme with the specified parameters.
     *
     * @param in the location of the API key (header, query, or cookie)
     * @param name the name of the API key parameter
     * @param description a description of the security scheme
     */
    public APIKeySecurityScheme(String in, String name, String description) {
        this(in, name, description, API_KEY);
    }

    /**
     * Constructs a new APIKeySecurityScheme with the specified parameters.
     * This constructor is used for JSON deserialization.
     *
     * @param in the location of the API key (header, query, or cookie)
     * @param name the name of the API key parameter
     * @param description a description of the security scheme
     * @param type the type of security scheme (must be "apiKey")
     * @throws IllegalArgumentException if the type is not "apiKey"
     */
    @JsonCreator
    public APIKeySecurityScheme(@JsonProperty("in") String in, @JsonProperty("name") String name,
                                @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("in", in);
        Assert.checkNotNullParam("name", name);
        if (! type.equals(API_KEY)) {
            throw new IllegalArgumentException("Invalid type for APIKeySecurityScheme");
        }
        this.in = in;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }


    /**
     * Gets the location of the API key.
     *
     * @return the location where the API key should be placed (header, query, or cookie)
     */
    public String getIn() {
        return in;
    }

    /**
     * Gets the name of the API key parameter.
     *
     * @return the parameter name for the API key
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type of this security scheme.
     *
     * @return the security scheme type (always "apiKey")
     */
    public String getType() {
        return type;
    }

    /**
     * Builder class for constructing APIKeySecurityScheme instances.
     */
    public static class Builder {
        private String in;
        private String name;
        private String description;

        /**
         * Sets the location of the API key.
         *
         * @param in the location where the API key should be placed (header, query, or cookie)
         * @return this builder instance for method chaining
         */
        public Builder in(String in) {
            this.in = in;
            return this;
        }

        /**
         * Sets the name of the API key parameter.
         *
         * @param name the parameter name for the API key
         * @return this builder instance for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description of the security scheme.
         *
         * @param description a description of the security scheme
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new APIKeySecurityScheme instance.
         *
         * @return a new APIKeySecurityScheme with the configured parameters
         */
        public APIKeySecurityScheme build() {
            return new APIKeySecurityScheme(in, name, description);
        }
    }
}
