package io.a2a.spec;

import io.a2a.util.Assert;

import static io.a2a.spec.OAuth2SecurityScheme.OAUTH2;

/**
 * OAuth 2.0 security scheme for agent authentication.
 * <p>
 * This security scheme uses OAuth 2.0 authorization flows to authenticate requests.
 * Supports authorization code, client credentials, implicit, and password flows
 * via the {@link OAuthFlows} configuration.
 * <p>
 * Corresponds to the OpenAPI "oauth2" security scheme type.
 *
 * @see SecurityScheme for the base interface
 * @see OAuthFlows for flow configuration
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
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
