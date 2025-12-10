package io.a2a.spec;

/**
 * Represents a non-streaming JSON-RPC request.
 *
 * @param <T> the type of the request parameters
 */
public abstract sealed class NonStreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits GetTaskRequest,
        CancelTaskRequest, SetTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        SendMessageRequest, DeleteTaskPushNotificationConfigRequest, ListTaskPushNotificationConfigRequest,
        GetAuthenticatedExtendedCardRequest, ListTasksRequest {
}
