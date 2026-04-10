package org.a2aproject.sdk.compat03.spec;

/**
 * A response to a cancel task request.
 */

public final class CancelTaskResponse_v0_3 extends JSONRPCResponse_v0_3<Task_v0_3> {

    public CancelTaskResponse_v0_3(String jsonrpc, Object id, Task_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, Task_v0_3.class);
    }

    public CancelTaskResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }


    public CancelTaskResponse_v0_3(Object id, Task_v0_3 result) {
        this(null, id, result, null);
    }
}
