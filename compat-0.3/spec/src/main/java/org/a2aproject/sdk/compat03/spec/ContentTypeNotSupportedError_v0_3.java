package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;


/**
 * An A2A-specific error indicating an incompatibility between the requested
 * content types and the agent's capabilities.
 */
public class ContentTypeNotSupportedError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;

    public ContentTypeNotSupportedError_v0_3(Integer code, String message, Object data) {
        super(defaultIfNull(code, CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE),
                defaultIfNull(message, "Incompatible content types"),
                data);
    }
}
