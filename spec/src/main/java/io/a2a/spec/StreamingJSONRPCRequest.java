package io.a2a.spec;

/**
 * Represents a streaming JSON-RPC request.
 */

public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits TaskResubscriptionRequest,
        SendStreamingMessageRequest {

}
