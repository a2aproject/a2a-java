package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

/**
 * An A2A-specific error indicating that the agent does not support push notifications.
 */
public class PushNotificationNotSupportedError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32003;

    public PushNotificationNotSupportedError() {
        this(null, null, null);
    }

    public PushNotificationNotSupportedError(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Push Notification is not supported"),
                data);
    }
}
