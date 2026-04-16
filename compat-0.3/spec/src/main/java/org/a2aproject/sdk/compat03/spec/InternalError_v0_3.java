package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;


/**
 * An error indicating an internal error on the server.
 */
public class InternalError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = A2AErrorCodes_v0_3.INTERNAL_ERROR_CODE;

    public InternalError_v0_3(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes_v0_3.INTERNAL_ERROR_CODE),
                defaultIfNull(message, "Internal Error"),
                data);
    }

    public InternalError_v0_3(String message) {
        this(null, message, null);
    }
}
