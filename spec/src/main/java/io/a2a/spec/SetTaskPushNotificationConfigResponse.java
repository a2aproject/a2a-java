package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 response to a task push notification configuration request.
 * This response is sent back to the client after processing a SetTaskPushNotificationConfigRequest,
 * indicating whether the push notification configuration was successfully set or if an error occurred.
 *
 * <p>The response can contain either:</p>
 * <ul>
 *   <li>A successful result with the configured TaskPushNotificationConfig</li>
 *   <li>An error object describing what went wrong during the configuration process</li>
 * </ul>
 *
 * <p>This follows the JSON-RPC 2.0 specification for response messages, ensuring
 * compatibility with standard JSON-RPC clients and tooling.</p>
 *
 * @see SetTaskPushNotificationConfigRequest
 * @see TaskPushNotificationConfig
 * @see JSONRPCResponse
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    /**
     * Creates a new SetTaskPushNotificationConfigResponse with all JSON-RPC 2.0 fields.
     * This constructor is primarily used by JSON deserialization.
     *
     * @param jsonrpc the JSON-RPC protocol version (should be "2.0")
     * @param id the request identifier that matches the original request
     * @param result the task push notification configuration result (null if error occurred)
     * @param error the error object (null if successful)
     */
    @JsonCreator
    public SetTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                 @JsonProperty("result") TaskPushNotificationConfig result,
                                                 @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    /**
     * Creates an error response for a task push notification configuration request.
     * The JSON-RPC version defaults to "2.0" and the result is set to null.
     *
     * @param id the request identifier that matches the original request
     * @param error the error object describing what went wrong
     */
    public SetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Creates a successful response for a task push notification configuration request.
     * The JSON-RPC version defaults to "2.0" and the error is set to null.
     *
     * @param id the request identifier that matches the original request
     * @param result the successfully configured task push notification configuration
     */
    public SetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }
}
