package io.a2a.spec;

/**
 * The response after receiving a set task push notification request.
 */
public final class SetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    public SetTaskPushNotificationConfigResponse(String jsonrpc, Object id, TaskPushNotificationConfig result, JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    public SetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public SetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }
}
