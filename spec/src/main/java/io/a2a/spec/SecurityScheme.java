package io.a2a.spec;

/**
 * Base interface for security schemes used to authenticate access to agent endpoints.
 * <p>
 * This sealed interface defines a discriminated union of authentication mechanisms based on
 * the OpenAPI 3.0 Security Scheme Object specification. Each implementation represents a
 * different authentication strategy that can be declared in an {@link AgentCard}.
 * <p>
 * Supported security schemes:
 * <ul>
 *   <li>{@link APIKeySecurityScheme} - API key in header, query, or cookie</li>
 *   <li>{@link HTTPAuthSecurityScheme} - HTTP Basic or Bearer authentication</li>
 *   <li>{@link OAuth2SecurityScheme} - OAuth 2.0 flows</li>
 *   <li>{@link OpenIdConnectSecurityScheme} - OpenID Connect discovery</li>
 *   <li>{@link MutualTLSSecurityScheme} - Client certificate authentication</li>
 * </ul>
 *
 * @see AgentCard#securitySchemes() for security scheme declarations
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme Object</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public sealed interface SecurityScheme permits APIKeySecurityScheme, HTTPAuthSecurityScheme, OAuth2SecurityScheme,
        OpenIdConnectSecurityScheme, MutualTLSSecurityScheme {

    /**
     * Returns the human-readable description of this security scheme.
     * @return the description, or null if not provided
     */
    String description();

    /**
     * Returns the type of the security scheme.
     * @return  the type of the security scheme.
     */
    String type();
}
