package io.a2a.spec;

import static io.a2a.spec.MutualTLSSecurityScheme.MUTUAL_TLS;

/**
 * Mutual TLS (mTLS) security scheme for agent authentication.
 * <p>
 * This security scheme uses mutual TLS authentication, where both the client and server
 * present X.509 certificates to authenticate each other. This provides strong,
 * certificate-based authentication at the transport layer.
 * <p>
 * Unlike other authentication schemes that operate at the HTTP layer, mTLS authentication
 * occurs during the TLS handshake before any HTTP communication begins. This makes it
 * particularly suitable for high-security environments and machine-to-machine communication.
 * <p>
 * Example usage:
 * <pre>{@code
 * MutualTLSSecurityScheme scheme = new MutualTLSSecurityScheme(
 *     "Client certificate authentication required"
 * );
 * }</pre>
 *
 * @param description optional description of the security scheme
 * @see SecurityScheme for the base interface
 * @see <a href="https://spec.openapis.org/oas/v3.0.0#security-scheme-object">OpenAPI Security Scheme</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8446#section-4.4.2">RFC 8446 - TLS 1.3 Client Authentication</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record MutualTLSSecurityScheme(String description) implements SecurityScheme {

    /**
     * The type identifier for mutual TLS security schemes: "mutualTLS".
     */
    public static final String MUTUAL_TLS = "mutualTLS";

}
