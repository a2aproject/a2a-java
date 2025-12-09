package io.a2a.spec;

/**
 * A response for a get task push notification request.
 */
public final class GetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    public GetTaskPushNotificationConfigResponse(String jsonrpc, Object id, TaskPushNotificationConfig result, JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    public GetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }

}
