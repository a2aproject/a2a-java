package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * Defines a security scheme using mTLS authentication.
 */
public final class MutualTLSSecurityScheme_v0_3 implements SecurityScheme_v0_3 {

    public static final String MUTUAL_TLS = "mutualTLS";
    private final String description;
    private final String type;

    public MutualTLSSecurityScheme_v0_3(String description) {
        this(description, MUTUAL_TLS);
    }

    public MutualTLSSecurityScheme_v0_3() {
        this(null, MUTUAL_TLS);
    }

    public MutualTLSSecurityScheme_v0_3(String description, String type) {
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
