package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response for a get task push notification request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskPushNotificationConfigResponse extends JSONRPCResponse<TaskPushNotificationConfig> {

    @JsonCreator
    public GetTaskPushNotificationConfigResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                 @JsonProperty("result") TaskPushNotificationConfig result,
                                                 @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, TaskPushNotificationConfig.class);
    }

    public GetTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetTaskPushNotificationConfigResponse(Object id, TaskPushNotificationConfig result) {
        this(null, id, result, null);
    }

}
