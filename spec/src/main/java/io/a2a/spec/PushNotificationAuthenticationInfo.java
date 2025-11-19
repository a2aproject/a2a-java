package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Authentication details for push notification endpoints.
 * <p>
 * This record specifies the authentication schemes and credentials required for the agent
 * to authenticate when POSTing task updates to the push notification endpoint. Supports
 * various authentication methods including HTTP Basic, Bearer tokens, API keys, and OAuth.
 * <p>
 * The {@code schemes} field lists supported authentication types (e.g., "Basic", "Bearer"),
 * while {@code credentials} contains the actual authentication data (e.g., base64-encoded
 * credentials, tokens).
 *
 * @param schemes list of supported authentication scheme names (required)
 * @param credentials the authentication credentials or tokens (optional)
 * @see PushNotificationConfig for the full push notification configuration
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
