package org.a2aproject.sdk.common.logging;

/**
 * Utility for truncating payloads to a configurable size with a marker.
 * <p>
 * Truncation is based on character length (not bytes) for simplicity and
 * consistency with typical string handling. The truncated marker
 * {@value #TRUNCATED_MARKER} is appended when truncation occurs.
 * </p>
 */
public final class PayloadTruncator {

    /**
     * Marker appended when payload is truncated.
     */
    public static final String TRUNCATED_MARKER = " [truncated]";

    private PayloadTruncator() {
    }

    /**
     * Truncates the payload if it exceeds maxSize.
     *
     * @param payload the payload string, may be null
     * @param maxSize maximum allowed size in characters; if &lt;=0, no truncation
     * @return truncated payload with marker, or original if within limit; null if input null
     */
    public static String truncate(String payload, int maxSize) {
        if (payload == null) {
            return null;
        }
        if (maxSize <= 0 || payload.length() <= maxSize) {
            return payload;
        }
        return payload.substring(0, maxSize) + TRUNCATED_MARKER;
    }

    /**
     * Checks if payload was truncated.
     *
     * @param payload the payload to check
     * @return true if payload ends with truncated marker
     */
    public static boolean isTruncated(String payload) {
        return payload != null && payload.endsWith(TRUNCATED_MARKER);
    }
}
