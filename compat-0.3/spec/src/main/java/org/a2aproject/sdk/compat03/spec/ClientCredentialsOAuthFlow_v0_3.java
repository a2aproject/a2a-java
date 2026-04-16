package org.a2aproject.sdk.compat03.spec;


import java.util.Map;


import org.a2aproject.sdk.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Client Credentials flow.
 */
public record ClientCredentialsOAuthFlow_v0_3(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    public ClientCredentialsOAuthFlow_v0_3 {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

}
