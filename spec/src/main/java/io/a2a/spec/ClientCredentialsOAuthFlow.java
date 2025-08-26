package io.a2a.spec;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Defines configuration details for the OAuth 2.0 Client Credentials flow.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientCredentialsOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    public ClientCredentialsOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

}
