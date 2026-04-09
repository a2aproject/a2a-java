package org.a2aproject.sdk.spec;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Represents a protocol-level error in the A2A Protocol.
 * <p>
 * This error extends {@link A2AError} to distinguish A2A protocol-specific errors
 * from standard JSON-RPC errors. Protocol errors have dedicated error codes in the
 * A2A specification.
 *
 * @see A2AError for the base error implementation
 */
public class A2AProtocolError extends A2AError {

    /**
     * Constructs a protocol error with the specified code, message, and details.
     *
     * @param code the numeric error code (required, see JSON-RPC 2.0 spec for standard codes)
     * @param message the human-readable error message (required)
     * @param details additional error details as key-value pairs (defaults to empty map if null)
     */
    public A2AProtocolError(Integer code, String message, @Nullable Map<String, Object> details) {
        super(code, message, details);
    }
}
