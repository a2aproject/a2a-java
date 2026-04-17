package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a non-streaming JSON-RPC request.
 */
public abstract sealed class NonStreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits GetTaskRequest,
        CancelTaskRequest, SetTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        SendMessageRequest, DeleteTaskPushNotificationConfigRequest, ListTaskPushNotificationConfigRequest,
        GetAuthenticatedExtendedCardRequest {
}
