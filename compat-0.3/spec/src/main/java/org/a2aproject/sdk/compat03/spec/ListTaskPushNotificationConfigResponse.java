package org.a2aproject.sdk.compat03.spec;

import java.util.List;

/**
 * A response for a list task push notification config request.
 */
public final class ListTaskPushNotificationConfigResponse extends JSONRPCResponse<List<TaskPushNotificationConfig>> {

    public ListTaskPushNotificationConfigResponse(String jsonrpc, Object id, List<TaskPushNotificationConfig> result, JSONRPCError error) {
        super(jsonrpc, id, result, error, (Class<List<TaskPushNotificationConfig>>) (Class<?>) List.class);
    }

    public ListTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public ListTaskPushNotificationConfigResponse(Object id, List<TaskPushNotificationConfig> result) {
        this(null, id,  result, null);
    }

}
