package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * An A2A-specific error indicating that the requested operation is not supported by the agent.
 */
public class UnsupportedOperationError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE;

    public UnsupportedOperationError(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data);
    }

    public UnsupportedOperationError() {
        this(null, null, null);
    }
}
