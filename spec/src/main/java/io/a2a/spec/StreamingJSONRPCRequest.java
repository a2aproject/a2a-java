package io.a2a.spec;

/**
 * Represents a streaming JSON-RPC request.
 *
 * @param <T> the type of the request parameters
 */
public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits TaskResubscriptionRequest,
        SendStreamingMessageRequest {

}
