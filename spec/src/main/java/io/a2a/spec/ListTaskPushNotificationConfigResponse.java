package io.a2a.spec;

/**
 * JSON-RPC response containing all push notification configurations for a task with pagination support.
 * <p>
 * This response returns a {@link ListTaskPushNotificationConfigResult} containing
 * {@link TaskPushNotificationConfig} entries configured for the requested task,
 * showing all active notification endpoints with optional pagination information.
 * <p>
 * If an error occurs, the error field will contain a {@link JSONRPCError}.
 *
 * @see ListTaskPushNotificationConfigRequest for the corresponding request
 * @see ListTaskPushNotificationConfigResult for the result structure
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class ListTaskPushNotificationConfigResponse extends JSONRPCResponse<ListTaskPushNotificationConfigResult> {

    /**
     * Constructs response with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the result containing list of push notification configurations and pagination info
     * @param error the error (if any)
     */
    public ListTaskPushNotificationConfigResponse(String jsonrpc, Object id, ListTaskPushNotificationConfigResult result, JSONRPCError error) {
        super(jsonrpc, id, result, error, ListTaskPushNotificationConfigResult.class);
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
     * @param result the result containing list of push notification configurations and pagination info
     */
    public ListTaskPushNotificationConfigResponse(Object id, ListTaskPushNotificationConfigResult result) {
        this(null, id, result, null);
    }

}
