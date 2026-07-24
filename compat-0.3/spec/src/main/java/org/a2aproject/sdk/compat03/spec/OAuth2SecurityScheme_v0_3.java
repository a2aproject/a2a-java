package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Defines a security scheme using OAuth 2.0.
 */
public record OAuth2SecurityScheme_v0_3(
        OAuthFlows_v0_3 flows,
        @Nullable String description,
        @Nullable String oauth2MetadataUrl,
        String type
) implements SecurityScheme_v0_3 {

    public static final String TYPE = "oauth2";

    public OAuth2SecurityScheme_v0_3(OAuthFlows_v0_3 flows, @Nullable String description, @Nullable String oauth2MetadataUrl, @Nullable String type) {
        Assert.checkNotNullParam("flows", flows);
        if (type != null && !type.equals(TYPE)) {
            throw new IllegalArgumentException("Invalid type for OAuth2SecurityScheme");
        }
        this.flows = flows;
        this.description = description;
        this.oauth2MetadataUrl = oauth2MetadataUrl;
        this.type = TYPE;
    }

    public OAuth2SecurityScheme_v0_3(OAuthFlows_v0_3 flows, @Nullable String description, @Nullable String oauth2MetadataUrl) {
        this(flows, description, oauth2MetadataUrl, TYPE);
    }

    public static class Builder {
        private OAuthFlows_v0_3 flows;
        private String description;
        private String oauth2MetadataUrl;

        public Builder flows(OAuthFlows_v0_3 flows) {
            this.flows = flows;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder oauth2MetadataUrl(String oauth2MetadataUrl) {
            this.oauth2MetadataUrl = oauth2MetadataUrl;
            return this;
        }

        public OAuth2SecurityScheme_v0_3 build() {
            return new OAuth2SecurityScheme_v0_3(flows, description, oauth2MetadataUrl);
        }
    }
}
