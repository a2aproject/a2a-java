package io.a2a.spec;


import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 Error object, included in an error response.
 */
public class JSONRPCError extends Error implements Event, A2AError {

    private final Integer code;
    private final Object data;

    /**
     * Constructs a JSON-RPC error with the specified code, message, and optional data.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     *
     * @param code the numeric error code (required, see JSON-RPC 2.0 spec for standard codes)
     * @param message the human-readable error message (required)
     * @param data additional error information, structure defined by the error code (optional)
     * @throws IllegalArgumentException if code or message is null
     */
    public JSONRPCError(Integer code, String message, Object data) {
        super(message);
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.data = data;
    }

    /**
     * Gets the error code
     *
     * @return the error code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Gets the data associated with the error.
     *
     * @return the data. May be {@code null}
     */
    public Object getData() {
        return data;
    }
}
