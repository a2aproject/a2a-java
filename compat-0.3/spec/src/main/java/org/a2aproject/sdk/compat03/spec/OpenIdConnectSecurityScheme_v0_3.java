package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Defines a security scheme using OpenID Connect.
 */
public record OpenIdConnectSecurityScheme_v0_3(
        String openIdConnectUrl,
        @Nullable String description,
        String type
) implements SecurityScheme_v0_3 {

    public static final String TYPE = "openIdConnect";

    public OpenIdConnectSecurityScheme_v0_3 {
        Assert.checkNotNullParam("openIdConnectUrl", openIdConnectUrl);
        if (type == null) {
            type = TYPE;
        }
        if (!type.equals(TYPE)) {
            throw new IllegalArgumentException("Invalid type for OpenIdConnectSecurityScheme");
        }
    }

    public OpenIdConnectSecurityScheme_v0_3(String openIdConnectUrl, @Nullable String description) {
        this(openIdConnectUrl, description, TYPE);
    }

    public static class Builder {
        private String openIdConnectUrl;
        private String description;

        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public OpenIdConnectSecurityScheme_v0_3 build() {
            return new OpenIdConnectSecurityScheme_v0_3(openIdConnectUrl, description);
        }
    }
}
