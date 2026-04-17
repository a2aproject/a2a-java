package org.a2aproject.sdk.compat03.spec;

import java.util.Map;


import org.a2aproject.sdk.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Authorization Code flow.
 */
public record AuthorizationCodeOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes,
                                         String tokenUrl) {

    public AuthorizationCodeOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
