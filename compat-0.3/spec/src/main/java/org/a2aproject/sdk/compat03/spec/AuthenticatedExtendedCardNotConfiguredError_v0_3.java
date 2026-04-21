package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;


/**
 * An A2A-specific error indicating that the agent does not have an
 * Authenticated Extended Card configured
 */
public class AuthenticatedExtendedCardNotConfiguredError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = -32007;

    public AuthenticatedExtendedCardNotConfiguredError_v0_3(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Authenticated Extended Card not configured"),
                data);
    }
}
