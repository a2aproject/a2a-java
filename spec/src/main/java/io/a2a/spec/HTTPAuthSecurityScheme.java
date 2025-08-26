package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Defines a security scheme using HTTP authentication.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HTTPAuthSecurityScheme implements SecurityScheme {

    public static final String HTTP = "http";
    private final String bearerFormat;
    private final String scheme;
    private final String description;
    private final String type;

    public HTTPAuthSecurityScheme(String bearerFormat, String scheme, String description) {
        this(bearerFormat, scheme, description, HTTP);
    }

    @JsonCreator
    public HTTPAuthSecurityScheme(@JsonProperty("bearerFormat") String bearerFormat, @JsonProperty("scheme") String scheme,
                                  @JsonProperty("description") String description, @JsonProperty("type") String type) {
        Assert.checkNotNullParam("scheme", scheme);
        if (! type.equals(HTTP)) {
            throw new IllegalArgumentException("Invalid type for HTTPAuthSecurityScheme");
        }
        this.bearerFormat = bearerFormat;
        this.scheme = scheme;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public String getScheme() {
        return scheme;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private String bearerFormat;
        private String scheme;
        private String description;

        public Builder bearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }

        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public HTTPAuthSecurityScheme build() {
            return new HTTPAuthSecurityScheme(bearerFormat, scheme, description);
        }
    }
}
