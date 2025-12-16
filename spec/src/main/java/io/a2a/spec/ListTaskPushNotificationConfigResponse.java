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

    /**
     * Constructs response with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the list of push notification configurations
     * @param error the error (if any)
     */
    public ListTaskPushNotificationConfigResponse(String jsonrpc, Object id, List<TaskPushNotificationConfig> result, JSONRPCError error) {
        super(jsonrpc, id, result, error, (Class<List<TaskPushNotificationConfig>>) (Class<?>) List.class);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public ListTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs success response.
     *
     * @param id the request ID
     * @param result the list of push notification configurations
     */
    public ListTaskPushNotificationConfigResponse(Object id, List<TaskPushNotificationConfig> result) {
        this(null, id,  result, null);
    }

}
