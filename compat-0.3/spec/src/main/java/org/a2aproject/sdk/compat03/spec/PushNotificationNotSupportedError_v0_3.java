package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

/**
 * An A2A-specific error indicating that the agent does not support push notifications.
 */
public class PushNotificationNotSupportedError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = -32003;

    public PushNotificationNotSupportedError_v0_3() {
        this(null, null, null);
    }

    public PushNotificationNotSupportedError_v0_3(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Push Notification is not supported"),
                data);
    }
}
