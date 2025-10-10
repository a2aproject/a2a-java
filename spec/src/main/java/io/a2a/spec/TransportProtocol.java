package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    @JsonValue
    public String asString() {
        return transport;
    }

    @JsonCreator
    public static TransportProtocol fromString(String transport) {
        return switch (transport) {
            case "JSONRPC" -> JSONRPC;
            case "GRPC" -> GRPC;
            case "HTTP+JSON" -> HTTP_JSON;
            default -> throw new IllegalArgumentException("Invalid transport: " + transport);
        };
    }
}
