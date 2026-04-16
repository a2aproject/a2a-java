package org.a2aproject.sdk.compat03.spec;

/**
 * The response for a get task request.
 */
public final class GetTaskResponse_v0_3 extends JSONRPCResponse_v0_3<Task_v0_3> {

    public GetTaskResponse_v0_3(String jsonrpc, Object id, Task_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, Task_v0_3.class);
    }

    public GetTaskResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public GetTaskResponse_v0_3(Object id, Task_v0_3 result) {
        this(null, id, result, null);
    }
}
