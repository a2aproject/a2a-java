package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth 2.0 Implicit flow.
 * <p>
 * The implicit flow is designed for browser-based applications where the client
 * cannot securely store credentials. The access token is returned directly from
 * the authorization endpoint without an intermediate authorization code.
 * <p>
 * <strong>Note:</strong> The implicit flow is considered less secure than the
 * authorization code flow and is deprecated in OAuth 2.1. It should only be used
 * for legacy applications or when the authorization code flow with PKCE is not feasible.
 *
 * @param authorizationUrl URL for the authorization endpoint where users authenticate (required)
 * @param refreshUrl URL for obtaining refresh tokens (optional, rarely used in implicit flow)
 * @param scopes map of available OAuth scopes to their descriptions (required)
 * @see OAuthFlows for the container of all supported OAuth flows
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.2">RFC 6749 - Implicit Grant</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImplicitOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes) {

    public ImplicitOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
    }
}
