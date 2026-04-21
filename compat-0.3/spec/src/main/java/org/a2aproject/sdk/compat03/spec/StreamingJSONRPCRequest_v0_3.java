package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a streaming JSON-RPC request.
 */

public abstract sealed class StreamingJSONRPCRequest_v0_3<T> extends JSONRPCRequest_v0_3<T> permits TaskResubscriptionRequest_v0_3,
        SendStreamingMessageRequest_v0_3 {

}
