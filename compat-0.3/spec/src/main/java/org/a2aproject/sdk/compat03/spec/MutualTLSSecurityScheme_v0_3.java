package org.a2aproject.sdk.compat03.spec;

import org.jspecify.annotations.Nullable;

/**
 * Defines a security scheme using mTLS authentication.
 */
public record MutualTLSSecurityScheme_v0_3(@Nullable String description, String type) implements SecurityScheme_v0_3 {

    public static final String TYPE = "mutualTLS";

    public MutualTLSSecurityScheme_v0_3(@Nullable String description, @Nullable String type) {
        if (type != null && !type.equals(TYPE)) {
            throw new IllegalArgumentException("Invalid type for MutualTLSSecurityScheme");
        }
        this.description = description;
        this.type = TYPE;
    }

    public MutualTLSSecurityScheme_v0_3(@Nullable String description) {
        this(description, TYPE);
    }

    public MutualTLSSecurityScheme_v0_3() {
        this(null, TYPE);
    }
}
