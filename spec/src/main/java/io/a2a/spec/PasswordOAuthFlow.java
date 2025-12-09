package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Resource Owner Password flow.
 */
public record PasswordOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    public PasswordOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
