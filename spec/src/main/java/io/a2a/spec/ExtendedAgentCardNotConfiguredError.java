package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.EXTENDED_AGENT_CARD_NOT_CONFIGURED_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;


/**
 * A2A Protocol error indicating that the agent does not have an authenticated extended card configured.
 * <p>
 * This error is returned when a client attempts to retrieve an authenticated extended agent card,
 * but the agent has not configured authentication-protected extended card information.
 * <p>
 * Extended cards may contain additional agent metadata, capabilities, or configuration that
 * should only be accessible to authenticated clients. Agents that don't implement this feature
 * will return this error.
 * <p>
 * Corresponds to A2A-specific error code {@code -32007}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // In agent implementation
 * if (extendedAgentCard == null) {
 *     throw new ExtendedAgentCardNotConfiguredError();
 * }
 * }</pre>
 *
 * @see AgentCard for the base agent card structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class ExtendedAgentCardNotConfiguredError extends A2AProtocolError {

    /**
     * Constructs an error for agents that don't support authenticated extended card retrieval.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public ExtendedAgentCardNotConfiguredError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, EXTENDED_AGENT_CARD_NOT_CONFIGURED_ERROR_CODE),
                defaultIfNull(message, "Extended Card not configured"),
                data,
                "https://a2a-protocol.org/errors/extended-agent-card-not-configured");
    }
}
