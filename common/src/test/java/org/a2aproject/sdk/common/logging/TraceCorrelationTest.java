package org.a2aproject.sdk.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class TraceCorrelationTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        TraceCorrelation.clearMdc();
    }

    @Test
    void testGetTraceIdFromMDC() {
        MDC.put("trace_id", "trace-abc-123");
        assertEquals("trace-abc-123", TraceCorrelation.getTraceId());
    }

    @Test
    void testGetSpanIdFromMDC() {
        MDC.put("span_id", "span-xyz-789");
        assertEquals("span-xyz-789", TraceCorrelation.getSpanId());
    }

    @Test
    void testGetTraceIdAlternativeKeys() {
        MDC.put("traceId", "trace-alt");
        assertEquals("trace-alt", TraceCorrelation.getTraceId());
    }

    @Test
    void testNoTraceReturnsNull() {
        assertNull(TraceCorrelation.getTraceId());
        assertNull(TraceCorrelation.getSpanId());
    }

    @Test
    void testSetAndClearMdc() {
        TraceCorrelation.setMdc("t-123", "s-456");
        assertEquals("t-123", MDC.get("trace_id"));
        assertEquals("s-456", MDC.get("span_id"));
        TraceCorrelation.clearMdc();
        assertNull(MDC.get("trace_id"));
        assertNull(MDC.get("span_id"));
    }

    @Test
    void testSetMdcNullRemoves() {
        MDC.put("trace_id", "existing");
        TraceCorrelation.setMdc(null, null);
        assertNull(MDC.get("trace_id"));
        assertNull(MDC.get("traceId"));
    }
}
