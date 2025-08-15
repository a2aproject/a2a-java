package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth 2.0 Implicit flow as defined in the A2A specification.
 * This corresponds to the ImplicitOAuthFlow interface in the TypeScript definitions.
 * 
 * The Implicit flow is used for client-side applications that cannot securely store
 * client credentials. The authorization server issues access tokens directly to the
 * client without requiring client authentication.
 * 
 * @param authorizationUrl The authorization URL to be used for this flow (required, must be HTTPS)
 * @param refreshUrl The URL to be used for obtaining refresh tokens (optional, must be HTTPS if provided)
 * @param scopes The available scopes for the OAuth2 security scheme (required, may be empty)
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.2">RFC 6749 Section 4.2 - Implicit Grant</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImplicitOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes) {

    /**
     * Compact constructor that validates required parameters.
     * 
     * @throws IllegalArgumentException if authorizationUrl or scopes is null
     */
    public ImplicitOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
    }
}
