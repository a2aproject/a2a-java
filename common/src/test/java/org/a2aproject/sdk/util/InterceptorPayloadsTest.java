package org.a2aproject.sdk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InterceptorPayloadsTest {

    @Test
    void castsCompatiblePayloadsAndPreservesNull() {
        String payload = "request";
        assertSame(payload, InterceptorPayloads.cast(payload, CharSequence.class));
        assertNull(InterceptorPayloads.castNullable(null, CharSequence.class));
    }

    @Test
    void describesUnexpectedPayloadType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InterceptorPayloads.cast(42, CharSequence.class));
        assertEquals("Interceptor payload must be java.lang.CharSequence; got java.lang.Integer", exception.getMessage());
    }
}
