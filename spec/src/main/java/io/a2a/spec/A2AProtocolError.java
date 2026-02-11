package io.a2a.spec;

import org.jspecify.annotations.Nullable;

/**
 * Represents a protocol-level error in the A2A Protocol with a reference URL.
 * <p>
 * This error extends {@link A2AError} to include a URL field that provides a reference
 * to documentation, specification details, or additional context about the protocol error.
 * This is particularly useful for protocol version mismatches, unsupported features, or
 * other situations where pointing to the protocol specification would help diagnose the issue.
 *
 * @see A2AError for the base error implementation
 */
public class A2AProtocolError extends A2AError {

    /**
     * URL reference for additional information about this protocol error.
     */
    private final @Nullable String url;

    /**
     * Constructs a protocol error with the specified code, message, data, and reference URL.
     *
     * @param code the numeric error code (required, see JSON-RPC 2.0 spec for standard codes)
     * @param message the human-readable error message (required)
     * @param data additional error information, structure defined by the error code (optional)
     * @param url URL reference providing additional context about this protocol error (optional)
     */
    public A2AProtocolError(Integer code, String message, @Nullable Object data, @Nullable String url) {
        super(code, message, data);
        this.url = url;
    }

    /**
     * Gets the URL reference for additional information about this protocol error.
     * <p>
     * This URL typically points to protocol specification documentation or other resources
     * that provide context about the error condition.
     *
     * @return the reference URL, or null if not provided
     */
    public @Nullable String getUrl() {
        return url;
    }
}
