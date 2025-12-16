package io.a2a.spec;

/**
 * Base interface for all JSON-RPC 2.0 protocol messages in the A2A Protocol.
 * <p>
 * This sealed interface defines the fundamental structure shared by all JSON-RPC 2.0
 * messages used in the A2A Protocol's JSON-RPC transport layer. It ensures type safety
 * and exhaustiveness checking by permitting only {@link JSONRPCRequest} and {@link JSONRPCResponse}
 * as implementing types.
 * <p>
 * According to the JSON-RPC 2.0 specification, all messages must include a {@code jsonrpc}
 * version field set to "2.0", and may optionally include an {@code id} field for correlation
 * between requests and responses.
 * @see JSONRPCRequest
 * @see JSONRPCResponse
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {

    /**
     * The JSON-RPC protocol version string as defined by the JSON-RPC 2.0 specification.
     * All messages must have their {@code jsonrpc} field set to this value.
     */
    String JSONRPC_VERSION = "2.0";

    /**
     * Gets the JSON-RPC version for this message.
     * <p>
     * According to the JSON-RPC 2.0 specification, this must be exactly "2.0".
     *
     * @return the JSON-RPC version string, always {@value #JSONRPC_VERSION}
     */
    String getJsonrpc();

    /**
     * Gets the request/response correlation identifier.
     * <p>
     * The ID is used to match responses with their corresponding requests. It may be
     * a String, Integer, or null for notification-style requests that do not expect a response.
     *
     * @return the correlation ID, or null for notifications
     */
    Object getId();

}
