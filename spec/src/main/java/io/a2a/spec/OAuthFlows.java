package io.a2a.spec;

/**
 * Configuration for supported OAuth 2.0 authorization flows.
 * <p>
 * This record specifies which OAuth 2.0 flows the agent supports and their configurations,
 * including authorization and token endpoints, scopes, and refresh URLs.
 * <p>
 * All fields are optional; only the flows supported by the agent should be specified.
 *
 * @param authorizationCode OAuth 2.0 authorization code flow configuration
 * @param clientCredentials OAuth 2.0 client credentials flow configuration
 * @param implicit OAuth 2.0 implicit flow configuration
 * @param password OAuth 2.0 resource owner password credentials flow configuration
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#oauth-flows-object">OpenAPI OAuth Flows Object</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record OAuthFlows(AuthorizationCodeOAuthFlow authorizationCode, ClientCredentialsOAuthFlow clientCredentials,
                         ImplicitOAuthFlow implicit, PasswordOAuthFlow password) {

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link OAuthFlows} instances.
     */
    public static class Builder {
        private AuthorizationCodeOAuthFlow authorizationCode;
        private ClientCredentialsOAuthFlow clientCredentials;
        private ImplicitOAuthFlow implicit;
        private PasswordOAuthFlow password;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the authorization code flow configuration.
         *
         * @param authorizationCode the authorization code flow (optional)
         * @return this builder for method chaining
         */
        public Builder authorizationCode(AuthorizationCodeOAuthFlow authorizationCode) {
            this.authorizationCode = authorizationCode;
            return this;
        }

        /**
         * Sets the client credentials flow configuration.
         *
         * @param clientCredentials the client credentials flow (optional)
         * @return this builder for method chaining
         */
        public Builder clientCredentials(ClientCredentialsOAuthFlow clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        /**
         * Sets the implicit flow configuration.
         *
         * @param implicit the implicit flow (optional)
         * @return this builder for method chaining
         */
        public Builder implicit(ImplicitOAuthFlow implicit) {
            this.implicit = implicit;
            return this;
        }

        /**
         * Sets the password flow configuration.
         *
         * @param password the password flow (optional)
         * @return this builder for method chaining
         */
        public Builder password(PasswordOAuthFlow password) {
            this.password = password;
            return this;
        }

        /**
         * Builds a new immutable OAuthFlows instance.
         *
         * @return a new OAuthFlows instance
         */
        public OAuthFlows build() {
            return new OAuthFlows(authorizationCode, clientCredentials, implicit, password);
        }
    }
}
