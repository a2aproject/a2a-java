package io.a2a.server;

import io.a2a.spec.JSONRPCError;

/**
 * Exception wrapper for JSON-RPC protocol errors.
 * <p>
 * This exception encapsulates a {@link JSONRPCError} for handling
 * protocol-level errors during JSON-RPC request processing.
 * </p>
 */
public class JSONRPCException extends Exception{
    private final JSONRPCError error;

    /**
     * Creates a JSONRPCException wrapping the specified error.
     *
     * @param error the JSON-RPC error
     */
    public JSONRPCException(JSONRPCError error) {
        this.error = error;
    }

    /**
     * Returns the wrapped JSON-RPC error.
     *
     * @return the JSON-RPC error
     */
    public JSONRPCError getError() {
        return error;
    }
}
