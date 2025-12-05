package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A2A Protocol error indicating that an agent returned a response not conforming to protocol specifications.
 * <p>
 * This error is typically raised by client implementations when validating agent responses.
 * It indicates that the agent's response structure, content, or format violates the A2A Protocol
 * requirements for the invoked method.
 * <p>
 * Common violations:
 * <ul>
 *   <li>Missing required fields in response objects</li>
 *   <li>Invalid field types or values</li>
 *   <li>Malformed event stream data</li>
 *   <li>Response doesn't match declared agent capabilities</li>
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
 * @see JSONRPCResponse for response structure
 * @see SendMessageResponse for message send response
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidAgentResponseError extends JSONRPCError {

    @JsonCreator
    public InvalidAgentResponseError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, INVALID_AGENT_RESPONSE_ERROR_CODE),
                defaultIfNull(message, "Invalid agent response"),
                data);
    }
}
