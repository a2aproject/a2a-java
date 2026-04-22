package io.a2a.spec;

import io.a2a.util.Assert;

import static io.a2a.spec.MutualTLSSecurityScheme.MUTUAL_TLS;

/**
 * Defines a security scheme using mTLS authentication.
 */
public final class MutualTLSSecurityScheme implements SecurityScheme {

    public static final String MUTUAL_TLS = "mutualTLS";
    private final String description;
    private final String type;

    public MutualTLSSecurityScheme(String description) {
        this(description, MUTUAL_TLS);
    }

    public MutualTLSSecurityScheme() {
        this(null, MUTUAL_TLS);
    }

    public MutualTLSSecurityScheme(String description, String type) {
        Assert.checkNotNullParam("type", type);
        if (!type.equals(MUTUAL_TLS)) {
            throw new IllegalArgumentException("Invalid type for MutualTLSSecurityScheme");
        }
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

}
