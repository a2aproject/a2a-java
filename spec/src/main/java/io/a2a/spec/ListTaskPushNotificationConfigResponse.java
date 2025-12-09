package io.a2a.spec;

import java.util.List;

/**
 * JSON-RPC response containing all push notification configurations for a task.
 * <p>
 * This response returns a list of all {@link TaskPushNotificationConfig} entries
 * configured for the requested task, showing all active notification endpoints.
 * <p>
 * If an error occurs, the error field will contain a {@link JSONRPCError}.
 *
 * @see ListTaskPushNotificationConfigRequest for the corresponding request
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
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
