package io.a2a.spec;

import io.a2a.util.Assert;

import static io.a2a.spec.OAuth2SecurityScheme.OAUTH2;

/**
 * Defines a security scheme using OAuth 2.0.
 */
public final class OAuth2SecurityScheme implements SecurityScheme {

    public static final String OAUTH2 = "oauth2";
    private final OAuthFlows flows;
    private final String description;
    private final String type;
    private final String oauth2MetadataUrl;

    public OAuth2SecurityScheme(OAuthFlows flows, String description, String oauth2MetadataUrl) {
        this(flows, description, oauth2MetadataUrl, OAUTH2);
    }

    public OAuth2SecurityScheme(OAuthFlows flows, String description, String oauth2MetadataUrl, String type) {
        Assert.checkNotNullParam("flows", flows);
        Assert.checkNotNullParam("type", type);
        if (!type.equals(OAUTH2)) {
            throw new IllegalArgumentException("Invalid type for OAuth2SecurityScheme");
        }
        this.flows = flows;
        this.description = description;
        this.oauth2MetadataUrl = oauth2MetadataUrl;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public OAuthFlows getFlows() {
        return flows;
    }

    public String getType() {
        return type;
    }

    public String getOauth2MetadataUrl() {
        return oauth2MetadataUrl;
    }

    public static class Builder {
        private OAuthFlows flows;
        private String description;
        private String oauth2MetadataUrl;

        public Builder flows(OAuthFlows flows) {
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

        public OAuth2SecurityScheme build() {
            return new OAuth2SecurityScheme(flows, description, oauth2MetadataUrl);
        }
    }
}
