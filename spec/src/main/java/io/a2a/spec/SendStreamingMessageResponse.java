package io.a2a.spec;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC response for streaming message initiation requests.
 * <p>
 * This response is sent after receiving a {@link SendStreamingMessageRequest} and contains
 * a stream of {@link StreamingEventKind} events representing the agent's processing progress.
 * Unlike non-streaming responses, this provides real-time updates as the agent works.
 * <p>
 * The result field contains events such as {@link Task}, {@link TaskStatusUpdateEvent},
 * {@link TaskArtifactUpdateEvent}, and {@link Message} as they are produced by the agent.
 * <p>
 * If an error occurs during request processing, the error field will be populated with
 * a {@link JSONRPCError} instead of streaming events.
 *
 * @see SendStreamingMessageRequest for the corresponding request
 * @see StreamingEventKind for the types of events that can be streamed
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendStreamingMessageResponse extends JSONRPCResponse<StreamingEventKind> {

    @JsonCreator
    public SendStreamingMessageResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                        @JsonProperty("result") StreamingEventKind result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, StreamingEventKind.class);
    }

    public SendStreamingMessageResponse(Object id, StreamingEventKind result) {
        this(null, id, result, null);
    }

    public SendStreamingMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
