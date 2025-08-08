package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC error indicating that push notifications are not supported.
 * This error is returned when a client attempts to configure or use push notification
 * functionality on an agent that does not support this feature.
 *
 * <p>This error follows the JSON-RPC 2.0 error specification and uses a specific
 * error code (-32003) to identify push notification support issues. It extends
 * the base JSONRPCError class to provide structured error information.</p>
 *
 * <p>Common scenarios where this error occurs:</p>
 * <ul>
 *   <li>Attempting to set task push notification configuration on an unsupported agent</li>
 *   <li>Trying to enable push notifications when the agent lacks the capability</li>
 *   <li>Requesting push notification features in environments where they're disabled</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushNotificationNotSupportedError extends JSONRPCError {

    /** The default error code for push notification not supported errors */
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
