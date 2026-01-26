package io.a2a.jsonrpc.common.wrappers;

/**
 * Represents a non-streaming JSON-RPC request.
 *
 * @param <T> the type of the request parameters
 */
public abstract sealed class NonStreamingJSONRPCRequest<T> extends A2ARequest<T> permits GetTaskRequest,
        CancelTaskRequest, CreateTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        SendMessageRequest, DeleteTaskPushNotificationConfigRequest, ListTaskPushNotificationConfigRequest,
        GetExtendedAgentCardRequest, ListTasksRequest {

    NonStreamingJSONRPCRequest(String jsonrpc, String method, Object id, T params) {
        validateAndSetJsonParameters(jsonrpc, method, id, params, true);
    }

    NonStreamingJSONRPCRequest(String jsonrpc, String method, Object id) {
        validateAndSetJsonParameters(jsonrpc, method, id, null, false);
    }
}
