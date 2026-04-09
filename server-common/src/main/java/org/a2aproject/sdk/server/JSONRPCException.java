package org.a2aproject.sdk.server;

import org.a2aproject.sdk.spec.A2AError;

/**
 * Exception wrapper for JSON-RPC protocol errors.
 * <p>
 * This exception encapsulates a {@link A2AError} for handling
 * protocol-level errors during JSON-RPC request processing.
 * </p>
 */
public class JSONRPCException extends Exception{
    private final A2AError error;

    /**
     * Creates a JSONRPCException wrapping the specified error.
     *
     * @param error the JSON-RPC error
     */
    public JSONRPCException(A2AError error) {
        this.error = error;
    }

    /**
     * Returns the wrapped JSON-RPC error.
     *
     * @return the JSON-RPC error
     */
    public A2AError getError() {
        return error;
    }
}
