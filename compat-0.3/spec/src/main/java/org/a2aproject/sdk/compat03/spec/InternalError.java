package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;


/**
 * An error indicating an internal error on the server.
 */
public class InternalError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = A2AErrorCodes.INTERNAL_ERROR_CODE;

    public InternalError(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes.INTERNAL_ERROR_CODE),
                defaultIfNull(message, "Internal Error"),
                data);
    }

    public InternalError(String message) {
        this(null, message, null);
    }
}
