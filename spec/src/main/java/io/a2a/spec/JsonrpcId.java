package io.a2a.spec;

/**
 * Marker interface for JSON-RPC request/response correlation identifiers.
 * <p>
 * According to the JSON-RPC 2.0 specification, the {@code id} field must be
 * a String, Number, or NULL value. This interface serves as a type marker
 * for implementations that represent valid JSON-RPC IDs.
 * <p>
 * The A2A Java SDK provides two concrete implementations:
 * <ul>
 *   <li>{@link StringJsonrpcId} - For string-based identifiers</li>
 *   <li>{@link IntegerJsonrpcId} - For numeric identifiers</li>
 * </ul>
 * <p>
 * Null IDs are also valid and represent notifications (requests that do not
 * expect a response).
 *
 * @see StringJsonrpcId
 * @see IntegerJsonrpcId
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
public interface JsonrpcId {
}
