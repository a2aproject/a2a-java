package org.a2aproject.sdk.compat03.spec;

/**
 * A response for a delete task push notification config request.
 */
public final class DeleteTaskPushNotificationConfigResponse_v0_3 extends JSONRPCResponse_v0_3<Void> {

    public DeleteTaskPushNotificationConfigResponse_v0_3(String jsonrpc, Object id, Void result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, Void.class);
    }

    public DeleteTaskPushNotificationConfigResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public DeleteTaskPushNotificationConfigResponse_v0_3(Object id) {
        this(null, id, null, null);
    }

}
