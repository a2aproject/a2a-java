package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth 2.0 Authorization Code flow.
 * <p>
 * The authorization code flow is the most secure OAuth 2.0 flow, recommended for
 * server-side applications. It involves redirecting the user to an authorization
 * server, obtaining an authorization code, and then exchanging that code for an
 * access token.
 * <p>
 * This flow is suitable when the client can securely store client credentials
 * and the authorization code can be exchanged server-side.
 *
 * @param authorizationUrl URL for the authorization endpoint where users authenticate (required)
 * @param refreshUrl URL for obtaining refresh tokens (optional)
 * @param scopes map of available OAuth scopes to their descriptions (required)
 * @param tokenUrl URL for the token endpoint where codes are exchanged for tokens (required)
 * @param pkceRequired Indicates if PKCE (RFC 7636) is required for this flow. (required)
 * @see OAuthFlows for the container of all supported OAuth flows
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.1">RFC 6749 - Authorization Code Grant</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AuthorizationCodeOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes,
                                         String tokenUrl, boolean pkceRequired) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param authorizationUrl the authorizationUrl parameter (see class-level JavaDoc)
     * @param refreshUrl the refreshUrl parameter (see class-level JavaDoc)
     * @param scopes the scopes parameter (see class-level JavaDoc)
     * @param tokenUrl the tokenUrl parameter (see class-level JavaDoc)
     * @param pkceRequired Indicates if PKCE (RFC 7636) is required for this flow. (required)
     * @throws IllegalArgumentException if authorizationUrl, scopes, or tokenUrl is null
     */
    public AuthorizationCodeOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
