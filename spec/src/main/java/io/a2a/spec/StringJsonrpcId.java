package io.a2a.spec;

/**
 * Utility class for handling string-based JSON-RPC request identifiers.
 * This class provides functionality for working with string IDs in JSON-RPC 2.0 requests
 * and responses within the A2A protocol.
 *
 * <p>JSON-RPC 2.0 allows request identifiers to be strings, numbers, or null.
 * This class specifically handles the string variant, which is commonly used
 * for human-readable or UUID-based request tracking.</p>
 *
 * <p>String IDs are particularly useful for:</p>
 * <ul>
 *   <li>Debugging and logging purposes</li>
 *   <li>Correlation with external systems</li>
 *   <li>UUID-based request tracking</li>
 *   <li>Human-readable request identification</li>
 * </ul>
 *
 * @see JSONRPCRequest
 * @see JSONRPCResponse
 */
public class StringJsonrpcId {
}
