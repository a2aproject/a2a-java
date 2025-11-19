package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    @JsonCreator
    public SetTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                 @JsonProperty("result") TaskPushNotificationConfig result,
                                                 @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    public SetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public SetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }
}
