package io.a2a.server.rest.quarkus;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

/**
 * Transport metadata implementation for Quarkus REST.
 *
 * <p>This class provides transport protocol identification for the Quarkus REST
 * reference implementation. It reports {@link TransportProtocol#HTTP_JSON} as
 * the transport protocol, indicating that this implementation uses HTTP with
 * JSON payloads for the A2A protocol.
 *
 * <p>The transport metadata is used by the framework for:
 * <ul>
 *   <li>Logging and monitoring (identifying which transport handled a request)</li>
 *   <li>Protocol negotiation and version compatibility checks</li>
 *   <li>Metrics and telemetry (transport-specific performance tracking)</li>
 * </ul>
 *
 * @see TransportMetadata
 * @see TransportProtocol
 */
public class QuarkusRestTransportMetadata implements TransportMetadata {
    /**
     * Returns the transport protocol identifier.
     *
     * @return {@code "http+json"} indicating HTTP transport with JSON encoding
     */
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }
}
