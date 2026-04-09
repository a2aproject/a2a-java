package org.a2aproject.sdk.spec;

import static org.a2aproject.sdk.util.Utils.defaultIfNull;

import java.util.Map;

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

    public ContentTypeNotSupportedError() {
        this(null, null, null);
    }
    /**
     * Constructs a content type not supported error.
     *
     * @param code the error code
     * @param message the error message
     * @param details additional error details
     */
    public ContentTypeNotSupportedError(@Nullable Integer code, @Nullable String message, @Nullable Map<String, Object> details) {
        super(defaultIfNull(code, A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED.code()),
                defaultIfNull(message, "Incompatible content types"),
                details);
    }
}
