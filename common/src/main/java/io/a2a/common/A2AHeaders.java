package io.a2a.common;

/**
 * Common A2A protocol headers and constants.
 */
public final class A2AHeaders {
    
    /**
     * HTTP header name for A2A extensions.
     * Used to communicate which extensions are requested by the client.
     */
    public static final String X_A2A_EXTENSIONS = "X-A2A-Extensions";

    /**
     * HTTP header name for a push notification token.
     */
    public static final String X_A2A_NOTIFICATION_TOKEN = "X-A2A-Notification-Token";
    
    private A2AHeaders() {
        // Utility class
    }
}
