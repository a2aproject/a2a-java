package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Abstract base class for streaming JSON-RPC 2.0 requests in the A2A protocol.
 * Streaming requests are designed to handle operations that may produce continuous
 * or real-time data streams, such as task resubscription or streaming message sending.
 *
 * <p>This class extends the base JSONRPCRequest and provides specialized handling
 * for streaming operations. Streaming requests typically involve:</p>
 * <ul>
 *   <li>Long-running operations that produce incremental results</li>
 *   <li>Real-time data transmission</li>
 *   <li>Continuous monitoring or subscription-based operations</li>
 * </ul>
 *
 * <p>The class is sealed and only permits specific implementations:
 * TaskResubscriptionRequest and SendStreamingMessageRequest, ensuring type safety
 * and controlled extensibility.</p>
 *
 * <p>Deserialization is handled by the StreamingJSONRPCRequestDeserializer to
 * properly route incoming JSON to the correct concrete implementation.</p>
 *
 * @param <T> the type of the request parameters
 * @see JSONRPCRequest
 * @see TaskResubscriptionRequest
 * @see SendStreamingMessageRequest
 * @see StreamingJSONRPCRequestDeserializer
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = StreamingJSONRPCRequestDeserializer.class)
public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits TaskResubscriptionRequest,
        SendStreamingMessageRequest {

}
