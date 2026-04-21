package org.a2aproject.sdk.compat03.spec;

import java.util.List;

/**
 * A response for a list task push notification config request.
 */
public final class ListTaskPushNotificationConfigResponse_v0_3 extends JSONRPCResponse_v0_3<List<TaskPushNotificationConfig_v0_3>> {

    public ListTaskPushNotificationConfigResponse_v0_3(String jsonrpc, Object id, List<TaskPushNotificationConfig_v0_3> result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, (Class<List<TaskPushNotificationConfig_v0_3>>) (Class<?>) List.class);
    }

    public ListTaskPushNotificationConfigResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public ListTaskPushNotificationConfigResponse_v0_3(Object id, List<TaskPushNotificationConfig_v0_3> result) {
        this(null, id,  result, null);
    }

}
