package org.a2aproject.sdk.common;

/**
 * Common A2A protocol headers and constants.
 */
public final class A2AHeaders {
    
    /**
     * HTTP header name for A2A protocol version.
     * Used to communicate the protocol version that the client is using.
     */
    public static final String A2A_VERSION = "A2A-Version";

    /**
     * HTTP header name for A2A extensions.
     * Used to communicate which extensions are requested by the client.
     */
    public static final String A2A_EXTENSIONS = "A2A-Extensions";

    /**
     * HTTP header name for a push notification token.
     */
    public static final String X_A2A_NOTIFICATION_TOKEN = "X-A2A-Notification-Token";

    /**
     * gRPC metadata header name identifying the target agent ID in a multi-agent deployment.
     * Used by transports without per-path routing (e.g. gRPC) to select which agent should
     * handle the call.
     */
    public static final String X_A2A_AGENT_ID = "X-A2A-Agent-Id";

    private A2AHeaders() {
        // Utility class
    }
}
