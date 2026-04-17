package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * A JSON RPC error response.
 */
public final class JSONRPCErrorResponse extends JSONRPCResponse<Void> {

    /**
     * Constructs a JSON-RPC error response with all fields.
     * <p>
     * This constructor is used for JSON deserialization.
     *
     * @param jsonrpc the JSON-RPC version (must be "2.0")
     * @param id the request ID, or null if the ID could not be determined from the request
     * @param result must be null for error responses
     * @param error the error object describing what went wrong (required)
     * @throws IllegalArgumentException if error is null
     */
    public JSONRPCErrorResponse(String jsonrpc, Object id, Void result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Void.class);
        Assert.checkNotNullParam("error", error);
    }

    public JSONRPCErrorResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public JSONRPCErrorResponse(JSONRPCError error) {
        this(null, null, null, error);
    }
}
