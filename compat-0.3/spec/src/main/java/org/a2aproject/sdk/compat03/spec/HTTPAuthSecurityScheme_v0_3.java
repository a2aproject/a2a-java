package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Defines a security scheme using HTTP authentication.
 */
public record HTTPAuthSecurityScheme_v0_3(
        @Nullable String bearerFormat,
        String scheme,
        @Nullable String description,
        String type
) implements SecurityScheme_v0_3 {

    public static final String TYPE = "http";

    public HTTPAuthSecurityScheme_v0_3(@Nullable String bearerFormat, String scheme, @Nullable String description, @Nullable String type) {
        Assert.checkNotNullParam("scheme", scheme);
        if (type != null && !TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid type for HTTPAuthSecurityScheme");
        }
        this.bearerFormat = bearerFormat;
        this.scheme = scheme;
        this.description = description;
        this.type = TYPE;
    }

    public HTTPAuthSecurityScheme_v0_3(@Nullable String bearerFormat, String scheme, @Nullable String description) {
        this(bearerFormat, scheme, description, TYPE);
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

        public HTTPAuthSecurityScheme_v0_3 build() {
            return new HTTPAuthSecurityScheme_v0_3(bearerFormat, scheme, description);
        }
    }
}
