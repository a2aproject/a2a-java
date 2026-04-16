package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * Defines a security scheme using OpenID Connect.
 */
public final class OpenIdConnectSecurityScheme_v0_3 implements SecurityScheme_v0_3 {

    public static final String OPENID_CONNECT = "openIdConnect";
    private final String openIdConnectUrl;
    private final String description;
    private final String type;

    public OpenIdConnectSecurityScheme_v0_3(String openIdConnectUrl, String description) {
        this(openIdConnectUrl, description, OPENID_CONNECT);
    }

    public OpenIdConnectSecurityScheme_v0_3(String openIdConnectUrl, String description, String type) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("openIdConnectUrl", openIdConnectUrl);
        if (!type.equals(OPENID_CONNECT)) {
            throw new IllegalArgumentException("Invalid type for OpenIdConnectSecurityScheme");
        }
        this.openIdConnectUrl = openIdConnectUrl;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public String getType() {
        return type;
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
