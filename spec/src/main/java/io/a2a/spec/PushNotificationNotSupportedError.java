package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An A2A-specific error indicating that the agent does not support push notifications.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushNotificationNotSupportedError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32003;

    public PushNotificationNotSupportedError() {
        this(null, null, null);
    }

    @JsonCreator
    public PushNotificationNotSupportedError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Push Notification is not supported"),
                data);
    }
}
