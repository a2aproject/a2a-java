package io.a2a.spec;

import java.util.Map;


import io.a2a.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Implicit flow.
 *
 * @param authorizationUrl the URL for the authorization endpoint
 * @param refreshUrl optional URL for obtaining refresh tokens
 * @param scopes the available scopes mapped to their descriptions
 */
public record ImplicitOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes) {

    public ImplicitOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
    }
}
