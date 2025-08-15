package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the configuration for supported OAuth 2.0 flows in the A2A protocol.
 * This record defines the various OAuth 2.0 grant types that an agent supports
 * for authentication and authorization.
 *
 * <p>OAuth flows define how clients can obtain access tokens to authenticate
 * with the agent. Different flows are suitable for different types of applications:</p>
 * <ul>
 *   <li>Authorization Code - for web applications with server-side components</li>
 *   <li>Client Credentials - for machine-to-machine authentication</li>
 *   <li>Implicit - for single-page applications (deprecated in OAuth 2.1)</li>
 *   <li>Password - for trusted applications (not recommended for new implementations)</li>
 * </ul>
 *
 * <p>This implementation follows the OpenAPI OAuth Flows Object specification,
 * ensuring compatibility with standard OAuth 2.0 implementations and tooling.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthFlows(AuthorizationCodeOAuthFlow authorizationCode, ClientCredentialsOAuthFlow clientCredentials,
                         ImplicitOAuthFlow implicit, PasswordOAuthFlow password) {

    /**
     * Builder class for constructing OAuthFlows instances.
     * Provides a fluent API for configuring supported OAuth 2.0 flows.
     */
    public static class Builder {
        /** Configuration for the authorization code flow */
        private AuthorizationCodeOAuthFlow authorizationCode;
        
        /** Configuration for the client credentials flow */
        private ClientCredentialsOAuthFlow clientCredentials;
        
        /** Configuration for the implicit flow */
        private ImplicitOAuthFlow implicit;
        
        /** Configuration for the password flow */
        private PasswordOAuthFlow password;

        /**
         * Sets the authorization code flow configuration.
         * 
         * @param authorizationCode the authorization code flow configuration
         * @return this builder instance for method chaining
         */
        public Builder authorizationCode(AuthorizationCodeOAuthFlow authorizationCode) {
            this.authorizationCode = authorizationCode;
            return this;
        }

        /**
         * Sets the client credentials flow configuration.
         * 
         * @param clientCredentials the client credentials flow configuration
         * @return this builder instance for method chaining
         */
        public Builder clientCredentials(ClientCredentialsOAuthFlow clientCredentials) {
            this.clientCredentials = clientCredentials;
            return this;
        }

        /**
         * Sets the implicit flow configuration.
         * 
         * @param implicit the implicit flow configuration
         * @return this builder instance for method chaining
         */
        public Builder implicit(ImplicitOAuthFlow implicit) {
            this.implicit = implicit;
            return this;
        }

        /**
         * Sets the password flow configuration.
         * 
         * @param password the password flow configuration
         * @return this builder instance for method chaining
         */
        public Builder password(PasswordOAuthFlow password) {
            this.password = password;
            return this;
        }

        /**
         * Builds and returns a new OAuthFlows instance.
         * 
         * @return a new OAuthFlows with the configured flow settings
         */
        public OAuthFlows build() {
            return new OAuthFlows(authorizationCode, clientCredentials, implicit, password);
        }
    }
}
