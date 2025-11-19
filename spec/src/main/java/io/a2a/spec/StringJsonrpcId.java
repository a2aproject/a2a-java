package io.a2a.spec;

/**
 * String-based implementation of JSON-RPC request/response correlation identifier.
 * <p>
 * This class represents a JSON-RPC ID that uses a String value for correlation
 * between requests and responses. According to the JSON-RPC 2.0 specification,
 * String IDs are one of the valid identifier types (along with Number and NULL).
 * <p>
 * String IDs are commonly used when:
 * <ul>
 *   <li>Client-generated correlation requires UUID or similar format</li>
 *   <li>IDs need to encode additional context or metadata</li>
 *   <li>Compatibility with systems that use string-based correlation</li>
 * </ul>
 *
 * @see JsonrpcId
 * @see IntegerJsonrpcId
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
public class StringJsonrpcId implements JsonrpcId {
}
