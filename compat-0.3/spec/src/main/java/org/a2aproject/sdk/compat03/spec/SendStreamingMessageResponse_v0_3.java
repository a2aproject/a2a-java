package org.a2aproject.sdk.compat03.spec;

/**
 * The response after receiving a request to initiate a task with streaming.
 */
public final class SendStreamingMessageResponse_v0_3 extends JSONRPCResponse_v0_3<StreamingEventKind_v0_3> {

    public SendStreamingMessageResponse_v0_3(String jsonrpc, Object id, StreamingEventKind_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, StreamingEventKind_v0_3.class);
    }

    public SendStreamingMessageResponse_v0_3(Object id, StreamingEventKind_v0_3 result) {
        this(null, id, result, null);
    }

    public SendStreamingMessageResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }
}
