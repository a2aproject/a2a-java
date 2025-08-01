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
        switch (transport) {
            case "JSONRPC":
                return JSONRPC;
            case "GRPC":
                return GRPC;
            case "HTTP+JSON":
                return HTTP_JSON;
            default:
                throw new IllegalArgumentException("Invalid transport: " + transport);
        }
    }
}