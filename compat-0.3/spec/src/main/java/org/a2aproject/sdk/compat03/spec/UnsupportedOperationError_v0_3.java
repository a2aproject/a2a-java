package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

/**
 * An A2A-specific error indicating that the requested operation is not supported by the agent.
 */
public class UnsupportedOperationError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = A2AErrorCodes_v0_3.UNSUPPORTED_OPERATION_ERROR_CODE;

    public UnsupportedOperationError_v0_3(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes_v0_3.UNSUPPORTED_OPERATION_ERROR_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data);
    }

    public UnsupportedOperationError_v0_3() {
        this(null, null, null);
    }
}
