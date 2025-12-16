package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.AUTHENTICATED_EXTENDED_CARD_NOT_CONFIGURED_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;


/**
 * A2A Protocol error indicating that the agent does not have an authenticated extended card configured.
 * <p>
 * This error is returned when a client attempts to retrieve an authenticated extended agent card
 * via {@link GetAuthenticatedExtendedCardRequest}, but the agent has not configured authentication-protected
 * extended card information.
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
 * if (authenticatedExtendedCard == null) {
 *     throw new AuthenticatedExtendedCardNotConfiguredError();
 * }
 * }</pre>
 *
 * @see GetAuthenticatedExtendedCardRequest for retrieving authenticated extended cards
 * @see AgentCard for the base agent card structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class AuthenticatedExtendedCardNotConfiguredError extends JSONRPCError {

    /**
     * Constructs an error for agents that don't support authenticated extended card retrieval.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public AuthenticatedExtendedCardNotConfiguredError(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, AUTHENTICATED_EXTENDED_CARD_NOT_CONFIGURED_ERROR_CODE),
                defaultIfNull(message, "Authenticated Extended Card not configured"),
                data);
    }
}
