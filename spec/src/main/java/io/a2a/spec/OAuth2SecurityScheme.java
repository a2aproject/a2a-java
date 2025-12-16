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

    /**
     * The type identifier for OAuth 2.0 security schemes: "oauth2".
     */
    public static final String OAUTH2 = "oauth2";
    private final OAuthFlows flows;
    private final String description;
    private final String type;
    private final String oauth2MetadataUrl;

    /**
     * Constructs an OAuth 2.0 security scheme.
     *
     * @param flows the OAuth 2.0 flow configuration (required)
     * @param description optional description of the security scheme
     * @param oauth2MetadataUrl optional URL to OAuth 2.0 metadata (RFC 8414)
     */
    public OAuth2SecurityScheme(OAuthFlows flows, String description, String oauth2MetadataUrl) {
        this(flows, description, oauth2MetadataUrl, OAUTH2);
    }

    /**
     * Constructs an OAuth 2.0 security scheme with explicit type.
     *
     * @param flows the OAuth 2.0 flow configuration (required)
     * @param description optional description of the security scheme
     * @param oauth2MetadataUrl optional URL to OAuth 2.0 metadata (RFC 8414)
     * @param type the security scheme type (must be "oauth2")
     */
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
    /**
     * Gets the Description.
     *
     * @return the Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the OAuth flows configuration.
     *
     * @return the OAuth flows
     */
    public OAuthFlows getFlows() {
        return flows;
    }

    /**
     * Gets the Type.
     *
     * @return the Type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the Oauth2MetadataUrl.
     *
     * @return the Oauth2MetadataUrl
     */
    public String getOauth2MetadataUrl() {
        return oauth2MetadataUrl;
    }

    /**
     * Creates a new builder for {@link OAuth2SecurityScheme}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing instances.
     */
    public static class Builder {
        private OAuthFlows flows;
        private String description;
        private String oauth2MetadataUrl;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the flows.
         *
         * @param flows the flows
         * @return this builder for method chaining
         */
        public Builder flows(OAuthFlows flows) {
            this.flows = flows;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the oauth2MetadataUrl.
         *
         * @param oauth2MetadataUrl the oauth2MetadataUrl
         * @return this builder for method chaining
         */
        public Builder oauth2MetadataUrl(String oauth2MetadataUrl) {
            this.oauth2MetadataUrl = oauth2MetadataUrl;
            return this;
        }

        /**
         * Builds the OAuth2SecurityScheme.
         *
         * @return a new OAuth2SecurityScheme instance
         */
        public OAuth2SecurityScheme build() {
            return new OAuth2SecurityScheme(flows, description, oauth2MetadataUrl);
        }
    }
}
