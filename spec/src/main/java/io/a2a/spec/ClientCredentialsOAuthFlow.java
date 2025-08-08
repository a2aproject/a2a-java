package io.a2a.spec;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Configuration for the OAuth Client Credentials flow.
 * This flow is used for server-to-server authentication where the client application
 * authenticates directly with the authorization server using its own credentials.
 * No user interaction is required for this flow.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientCredentialsOAuthFlow(String refreshUrl, Map<String, String> scopes, String tokenUrl) {

    /**
     * Compact constructor that validates the required parameters.
     * 
     * @throws IllegalArgumentException if any required parameter is null
     */
    public ClientCredentialsOAuthFlow {
        Assert.checkNotNullParam("scopes", scopes);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
    }

}
