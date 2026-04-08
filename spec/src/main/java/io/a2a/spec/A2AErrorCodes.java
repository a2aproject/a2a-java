package io.a2a.spec;

import org.jspecify.annotations.Nullable;

/**
 * All the error codes for A2A errors.
 * <p>
 * Each constant provides:
 * <ul>
 *   <li>{@link #code()} - the JSON-RPC error code</li>
 *   <li>{@link #grpcStatus()} - the corresponding gRPC status name</li>
 *   <li>{@link #httpCode()} - the HTTP status code</li>
 * </ul>
 *
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification - Error Code Mappings</a>
 */
public enum A2AErrorCodes {

    /** Error code indicating the requested task was not found (-32001). */
    TASK_NOT_FOUND(-32001, "NOT_FOUND", 404),

    /** Error code indicating the task cannot be canceled in its current state (-32002). */
    TASK_NOT_CANCELABLE(-32002, "FAILED_PRECONDITION", 400),

    /** Error code indicating push notifications are not supported by this agent (-32003). */
    PUSH_NOTIFICATION_NOT_SUPPORTED(-32003, "FAILED_PRECONDITION", 400),

    /** Error code indicating the requested operation is not supported (-32004). */
    UNSUPPORTED_OPERATION(-32004, "UNIMPLEMENTED", 501),

    /** Error code indicating the content type is not supported (-32005). */
    CONTENT_TYPE_NOT_SUPPORTED(-32005, "INVALID_ARGUMENT", 400),

    /** Error code indicating the agent returned an invalid response (-32006). */
    INVALID_AGENT_RESPONSE(-32006, "INTERNAL", 500),

    /** Error code indicating extended agent card is not configured (-32007). */
    EXTENDED_AGENT_CARD_NOT_CONFIGURED(-32007, "FAILED_PRECONDITION", 400),

    /** Error code indicating client requested use of an extension marked as required: true in the Agent Card
     * but the client did not declare support for it in the request (-32008). */
    EXTENSION_SUPPORT_REQUIRED(-32008, "FAILED_PRECONDITION", 400),

    /** Error code indicating the A2A protocol version specified in the request (via A2A-Version service parameter)
     * is not supported by the agent (-32009). */
    VERSION_NOT_SUPPORTED(-32009, "UNIMPLEMENTED", 501),

    /** JSON-RPC error code for invalid request structure (-32600). */
    INVALID_REQUEST(-32600, "INVALID_ARGUMENT", 400),

    /** JSON-RPC error code for method not found (-32601). */
    METHOD_NOT_FOUND(-32601, "NOT_FOUND", 404),

    /** JSON-RPC error code for invalid method parameters (-32602). */
    INVALID_PARAMS(-32602, "INVALID_ARGUMENT", 422),

    /** JSON-RPC error code for internal server errors (-32603). */
    INTERNAL(-32603, "INTERNAL", 500),

    /** JSON-RPC error code for JSON parsing errors (-32700). */
    JSON_PARSE(-32700, "INVALID_ARGUMENT", 400);

    private final int code;
    private final String grpcStatus;
    private final int httpCode;

    A2AErrorCodes(int code, String grpcStatus, int httpCode) {
        this.code = code;
        this.grpcStatus = grpcStatus;
        this.httpCode = httpCode;
    }

    /**
     * Returns the JSON-RPC error code.
     *
     * @return the numeric error code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the corresponding gRPC status name.
     *
     * @return the gRPC status string (e.g., "NOT_FOUND", "INTERNAL")
     */
    public String grpcStatus() {
        return grpcStatus;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code
     */
    public int httpCode() {
        return httpCode;
    }

    /**
     * Looks up an error code enum constant by its JSON-RPC numeric code.
     *
     * @param code the JSON-RPC error code
     * @return the matching enum constant, or {@code null} if not found
     */
    public static @Nullable A2AErrorCodes fromCode(int code) {
        for (A2AErrorCodes e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

}
