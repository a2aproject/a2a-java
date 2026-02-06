package io.a2a.spec;


import io.a2a.util.Assert;

/**
 * Authentication information for agent authentication and push notification endpoints.
 * <p>
 * This record encapsulates authentication schemes and credentials for two primary use cases:
 * <ul>
 *   <li><b>Agent Authentication:</b> Clients authenticate to access protected agent resources.
 *       The {@code scheme} reference a security scheme from {@link AgentCard#securitySchemes()}.</li>
 *   <li><b>Push Notification Authentication:</b> Agents authenticate when POSTing task updates to
 *       client-provided push notification endpoints. Supports HTTP Basic, Bearer tokens, API keys, OAuth.</li>
 * </ul>
 *
 * @param scheme security scheme name for authentication (required)
 * @param credentials optional credentials string (format depends on scheme, e.g., base64-encoded for Basic auth)
 * @see AgentCard#securitySchemes() for available security schemes
 * @see PushNotificationConfig for push notification configuration
 * @see SecurityScheme for security scheme definitions
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AuthenticationInfo(String scheme, String credentials) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param scheme the schemes parameter (see class-level JavaDoc)
     * @param credentials the credentials parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if schemes is null
     */
    public AuthenticationInfo {
        Assert.checkNotNullParam("scheme", scheme);
    }
}
