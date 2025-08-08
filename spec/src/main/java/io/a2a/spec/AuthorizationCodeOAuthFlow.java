package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth Authorization Code flow.
 * This flow is used for server-side applications where the client can securely store credentials.
 * The authorization code flow involves redirecting the user to the authorization server,
 * obtaining an authorization code, and then exchanging it for an access token.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizationCodeOAuthFlow(String authorizationUrl, String refreshUrl, Map<String, String> scopes,
                                         String tokenUrl) {

    /**
     * Compact constructor that validates the required parameters.
     * 
     * @throws IllegalArgumentException if any required parameter is null
     */
    public AuthorizationCodeOAuthFlow {
        Assert.checkNotNullParam("authorizationUrl", authorizationUrl);
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }
}
