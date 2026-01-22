package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Configuration details for the OAuth 2.0 Device Code flow (RFC 8628).
 * <p>
 * This flow is designed for input-constrained devices such as IoT devices,
 * and CLI tools where the user authenticates on a separate device.
 *
 * @param deviceAuthorizationUrl the device authorization endpoint URL (required)
 * @param tokenUrl URL for the token endpoint where credentials are exchanged for tokens (required)
 * @param refreshUrl URL for obtaining refresh tokens (optional)
 * @param scopes map of available OAuth scopes to their descriptions (required)
 * @see OAuthFlows for the container of all supported OAuth flows
 * @see OAuth2SecurityScheme for the security scheme using these flows
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record DeviceCodeOAuthFlow(String deviceAuthorizationUrl, String tokenUrl, String refreshUrl, Map<String, String> scopes) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param deviceAuthorizationUrl the device authorization endpoint URL (required)
     * @param tokenUrl URL for the token endpoint where credentials are exchanged for tokens (required)
     * @param refreshUrl URL for obtaining refresh tokens (optional)
     * @param scopes map of available OAuth scopes to their descriptions (required)
     * @throws IllegalArgumentException if authorizationUrl, scopes, or tokenUrl is null
     */
    public DeviceCodeOAuthFlow {
        Assert.checkNotNullParam("deviceAuthorizationUrl", deviceAuthorizationUrl);
        Assert.checkNotNullParam("tokenUrl", tokenUrl);
        Assert.checkNotNullParam("scopes", scopes);
    }
}
