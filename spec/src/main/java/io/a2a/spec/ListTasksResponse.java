package io.a2a.spec;

/**
 * The response for a list tasks request.
 */
public final class ListTasksResponse extends JSONRPCResponse<ListTasksResult> {

    public ListTasksResponse(String jsonrpc, Object id, ListTasksResult result, JSONRPCError error) {
        super(jsonrpc, id, result, error, ListTasksResult.class);
    }

    public ListTasksResponse(Object id, ListTasksResult result) {
        this(null, id, result, null);
    }

    public ListTasksResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
