package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a non-streaming JSON-RPC request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = NonStreamingJSONRPCRequestDeserializer.class)
public abstract sealed class NonStreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits GetTaskRequest,
        CancelTaskRequest, SetTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        SendMessageRequest, DeleteTaskPushNotificationConfigRequest, ListTaskPushNotificationConfigRequest,
        GetAuthenticatedExtendedCardRequest {
}
