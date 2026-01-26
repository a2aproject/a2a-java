package io.a2a.jsonrpc.common.wrappers;

import io.a2a.spec.A2AError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.TaskPushNotificationConfig;

/**
 * JSON-RPC response confirming push notification configuration for a task.
 * <p>
 * This response confirms that the push notification configuration has been successfully
 * registered for the task. The result contains the full {@link TaskPushNotificationConfig}
 * as stored by the agent.
 * <p>
 * If push notifications are not supported or an error occurs, the error field will contain
 * a {@link A2AError} (e.g., {@link PushNotificationNotSupportedError}).
 *
 * @see CreateTaskPushNotificationConfigRequest for the corresponding request
 * @see TaskPushNotificationConfig for the configuration structure
 * @see PushNotificationNotSupportedError for the error when unsupported
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class CreateTaskPushNotificationConfigResponse extends A2AResponse<TaskPushNotificationConfig> {

    /**
     * Constructs response with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the push notification configuration
     * @param error the error (if any)
     */
    public CreateTaskPushNotificationConfigResponse(String jsonrpc, Object id, TaskPushNotificationConfig result, A2AError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public CreateTaskPushNotificationConfigResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs success response.
     *
     * @param id the request ID
     * @param result the push notification configuration
     */
    public CreateTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }
}
