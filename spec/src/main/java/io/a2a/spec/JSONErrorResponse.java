package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A simplified error response wrapper for non-JSON-RPC error scenarios.
 * <p>
 * This record provides a lightweight error response format for cases where
 * a full JSON-RPC error structure is not appropriate, such as HTTP-level
 * errors or transport-layer failures.
 * <p>
 * Unlike {@link JSONRPCErrorResponse}, this is not part of the JSON-RPC 2.0
 * specification but serves as a utility for simpler error reporting in the
 * A2A Java SDK implementation.
 *
 * @param error a human-readable error message
 * @see JSONRPCErrorResponse
 * @see JSONRPCError
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record JSONErrorResponse(String error) {
}
