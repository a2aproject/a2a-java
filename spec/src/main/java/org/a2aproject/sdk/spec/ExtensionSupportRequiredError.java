package org.a2aproject.sdk.spec;

import static org.a2aproject.sdk.util.Utils.defaultIfNull;

import java.util.Map;

import org.jspecify.annotations.Nullable;


/**
 * A2A Protocol error indicating that a client requested use of an extension marked as required
 * but did not declare support for it.
 * <p>
 * This error is returned when a client attempts to use a feature or capability that requires
 * a specific extension, but the client has not declared support for that extension in the request.
 * Extensions marked as {@code required: true} in the Agent Card must be explicitly supported
 * by the client.
 * <p>
 * Corresponds to A2A-specific error code {@code -32008}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // In agent implementation
 * if (requiredExtension && !clientSupportsExtension) {
 *     throw new ExtensionSupportRequiredError(null,
 *         "Extension 'custom-auth' is required but not supported by client", null);
 * }
 * }</pre>
 *
 * @see AgentCard for extension declarations
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class ExtensionSupportRequiredError extends A2AProtocolError {

    /**
     * Constructs an error when a client requests a required extension without declaring support.
     *
     * @param code the error code (defaults to -32008 if null)
     * @param message the error message (defaults to standard message if null)
     * @param details additional error details (optional)
     */
    public ExtensionSupportRequiredError(@Nullable Integer code, @Nullable String message, @Nullable Map<String, Object> details) {
        super(
                defaultIfNull(code, A2AErrorCodes.EXTENSION_SUPPORT_REQUIRED.code()),
                defaultIfNull(message, "Extension support required but not declared"),
                details);
    }
}
