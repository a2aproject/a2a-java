package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

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
