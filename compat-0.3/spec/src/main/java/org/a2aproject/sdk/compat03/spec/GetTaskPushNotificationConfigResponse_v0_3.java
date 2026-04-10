package org.a2aproject.sdk.compat03.spec;

/**
 * A response for a get task push notification request.
 */
public final class GetTaskPushNotificationConfigResponse_v0_3 extends JSONRPCResponse_v0_3<TaskPushNotificationConfig_v0_3> {

    public GetTaskPushNotificationConfigResponse_v0_3(String jsonrpc, Object id, TaskPushNotificationConfig_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig_v0_3.class);
    }

    public GetTaskPushNotificationConfigResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public GetTaskPushNotificationConfigResponse_v0_3(Object id, TaskPushNotificationConfig_v0_3 result) {
        this(null, id, result, null);
    }

}
