package io.a2a.spec;

/**
 * Marker interface for JSON-RPC request identifiers.
 * 
 * According to the JSON-RPC 2.0 specification, the request identifier can be:
 * - A String
 * - A Number (integer or decimal)
 * - null (for notification requests)
 * 
 * This interface serves as a type marker to identify classes that can be used
 * as JSON-RPC request identifiers. Implementations should handle the specific
 * type of identifier (string, integer, etc.) and provide appropriate serialization
 * and deserialization behavior.
 * 
 * The identifier is used to correlate JSON-RPC requests with their corresponding
 * responses, allowing clients to match responses to the original requests in
 * asynchronous communication scenarios.
 * 
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 * @see IntegerJsonrpcId
 * @see StringJsonrpcId
 */
public interface JsonrpcId {
}
