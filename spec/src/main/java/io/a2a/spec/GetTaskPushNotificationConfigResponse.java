package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-RPC response containing the push notification configuration for a task.
 * This response provides the webhook configuration details that the server uses
 * to send asynchronous task updates to the client. The configuration includes
 * the webhook URL, HTTP method, headers, and other notification settings.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    /**
     * Creates a new GetTaskPushNotificationConfigResponse with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the response identifier matching the original request
     * @param result the push notification configuration if the request was successful
     * @param error the error information if the request failed
     */
    @JsonCreator
    public GetTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                 @JsonProperty("result") TaskPushNotificationConfig result,
                                                 @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    /**
     * Creates an error GetTaskPushNotificationConfigResponse with error information.
     * The JSON-RPC version defaults to "2.0" and no result is set.
     * 
     * @param id the response identifier matching the original request
     * @param error the error information explaining why the request failed
     */
    public GetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Creates a successful GetTaskPushNotificationConfigResponse with the push notification configuration.
     * The JSON-RPC version defaults to "2.0" and no error is set.
     * 
     * @param id the response identifier matching the original request
     * @param result the push notification configuration
     */
    public GetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }

}
