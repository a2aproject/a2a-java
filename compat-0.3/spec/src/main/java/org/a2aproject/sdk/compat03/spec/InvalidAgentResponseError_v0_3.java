package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;


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
 * @see JSONRPCResponse_v0_3 for response structure
 * @see SendMessageResponse_v0_3 for message send response
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>

 */
public class InvalidAgentResponseError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = A2AErrorCodes_v0_3.INVALID_AGENT_RESPONSE_ERROR_CODE;

    public InvalidAgentResponseError_v0_3(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes_v0_3.INVALID_AGENT_RESPONSE_ERROR_CODE),
                defaultIfNull(message, "Invalid agent response"),
                data);
    }
}
