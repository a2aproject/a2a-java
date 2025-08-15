package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC response for a streaming message request in the A2A protocol.
 * This response is returned after an agent processes a SendStreamingMessageRequest,
 * indicating the outcome of the streaming message initiation.
 *
 * <p>The response follows the JSON-RPC 2.0 specification and contains either
 * a successful result (StreamingEventKind) or an error. The result indicates the type
 * of streaming event that was generated as a result of initiating the streaming message.</p>
 *
 * <p>Unlike SendMessageResponse which contains a single EventKind, this streaming variant
 * contains a StreamingEventKind that represents the initial response to starting a
 * streaming conversation. Subsequent streaming responses will be delivered separately.</p>
 *
 * <p>Possible outcomes:</p>
 * <ul>
 *   <li>Success: Contains a StreamingEventKind result indicating streaming was initiated</li>
 *   <li>Error: Contains a JSONRPCError describing what went wrong during initiation</li>
 * </ul>
 *
 * <p>The StreamingEventKind result provides information about how the agent handled
 * the streaming request, such as whether it was accepted and streaming has begun.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendStreamingMessageResponse extends JSONRPCResponse<StreamingEventKind> {

    /**
     * Creates a new SendStreamingMessageResponse with full parameter specification.
     * 
     * @param jsonrpc the JSON-RPC protocol version (defaults to "2.0" if null)
     * @param id the response identifier matching the original request ID
     * @param result the successful result containing the streaming event kind (null if error occurred)
     * @param error the error information (null if successful)
     * @throws IllegalArgumentException if id is not null, string, or integer
     */
    @JsonCreator
    public SendStreamingMessageResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                        @JsonProperty("result") StreamingEventKind result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, StreamingEventKind.class);
    }

    /**
     * Creates a successful SendStreamingMessageResponse with the specified result.
     * Uses the default JSON-RPC version ("2.0") and sets error to null.
     * 
     * @param id the response identifier matching the original request ID
     * @param result the successful result containing the streaming event kind
     */
    public SendStreamingMessageResponse(Object id, StreamingEventKind result) {
        this(null, id, result, null);
    }

    /**
     * Creates an error SendStreamingMessageResponse with the specified error.
     * Uses the default JSON-RPC version ("2.0") and sets result to null.
     * 
     * @param id the response identifier matching the original request ID
     * @param error the error information describing what went wrong during streaming initiation
     */
    public SendStreamingMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
