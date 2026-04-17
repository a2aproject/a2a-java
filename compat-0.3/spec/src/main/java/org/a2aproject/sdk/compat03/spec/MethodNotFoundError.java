package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

/**
 * An error indicating that the requested method does not exist or is not available.
 */
public class MethodNotFoundError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = METHOD_NOT_FOUND_ERROR_CODE;

    public MethodNotFoundError(Integer code, String message, Object data) {
        super(defaultIfNull(code, METHOD_NOT_FOUND_ERROR_CODE), defaultIfNull(message, "Method not found"), data);
    }

    public MethodNotFoundError() {
        this(METHOD_NOT_FOUND_ERROR_CODE, null, null);
    }
}
