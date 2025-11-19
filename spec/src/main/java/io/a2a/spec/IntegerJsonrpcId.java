package io.a2a.spec;

/**
 * Integer-based implementation of JSON-RPC request/response correlation identifier.
 * <p>
 * This class represents a JSON-RPC ID that uses an Integer value for correlation
 * between requests and responses. According to the JSON-RPC 2.0 specification,
 * Number IDs are one of the valid identifier types (along with String and NULL).
 * <p>
 * Integer IDs are commonly used when:
 * <ul>
 *   <li>Sequential request numbering is desired</li>
 *   <li>Memory efficiency is important (integers are smaller than strings)</li>
 *   <li>Simple monotonic correlation is sufficient</li>
 * </ul>
 *
 * @see JsonrpcId
 * @see StringJsonrpcId
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
public class IntegerJsonrpcId implements JsonrpcId {
}
