package org.a2aproject.sdk.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PayloadTruncatorTest {

    @Test
    public void testTruncateWithinLimit() {
        String payload = "short payload";
        assertEquals(payload, PayloadTruncator.truncate(payload, 4096));
        assertFalse(PayloadTruncator.isTruncated(PayloadTruncator.truncate(payload, 4096)));
    }

    @Test
    public void testTruncateExceedsLimit() {
        String payload = "a".repeat(5000);
        String truncated = PayloadTruncator.truncate(payload, 4096);
        assertTrue(truncated.endsWith(PayloadTruncator.TRUNCATED_MARKER));
        assertTrue(truncated.length() == 4096 + PayloadTruncator.TRUNCATED_MARKER.length());
        assertTrue(PayloadTruncator.isTruncated(truncated));
    }

    @Test
    public void testTruncateExactLimit() {
        String payload = "a".repeat(4096);
        String result = PayloadTruncator.truncate(payload, 4096);
        assertEquals(payload, result);
        assertFalse(PayloadTruncator.isTruncated(result));
    }

    @Test
    public void testTruncateNull() {
        assertNull(PayloadTruncator.truncate(null, 4096));
    }

    @Test
    public void testTruncateZeroMaxSize() {
        String payload = "test";
        assertEquals(payload, PayloadTruncator.truncate(payload, 0));
    }

    @Test
    public void testIsTruncatedNull() {
        assertFalse(PayloadTruncator.isTruncated(null));
    }

    @Test
    public void testDefaultMaxSize() {
        String payload = "a".repeat(4097);
        String truncated = PayloadTruncator.truncate(payload, A2APayloadLoggingConfig.DEFAULT_MAX_SIZE);
        assertTrue(PayloadTruncator.isTruncated(truncated));
    }
}
