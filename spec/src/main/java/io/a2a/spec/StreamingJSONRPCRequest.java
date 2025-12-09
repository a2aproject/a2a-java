package io.a2a.spec;

/**
 * Base class for JSON-RPC requests that support streaming responses in the A2A Protocol.
 * <p>
 * This sealed class extends {@link JSONRPCRequest} to provide specialized support for
 * A2A Protocol methods that return streaming responses. Streaming requests enable
 * server-sent events and real-time updates to be pushed to clients as they occur,
 * rather than waiting for a complete response.
 * <p>
 * The A2A Protocol defines two primary streaming operations:
 * <ul>
 *   <li>{@link SendStreamingMessageRequest} - Stream task execution events in real-time</li>
 *   <li>{@link SubscribeToTaskRequest} - Resubscribe to events from an existing task</li>
 * </ul>
 * <p>
 * Streaming requests follow the JSON-RPC 2.0 specification structure but the response
 * is delivered as a stream of {@link StreamingEventKind} objects rather than a single
 * response message.
 * <p>
 * This class uses a custom deserializer to properly handle polymorphic deserialization
 * of streaming request types.
 *
 * @param <T> the type of the params object for this streaming request
 * @see SendStreamingMessageRequest
 * @see SubscribeToTaskRequest
 * @see StreamingEventKind
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public abstract sealed class StreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits SubscribeToTaskRequest,
        SendStreamingMessageRequest {

}
