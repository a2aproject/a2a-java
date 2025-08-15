package io.a2a.spec;

/**
 * Represents an integer-based JSON-RPC request identifier.
 * 
 * According to the JSON-RPC 2.0 specification, the ID can be a String, Number, or null.
 * This class specifically handles integer (Number) identifiers. Numbers should not
 * contain fractional parts as per the specification.
 * 
 * The ID is used to correlate JSON-RPC requests with their corresponding responses.
 * When a request is made, the same ID should be included in the response to allow
 * the client to match responses to requests.
 * 
 * @see <a href="https://www.jsonrpc.org/specification#request_object">JSON-RPC 2.0 Request Object</a>
 */
public class IntegerJsonrpcId {
}
