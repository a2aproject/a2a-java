package org.a2aproject.sdk.compat03.spec;

/**
 * The response after receiving a send message request.
 */
public final class SendMessageResponse_v0_3 extends JSONRPCResponse_v0_3<EventKind_v0_3> {

    public SendMessageResponse_v0_3(String jsonrpc, Object id, EventKind_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, EventKind_v0_3.class);
    }

    public SendMessageResponse_v0_3(Object id, EventKind_v0_3 result) {
        this(null, id, result, null);
    }

    public SendMessageResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }
}
