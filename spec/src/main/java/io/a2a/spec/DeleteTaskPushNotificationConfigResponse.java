package io.a2a.spec;

/**
 * A response for a delete task push notification config request.
 */
public final class DeleteTaskPushNotificationConfigResponse extends JSONRPCResponse<Void> {

    public DeleteTaskPushNotificationConfigResponse(String jsonrpc, Object id, Void result,JSONRPCError error) {
        super(jsonrpc, id, result, error, Void.class);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id) {
        this(null, id, null, null);
    }

}
