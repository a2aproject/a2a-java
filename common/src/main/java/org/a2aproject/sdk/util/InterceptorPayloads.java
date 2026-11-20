package org.a2aproject.sdk.util;

import org.jspecify.annotations.Nullable;

/** Type checks for payloads returned by client interceptors. */
public final class InterceptorPayloads {

    private InterceptorPayloads() {
    }

    /**
     * Casts a non-null interceptor payload.
     *
     * @param payload the interceptor payload
     * @param expectedType the expected payload type
     * @param <T> the expected payload type
     * @return the cast payload
     * @throws IllegalArgumentException if the payload has an unexpected type
     */
    public static <T> T cast(Object payload, Class<T> expectedType) {
        return Assert.checkNotNullParam("payload", castNullable(payload, expectedType));
    }

    /** Casts a payload while preserving null for transports that allow it. */
    public static <T> @Nullable T castNullable(@Nullable Object payload, Class<T> expectedType) {
        if (payload != null && !expectedType.isInstance(payload)) {
            throw new IllegalArgumentException("Interceptor payload must be " + expectedType.getName()
                    + "; got " + payload.getClass().getName());
        }
        return expectedType.cast(payload);
    }
}
