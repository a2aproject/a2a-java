package org.a2aproject.sdk.compat03.spec;


import java.util.Map;


import org.a2aproject.sdk.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Client Credentials flow.
 */
public record ClientCredentialsOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    public ClientCredentialsOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

}
