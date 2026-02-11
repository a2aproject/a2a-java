package io.a2a.spec;

import org.jspecify.annotations.Nullable;

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
 * @param deviceCode        OAuth 2.0 device code flow configuration
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#oauth-flows-object">OpenAPI OAuth Flows Object</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record OAuthFlows(@Nullable AuthorizationCodeOAuthFlow authorizationCode, @Nullable ClientCredentialsOAuthFlow clientCredentials,
                         @Nullable DeviceCodeOAuthFlow deviceCode) {

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
        private @Nullable AuthorizationCodeOAuthFlow authorizationCode;
        private @Nullable ClientCredentialsOAuthFlow clientCredentials;
        private @Nullable DeviceCodeOAuthFlow deviceCode;

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
         * Sets the device code flow configuration.
         *
         * @param deviceCode the device code flow (optional)
         * @return this builder for method chaining
         */
        public Builder deviceCode(DeviceCodeOAuthFlow deviceCode) {
            this.deviceCode = deviceCode;
            return this;
        }


        /**
         * Builds a new immutable OAuthFlows instance.
         *
         * @return a new OAuthFlows instance
         */
        public OAuthFlows build() {
            return new OAuthFlows(authorizationCode, clientCredentials, deviceCode);
        }
    }
}
