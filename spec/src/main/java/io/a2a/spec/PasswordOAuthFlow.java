package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth 2.0 Resource Owner Password Credentials flow.
 * <p>
 * The password flow allows the client to exchange a user's credentials (username and password)
 * directly for an access token. This flow should only be used when there is a high degree of
 * trust between the user and the client application.
 * <p>
 * <strong>Note:</strong> This flow is generally discouraged and deprecated in OAuth 2.1
 * because it exposes user credentials to the client. Use the authorization code flow
 * with PKCE instead whenever possible.
 *
 * @param refreshUrl URL for obtaining refresh tokens (optional)
 * @param scopes map of available OAuth scopes to their descriptions (required)
 * @param tokenUrl URL for the token endpoint where credentials are exchanged for tokens (required)
 * @see OAuthFlows for the container of all supported OAuth flows
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.3">RFC 6749 - Resource Owner Password Credentials Grant</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record PasswordOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param refreshUrl the refreshUrl parameter (see class-level JavaDoc)
     * @param scopes the scopes parameter (see class-level JavaDoc)
     * @param tokenUrl the tokenUrl parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if scopes or tokenUrl is null
     */
    public PasswordOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
