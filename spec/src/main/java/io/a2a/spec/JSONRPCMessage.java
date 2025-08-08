package io.a2a.spec;

/**
 * Base interface for all JSON-RPC 2.0 messages.
 * 
 * This sealed interface defines the common structure for JSON-RPC messages,
 * which includes both requests and responses. According to the JSON-RPC 2.0
 * specification, all messages must contain:
 * - A "jsonrpc" field specifying the protocol version ("2.0")
 * - An "id" field for request/response correlation (except for notifications)
 * 
 * This interface is sealed to ensure type safety and restrict implementations
 * to only the permitted subtypes: {@link JSONRPCRequest} and {@link JSONRPCResponse}.
 * 
 * @see JSONRPCRequest
 * @see JSONRPCResponse
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {

    /** The JSON-RPC protocol version constant */
    String JSONRPC_VERSION = "2.0";

    /**
     * Gets the JSON-RPC protocol version.
     * 
     * @return the protocol version string (should be "2.0")
     */
    String getJsonrpc();

    /**
     * Gets the message identifier.
     * 
     * The identifier is used to correlate requests with responses.
     * For notifications, this may be null.
     * 
     * @return the message identifier, or null for notifications
     */
    Object getId();
}
