package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a non-streaming JSON-RPC request.
 */
public abstract sealed class NonStreamingJSONRPCRequest_v0_3<T> extends JSONRPCRequest_v0_3<T> permits GetTaskRequest_v0_3,
        CancelTaskRequest_v0_3, SetTaskPushNotificationConfigRequest_v0_3, GetTaskPushNotificationConfigRequest_v0_3,
        SendMessageRequest_v0_3, DeleteTaskPushNotificationConfigRequest_v0_3, ListTaskPushNotificationConfigRequest_v0_3,
        GetAuthenticatedExtendedCardRequest_v0_3 {
}
