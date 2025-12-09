package io.a2a.spec;

/**
 * JSON-RPC response confirming push notification configuration for a task.
 * <p>
 * This response confirms that the push notification configuration has been successfully
 * registered for the task. The result contains the full {@link TaskPushNotificationConfig}
 * as stored by the agent.
 * <p>
 * If push notifications are not supported or an error occurs, the error field will contain
 * a {@link JSONRPCError} (e.g., {@link PushNotificationNotSupportedError}).
 *
 * @see SetTaskPushNotificationConfigRequest for the corresponding request
 * @see TaskPushNotificationConfig for the configuration structure
 * @see PushNotificationNotSupportedError for the error when unsupported
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    public SetTaskPushNotificationConfigResponse(String jsonrpc, Object id, TaskPushNotificationConfig result, JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    public SetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public SetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }
}
