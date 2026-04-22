package io.a2a.spec;

/**
 * The response after receiving a send message request.
 */
public final class SendMessageResponse extends JSONRPCResponse<EventKind> {

    public SendMessageResponse(String jsonrpc, Object id, EventKind result, JSONRPCError error) {
        super(jsonrpc, id, result, error, EventKind.class);
    }

    public SendMessageResponse(Object id, EventKind result) {
        this(null, id, result, null);
    }

    public SendMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
