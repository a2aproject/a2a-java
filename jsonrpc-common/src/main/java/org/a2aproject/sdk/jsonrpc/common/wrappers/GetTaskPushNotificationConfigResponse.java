package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;

/**
 * JSON-RPC response containing a task's push notification configuration.
 * <p>
 * This response returns the {@link TaskPushNotificationConfig} for the requested task,
 * showing the current push notification endpoint and authentication settings.
 * <p>
 * If no configuration exists or an error occurs, the error field will contain a
 * {@link A2AError}.
 *
 * @see GetTaskPushNotificationConfigRequest for the corresponding request
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetTaskPushNotificationConfigResponse extends A2AResponse<TaskPushNotificationConfig> {

    /**
     * Constructs response with full parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the push notification config result
     * @param error the error if any
     */
    public GetTaskPushNotificationConfigResponse(String jsonrpc, Object id, TaskPushNotificationConfig result, A2AError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public GetTaskPushNotificationConfigResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs successful response.
     *
     * @param id the request ID
     * @param result the push notification config result
     */
    public GetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }

}
