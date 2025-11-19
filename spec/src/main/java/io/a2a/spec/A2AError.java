package io.a2a.spec;

/**
 * Marker interface for A2A Protocol error events.
 * <p>
 * This interface extends {@link Event} to allow errors to be transmitted as events
 * in the A2A Protocol's event stream. All protocol-level errors implement this interface,
 * enabling uniform error handling across both streaming and non-streaming communication.
 * <p>
 * A2A errors typically extend {@link JSONRPCError} to provide JSON-RPC 2.0 compliant
 * error responses with standard error codes, messages, and optional additional data.
 * <p>
 * Common implementations include:
 * <ul>
 *   <li>{@link InvalidParamsError} - Invalid method parameters</li>
 *   <li>{@link InvalidRequestError} - Malformed request</li>
 *   <li>{@link MethodNotFoundError} - Unknown method</li>
 *   <li>{@link InternalError} - Server-side error</li>
 *   <li>{@link TaskNotFoundError} - Task does not exist</li>
 *   <li>And others for specific protocol error conditions</li>
 * </ul>
 *
 * @see Event for the base event interface
 * @see JSONRPCError for the base error implementation
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public interface A2AError extends Event {
}
