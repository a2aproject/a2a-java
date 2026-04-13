package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

import static org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme.HTTP;

/**
 * Defines a security scheme using HTTP authentication.
 */
public final class HTTPAuthSecurityScheme implements SecurityScheme {

    public static final String HTTP = "http";
    private final String bearerFormat;
    private final String scheme;
    private final String description;
    private final String type;

    public HTTPAuthSecurityScheme(String bearerFormat, String scheme, String description) {
        this(bearerFormat, scheme, description, HTTP);
    }

    public HTTPAuthSecurityScheme(String bearerFormat, String scheme, String description, String type) {
        Assert.checkNotNullParam("scheme", scheme);
        Assert.checkNotNullParam("type", type);
        if (! HTTP.equals(type)) {
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
