package io.a2a.spec;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth 2.0 Client Credentials flow.
 * <p>
 * The client credentials flow is designed for machine-to-machine authentication
 * where the client application authenticates using its own credentials (client ID
 * and secret) rather than on behalf of a user. This is suitable for server-to-server
 * communication and backend services.
 * <p>
 * This flow is appropriate when the client is acting on its own behalf, not
 * representing a user, such as accessing its own resources or performing
 * administrative operations.
 *
 * @param refreshUrl URL for obtaining refresh tokens (optional, rarely used in client credentials flow)
 * @param scopes map of available OAuth scopes to their descriptions (required)
 * @param tokenUrl URL for the token endpoint where client credentials are exchanged for tokens (required)
 * @see OAuthFlows for the container of all supported OAuth flows
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4">RFC 6749 - Client Credentials Grant</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientCredentialsOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    public ClientCredentialsOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

}
