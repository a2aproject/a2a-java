package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a streaming JSON-RPC request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = StreamingJSONRPCRequestDeserializer.class)
public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits TaskResubscriptionRequest,
        SendStreamingMessageRequest {

}
