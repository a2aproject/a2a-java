package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;


/**
 * An A2A-specific error indicating that the agent does not have an
 * Authenticated Extended Card configured
 */
public class AuthenticatedExtendedCardNotConfiguredError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32007;

    public AuthenticatedExtendedCardNotConfiguredError(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Authenticated Extended Card not configured"),
                data);
    }
}
