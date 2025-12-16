package io.a2a.spec;

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
public final class SendStreamingMessageResponse extends JSONRPCResponse<StreamingEventKind> {

    /**


     * Constructs response with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param result the result


     * @param error the error if any


     */


    public SendStreamingMessageResponse(String jsonrpc, Object id, StreamingEventKind result, JSONRPCError error) {
        super(jsonrpc, id, result, error, StreamingEventKind.class);
    }

    /**


     * Constructs successful response.


     *


     * @param id the request ID


     * @param result the result


     */


    public SendStreamingMessageResponse(Object id, StreamingEventKind result) {
        this(null, id, result, null);
    }

    /**


     * Constructs error response.


     *


     * @param id the request ID


     * @param error the error


     */


    public SendStreamingMessageResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
