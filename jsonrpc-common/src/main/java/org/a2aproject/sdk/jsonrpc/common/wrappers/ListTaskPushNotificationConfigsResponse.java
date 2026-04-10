package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;

/**
 * JSON-RPC response containing all push notification configurations for a task with pagination support.
 * <p>
 * This response returns a {@link ListTaskPushNotificationConfigsResult} containing
 * {@link TaskPushNotificationConfig} entries configured for the requested task,
 * showing all active notification endpoints with optional pagination information.
 * <p>
 * If an error occurs, the error field will contain a {@link A2AError}.
 *
 * @see ListTaskPushNotificationConfigsRequest for the corresponding request
 * @see ListTaskPushNotificationConfigsResult for the result structure
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class ListTaskPushNotificationConfigsResponse extends A2AResponse<ListTaskPushNotificationConfigsResult> {

    /**
     * Constructs response with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the result containing list of push notification configurations and pagination info
     * @param error the error (if any)
     */
    public ListTaskPushNotificationConfigsResponse(String jsonrpc, Object id, ListTaskPushNotificationConfigsResult result, A2AError error) {
        super(jsonrpc, id, result, error, ListTaskPushNotificationConfigsResult.class);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public ListTaskPushNotificationConfigsResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs success response.
     *
     * @param id the request ID
     * @param result the result containing list of push notification configurations and pagination info
     */
    public ListTaskPushNotificationConfigsResponse(Object id, ListTaskPushNotificationConfigsResult result) {
        this(null, id, result, null);
    }

}
