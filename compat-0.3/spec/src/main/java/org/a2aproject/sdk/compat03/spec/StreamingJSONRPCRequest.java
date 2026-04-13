package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a streaming JSON-RPC request.
 */

public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits TaskResubscriptionRequest,
        SendStreamingMessageRequest {

}
