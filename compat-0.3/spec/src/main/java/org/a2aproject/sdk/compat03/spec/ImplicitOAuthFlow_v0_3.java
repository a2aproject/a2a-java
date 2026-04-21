package org.a2aproject.sdk.compat03.spec;

import java.util.Map;


import org.a2aproject.sdk.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Implicit flow.
 */
public record ImplicitOAuthFlow_v0_3(String authorizationUrl, String refreshUrl, Map<String, String> scopes) {

    public ImplicitOAuthFlow_v0_3 {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
    }
}
