package org.a2aproject.sdk.compat03.spec;

/**
 * A response to a cancel task request.
 */

public final class CancelTaskResponse extends JSONRPCResponse<Task> {

    public CancelTaskResponse(String jsonrpc, Object id, Task result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    public CancelTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }


    public CancelTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
