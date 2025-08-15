package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC response for a message send request in the A2A protocol.
 * This response is returned after an agent processes a SendMessageRequest,
 * indicating the outcome of the message delivery attempt.
 *
 * <p>The response follows the JSON-RPC 2.0 specification and contains either
 * a successful result (EventKind) or an error. The result indicates the type
 * of event that was generated as a result of sending the message.</p>
 *
 * <p>Possible outcomes:</p>
 * <ul>
 *   <li>Success: Contains an EventKind result indicating the message was processed</li>
 *   <li>Error: Contains a JSONRPCError describing what went wrong</li>
 * </ul>
 *
 * <p>The EventKind result provides information about how the agent handled
 * the message, such as whether it was accepted, queued, or processed immediately.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendMessageResponse extends JSONRPCResponse<EventKind> {

    /**
     * Creates a new SendMessageResponse with full parameter specification.
     * 
     * @param jsonrpc the JSON-RPC protocol version (defaults to "2.0" if null)
     * @param id the response identifier matching the original request ID
     * @param result the successful result containing the event kind (null if error occurred)
     * @param error the error information (null if successful)
     * @throws IllegalArgumentException if id is not null, string, or integer
     */
    @JsonCreator
    public SendMessageResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                               @JsonProperty("result") EventKind result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, EventKind.class);
    }

    /**
     * Creates a successful SendMessageResponse with the specified result.
     * Uses the default JSON-RPC version ("2.0") and sets error to null.
     * 
     * @param id the response identifier matching the original request ID
     * @param result the successful result containing the event kind
     */
    public SendMessageResponse(Object id, EventKind result) {
        this(null, id, result, null);
    }

    /**
     * Creates an error SendMessageResponse with the specified error.
     * Uses the default JSON-RPC version ("2.0") and sets result to null.
     * 
     * @param id the response identifier matching the original request ID
     * @param error the error information describing what went wrong
     */
    public SendMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
