package io.a2a.spec;

/**
 * The response for a list tasks request.
 */
public final class ListTasksResponse extends JSONRPCResponse<ListTasksResult> {

    /**


     * Constructs response with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param result the result


     * @param error the error if any


     */


    public ListTasksResponse(String jsonrpc, Object id, ListTasksResult result, JSONRPCError error) {
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


    public ListTasksResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
