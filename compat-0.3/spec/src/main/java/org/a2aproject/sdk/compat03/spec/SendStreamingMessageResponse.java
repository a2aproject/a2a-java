package org.a2aproject.sdk.compat03.spec;

/**
 * The response after receiving a request to initiate a task with streaming.
 */
public final class SendStreamingMessageResponse extends JSONRPCResponse<StreamingEventKind> {

    public SendStreamingMessageResponse(String jsonrpc, Object id, StreamingEventKind result, JSONRPCError error) {
        super(jsonrpc, id, result, error, StreamingEventKind.class);
    }

    public SendStreamingMessageResponse(Object id, StreamingEventKind result) {
        this(null, id, result, null);
    }

    public SendStreamingMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
