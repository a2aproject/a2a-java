package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;

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
@JsonTypeName(API_KEY)
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

    public APIKeySecurityScheme(String in, String name, String description) {
        this(in, name, description, API_KEY);
    }

    @JsonCreator
    public APIKeySecurityScheme(@JsonProperty("in") String in, @JsonProperty("name") String name,
                                @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("in", in);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("type", type);
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


    public String getIn() {
        return in;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private String in;
        private String name;
        private String description;

        public Builder in(String in) {
            this.in = in;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public APIKeySecurityScheme build() {
            return new APIKeySecurityScheme(in, name, description);
        }
    }
}
