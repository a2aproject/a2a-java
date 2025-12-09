package io.a2a.spec;

/**
 * Enumeration of supported transport protocols for A2A Protocol communication.
 * <p>
 * This enum defines the communication mechanisms that A2A agents and clients can use
 * to exchange messages. Each transport protocol has different characteristics, advantages,
 * and use cases.
 * <p>
 * Supported transport protocols:
 * <ul>
 *   <li>{@link #JSONRPC} - JSON-RPC 2.0 over HTTP (most common, human-readable)</li>
 *   <li>{@link #GRPC} - gRPC binary protocol (high performance, streaming)</li>
 *   <li>{@link #HTTP_JSON} - RESTful HTTP with JSON (REST-style endpoints)</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * TransportProtocol protocol = TransportProtocol.JSONRPC;
 * String protocolName = protocol.asString();  // "JSONRPC"
 * TransportProtocol parsed = TransportProtocol.fromString("JSONRPC");
 * }</pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 * @see <a href="https://grpc.io/">gRPC</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public enum TransportProtocol {
    /**
     * JSON-RPC 2.0 transport protocol over HTTP.
     * <p>
     * This is the primary and most widely supported transport for the A2A Protocol.
     * It uses JSON-RPC 2.0 request/response semantics with JSON encoding, making it
     * human-readable and easy to debug. Supports both synchronous and streaming communication.
     */
    JSONRPC("JSONRPC"),

    /**
     * gRPC binary transport protocol.
     * <p>
     * High-performance binary protocol using Protocol Buffers for serialization.
     * Provides efficient streaming, strong typing, and cross-language support.
     * Suitable for high-throughput scenarios and service-to-service communication.
     */
    GRPC("GRPC"),

    /**
     * HTTP+JSON RESTful transport protocol.
     * <p>
     * REST-style HTTP endpoints with JSON payloads. Provides a more traditional
     * web API approach compared to JSON-RPC, using HTTP methods (GET, POST, etc.)
     * and resource-oriented URLs.
     */
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
