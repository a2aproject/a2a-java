package org.a2aproject.sdk.common.logging;

public final class PayloadTruncator {
    public static final String TRUNCATED_SUFFIX = " [truncated]";
    private static final int SUFFIX_LEN = TRUNCATED_SUFFIX.length();
    private PayloadTruncator() {}
    public static String truncate(String payload, int maxSize) {
        if (payload == null) return null;
        if (maxSize <= 0) return TRUNCATED_SUFFIX;
        if (payload.length() <= maxSize) return payload;
        int cut = Math.max(0, maxSize - SUFFIX_LEN);
        return payload.substring(0, cut) + TRUNCATED_SUFFIX;
    }
}
