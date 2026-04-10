package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * A JSON RPC error response.
 */
public final class JSONRPCErrorResponse_v0_3 extends JSONRPCResponse_v0_3<Void> {

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
    public JSONRPCErrorResponse_v0_3(String jsonrpc, Object id, Void result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, Void.class);
        Assert.checkNotNullParam("error", error);
    }

    public JSONRPCErrorResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public JSONRPCErrorResponse_v0_3(JSONRPCError_v0_3 error) {
        this(null, null, null, error);
    }
}
