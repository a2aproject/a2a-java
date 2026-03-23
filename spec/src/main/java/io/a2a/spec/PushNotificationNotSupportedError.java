package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A2A Protocol error indicating that the agent does not support push notifications.
 * <p>
 * This error is returned when a client attempts push notification operations
 * on an agent that has {@link AgentCapabilities#pushNotifications()} set to {@code false}.
 * <p>
 * Push notifications allow agents to proactively send task updates to clients via
 * HTTP callbacks. Agents that don't support this capability will return this error
 * for all push notification-related methods.
 * <p>
 * Corresponds to A2A-specific error code {@code -32003}.
 * <p>
 * Usage example:
 * <pre>{@code
 * if (!agentCard.capabilities().pushNotifications()) {
 *     throw new PushNotificationNotSupportedError();
 * }
 * }</pre>
 *
 * @see AgentCapabilities#pushNotifications() for push notification capability
 * @see TaskPushNotificationConfig for push notification configuration
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class PushNotificationNotSupportedError extends A2AProtocolError {

    /**
     * Constructs error with default message.
     */
    public PushNotificationNotSupportedError() {
        this(null, null, null);
    }

    /**
     * Constructs error with all parameters.
     *
     * @param code the error code (defaults to -32003 if null)
     * @param message the error message (defaults to "Push Notification is not supported" if null)
     * @param details additional error details (optional)
     */
    public PushNotificationNotSupportedError(@Nullable Integer code, @Nullable String message, @Nullable Map<String, Object> details) {
        super(
                defaultIfNull(code, A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED.code()),
                defaultIfNull(message, "Push Notification is not supported"),
                details);
    }
}
