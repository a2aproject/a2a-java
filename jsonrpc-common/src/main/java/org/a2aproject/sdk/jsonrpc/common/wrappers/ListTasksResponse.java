package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;

/**
 * The response for a list tasks request.
 */
public final class ListTasksResponse extends A2AResponse<ListTasksResult> {

    /**
     * Constructs response with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the result
     * @param error the error if any
     */
    public ListTasksResponse(String jsonrpc, Object id, ListTasksResult result, A2AError error) {
        super(jsonrpc, id, result, error, ListTasksResult.class);
    }

    /**
     * Constructs successful response.
     *
     * @param id the request ID
     * @param result the result
     */
    public ListTasksResponse(Object id, ListTasksResult result) {
        this(null, id, result, null);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public ListTasksResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }
}
