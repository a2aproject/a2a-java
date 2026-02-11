package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

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
public class UnsupportedOperationError extends A2AProtocolError {

    /**
     * Constructs error with all parameters.
     *
     * @param code the error code (defaults to -32004 if null)
     * @param message the error message (defaults to "This operation is not supported" if null)
     * @param data additional error data (optional)
     */
    public UnsupportedOperationError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, UNSUPPORTED_OPERATION_ERROR_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data,
                "https://a2a-protocol.org/errors/unsupported-operation");
    }

    /**
     * Constructs error with default message.
     */
    public UnsupportedOperationError() {
        this(null, null, null);
    }
}
