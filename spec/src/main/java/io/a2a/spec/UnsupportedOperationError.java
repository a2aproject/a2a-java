package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A2A Protocol error indicating that the requested operation is not supported by the agent.
 * <p>
 * This error is returned when a client attempts an operation that is valid according to the
 * A2A Protocol but not implemented or enabled by the specific agent. This is distinct from
 * {@link MethodNotFoundError}, which indicates an unknown JSON-RPC method.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Calling streaming methods when {@link AgentCapabilities#streaming()} is false</li>
 *   <li>Attempting push notification operations when {@link AgentCapabilities#pushNotifications()} is false</li>
 *   <li>Using optional protocol features not implemented by the agent</li>
 *   <li>Agent-specific operations disabled by configuration</li>
 * </ul>
 * <p>
 * Corresponds to A2A-specific error code {@code -32004}.
 * <p>
 * Usage example:
 * <pre>{@code
 * if (!agentCard.capabilities().streaming()) {
 *     throw new UnsupportedOperationError();
 * }
 * }</pre>
 *
 * @see AgentCapabilities for agent capability declarations
 * @see MethodNotFoundError for unknown method errors
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnsupportedOperationError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32004;

    @JsonCreator
    public UnsupportedOperationError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data);
    }

    public UnsupportedOperationError() {
        this(null, null, null);
    }
}
