package org.a2aproject.sdk.compat03.common;

/**
 * A2A Protocol v0.3 specific headers.
 * These headers differ from the current protocol version.
 */
public final class A2AHeaders_v0_3 {

    /**
     * HTTP header name for A2A extensions in protocol v0.3.
     * Note: In current versions this is "A2A-Extensions" without the "X-" prefix.
     */
    public static final String X_A2A_EXTENSIONS = "X-A2A-Extensions";

    private A2AHeaders_v0_3() {
        // Utility class
    }
}
