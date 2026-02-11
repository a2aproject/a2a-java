package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * A2A Protocol error indicating incompatibility between requested content types and agent capabilities.
 * <p>
 * This error is returned when the input or output modes requested by a client are not supported
 * by the agent. Agents declare their supported content types via {@link AgentCard#defaultInputModes()}
 * and {@link AgentCard#defaultOutputModes()}, and clients can request specific modes via
 * {@link MessageSendConfiguration}.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Client requests audio input but agent only supports text</li>
 *   <li>Client requires video output but agent only produces text and images</li>
 *   <li>Incompatible combinations of input and output modes</li>
 * </ul>
 * <p>
 * Corresponds to A2A-specific error code {@code -32005}.
 * <p>
 * Usage example:
 * <pre>{@code
 * if (!agentCard.defaultInputModes().contains(requestedInputMode)) {
 *     throw new ContentTypeNotSupportedError(
 *         null,
 *         "Input mode " + requestedInputMode + " not supported",
 *         null
 *     );
 * }
 * }</pre>
 *
 * @see AgentCard#defaultInputModes() for agent input capabilities
 * @see AgentCard#defaultOutputModes() for agent output capabilities
 * @see MessageSendConfiguration for client content type preferences
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class ContentTypeNotSupportedError extends A2AProtocolError {

    /**
     * Constructs a content type not supported error.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public ContentTypeNotSupportedError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(defaultIfNull(code, CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE),
                defaultIfNull(message, "Incompatible content types"),
                data,
                "https://a2a-protocol.org/errors/content-type-not-supported");
    }
}
