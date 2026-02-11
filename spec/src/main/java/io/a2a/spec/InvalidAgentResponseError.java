package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;


/**
 * A2A Protocol error indicating that an agent returned a response not conforming to protocol specifications.
 * <p>
 * This error is typically raised by client implementations when validating agent responses.
 * It indicates that the agent's response structure, content, or format violates the A2A Protocol
 * requirements for the invoked method.
 * <p>
 * Common violations:
 * <ul>
 * <li>Missing required fields in response objects</li>
 * <li>Invalid field types or values</li>
 * <li>Malformed event stream data</li>
 * <li>Response doesn't match declared agent capabilities</li>
 * </ul>
 * <p>
 * Corresponds to A2A-specific error code {@code -32006}.
 * <p>
 * Usage example:
 * <pre>{@code
 * SendMessageResponse response = client.sendMessage(request);
 * if (response.task() == null) {
 *     throw new InvalidAgentResponseError(
 *         null,
 *         "Response missing required 'task' field",
 *         null
 *     );
 * }
 * }</pre>
 *
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class InvalidAgentResponseError extends A2AProtocolError {

    /**
     * Constructs an invalid agent response error.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public InvalidAgentResponseError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, INVALID_AGENT_RESPONSE_ERROR_CODE),
                defaultIfNull(message, "Invalid agent response"),
                data,
                "https://a2a-protocol.org/errors/invalid-agent-response");
    }
}
