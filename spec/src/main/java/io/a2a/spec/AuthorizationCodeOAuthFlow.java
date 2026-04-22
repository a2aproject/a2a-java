package io.a2a.spec;

import java.util.Map;


import io.a2a.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Authorization Code flow.
 *
 * @param authorizationUrl the URL for the authorization endpoint
 * @param refreshUrl optional URL for obtaining refresh tokens
 * @param scopes the available scopes mapped to their descriptions
 * @param tokenUrl the URL for the token endpoint
 */
public record AuthorizationCodeOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes,
                                         String tokenUrl) {

    public AuthorizationCodeOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
