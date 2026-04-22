package io.a2a.spec;

/**
 * The response for a get task request.
 */
public final class GetTaskResponse extends JSONRPCResponse<Task> {

    public GetTaskResponse(String jsonrpc, Object id, Task result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    public GetTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
