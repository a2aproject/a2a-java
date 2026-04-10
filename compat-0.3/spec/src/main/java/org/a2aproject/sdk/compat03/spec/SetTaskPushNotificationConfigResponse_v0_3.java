package org.a2aproject.sdk.compat03.spec;

/**
 * The response after receiving a set task push notification request.
 */
public final class SetTaskPushNotificationConfigResponse_v0_3 extends JSONRPCResponse_v0_3<TaskPushNotificationConfig_v0_3> {

    public SetTaskPushNotificationConfigResponse_v0_3(String jsonrpc, Object id, TaskPushNotificationConfig_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig_v0_3.class);
    }

    public SetTaskPushNotificationConfigResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public SetTaskPushNotificationConfigResponse_v0_3(Object id, TaskPushNotificationConfig_v0_3 result) {
        this(null, id, result, null);
    }
}
