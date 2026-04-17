package org.a2aproject.sdk.compat03.spec;

/**
 * Supported A2A transport protocols.
 */
public enum TransportProtocol {
    JSONRPC("JSONRPC"),
    GRPC("GRPC"),
    HTTP_JSON("HTTP+JSON");

    private final String transport;

    TransportProtocol(String transport) {
        this.transport = transport;
    }

    /**
     * Returns the string representation of this transport protocol.
     * <p>
     * Used for JSON serialization.
     *
     * @return the transport protocol name as a string
     */
    public String asString() {
        return transport;
    }

    /**
     * Parses a string into a {@link TransportProtocol} enum constant.
     * <p>
     * Used for JSON deserialization.
     *
     * @param transport the transport protocol string (e.g., "JSONRPC", "GRPC", "HTTP+JSON")
     * @return the corresponding TransportProtocol enum constant
     * @throws IllegalArgumentException if the transport string is not recognized
     */
    public static TransportProtocol fromString(String transport) {
        return switch (transport) {
            case "JSONRPC" -> JSONRPC;
            case "GRPC" -> GRPC;
            case "HTTP+JSON" -> HTTP_JSON;
            default -> throw new IllegalArgumentException("Invalid transport: " + transport);
        };
    }
}
