package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents the configuration for the OAuth 2.0 Resource Owner Password Credentials Grant flow.
 * This record defines the settings required for the password flow, which allows clients
 * to exchange a user's username and password directly for an access token.
 *
 * <p><strong>Security Warning:</strong> The password flow is considered less secure than
 * other OAuth 2.0 flows because it requires the client application to handle the user's
 * credentials directly. It should only be used when:</p>
 * <ul>
 *   <li>The client is highly trusted (e.g., first-party applications)</li>
 *   <li>Other flows are not feasible</li>
 *   <li>The client and authorization server have a high degree of trust</li>
 * </ul>
 *
 * <p>This flow is not recommended for new implementations and has been removed
 * from OAuth 2.1. Consider using the Authorization Code flow with PKCE instead.</p>
 *
 * <p>Required components:</p>
 * <ul>
 *   <li><strong>Token URL:</strong> The endpoint where tokens are requested</li>
 *   <li><strong>Refresh URL:</strong> Optional endpoint for refreshing tokens</li>
 *   <li><strong>Scopes:</strong> Map of available scopes and their descriptions</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PasswordOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    /**
     * Compact constructor for PasswordOAuthFlow.
     * Validates that required parameters are not null.
     *
     * @throws IllegalArgumentException if scopes or tokenUrl is null
     */
    public PasswordOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

    /**
     * Builder class for constructing PasswordOAuthFlow instances.
     * Provides a fluent API for configuring the OAuth password flow settings.
     */
    public static class Builder {
        /** The refresh endpoint URL */
        private String refreshUrl;
        
        /** Available OAuth scopes */
        private Map<String, String> scopes;
        
        /** The token endpoint URL */
        private String tokenUrl;

        /**
         * Sets the refresh endpoint URL.
         * This is the optional URL where refresh tokens can be exchanged for new access tokens.
         * 
         * @param refreshUrl the refresh endpoint URL (optional)
         * @return this builder instance for method chaining
         */
        public Builder refreshUrl(String refreshUrl) {
            this.refreshUrl = refreshUrl;
            return this;
        }

        /**
         * Sets the available OAuth scopes.
         * Each scope defines a specific permission level, with the key being the scope name
         * and the value being a human-readable description.
         * 
         * @param scopes map of scope names to their descriptions
         * @return this builder instance for method chaining
         */
        public Builder scopes(Map<String, String> scopes) {
            this.scopes = scopes;
            return this;
        }

        /**
         * Sets the token endpoint URL.
         * This is the URL where the client will send username/password credentials
         * to obtain an access token.
         * 
         * @param tokenUrl the token endpoint URL (required)
         * @return this builder instance for method chaining
         */
        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        /**
         * Builds and returns a new PasswordOAuthFlow instance.
         * 
         * @return a new PasswordOAuthFlow with the configured settings
         */
        public PasswordOAuthFlow build() {
            return new PasswordOAuthFlow(refreshUrl, scopes, tokenUrl);
        }
    }
}
