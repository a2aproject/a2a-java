package org.a2aproject.sdk.compat03.spec;

/**
 * A simplified error response wrapper for non-JSON-RPC error scenarios.
 * <p>
 * This record provides a lightweight error response format for cases where
 * a full JSON-RPC error structure is not appropriate, such as HTTP-level
 * errors or transport-layer failures.
 * <p>
 * Unlike {@link JSONRPCErrorResponse_v0_3}, this is not part of the JSON-RPC 2.0
 * specification but serves as a utility for simpler error reporting in the
 * A2A Java SDK implementation.
 *
 * @param error a human-readable error message
 * @see JSONRPCErrorResponse_v0_3
 * @see JSONRPCError_v0_3
 */
public record JSONErrorResponse_v0_3(String error) {
}
