package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.VERSION_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;


/**
 * A2A Protocol error indicating that the A2A protocol version specified in the request
 * is not supported by the agent.
 * <p>
 * This error is returned when a client specifies a protocol version (via the A2A-Version
 * service parameter) that the agent does not support. Agents should return this error
 * when they cannot handle the requested protocol version.
 * <p>
 * Corresponds to A2A-specific error code {@code -32009}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // In agent implementation
 * if (!isSupportedVersion(requestedVersion)) {
 *     throw new VersionNotSupportedError(null,
 *         "Protocol version " + requestedVersion + " is not supported", null);
 * }
 * }</pre>
 *
 * @see AgentInterface#protocolVersion() for supported version declaration
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class VersionNotSupportedError extends A2AProtocolError {

    /**
     * Constructs an error when the requested protocol version is not supported.
     *
     * @param code the error code (defaults to -32009 if null)
     * @param message the error message (defaults to standard message if null)
     * @param data additional error data (optional)
     */
    public VersionNotSupportedError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, VERSION_NOT_SUPPORTED_ERROR_CODE),
                defaultIfNull(message, "Protocol version not supported"),
                data,
                "https://a2a-protocol.org/errors/version-not-supported");
    }
}
