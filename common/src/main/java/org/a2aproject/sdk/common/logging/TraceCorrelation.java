package org.a2aproject.sdk.common.logging;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

public final class TraceCorrelation {
    private TraceCorrelation() {}
    public static final String TRACE_ID_KEY = "trace_id";
    public static final String SPAN_ID_KEY = "span_id";

    @Nullable
    public static String currentTraceId() {
        String t = tryOtelTraceId();
        if (t != null && !t.isEmpty()) return t;
        t = MDC.get("trace_id"); if (t != null) return t;
        t = MDC.get("traceId"); if (t != null) return t;
        return MDC.get("X-B3-TraceId");
    }

    @Nullable
    public static String currentSpanId() {
        String s = tryOtelSpanId();
        if (s != null && !s.isEmpty()) return s;
        s = MDC.get("span_id"); if (s != null) return s;
        s = MDC.get("spanId"); if (s != null) return s;
        return MDC.get("X-B3-SpanId");
    }

    @Nullable
    private static String tryOtelTraceId() {
        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Object span = spanClass.getMethod("current").invoke(null);
            Object ctx = span.getClass().getMethod("getSpanContext").invoke(span);
            Boolean valid = (Boolean) ctx.getClass().getMethod("isValid").invoke(ctx);
            if (valid == null || !valid) return null;
            String id = (String) ctx.getClass().getMethod("getTraceId").invoke(ctx);
            if (id != null && !id.equals("00000000000000000000000000000000")) return id;
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static String tryOtelSpanId() {
        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Object span = spanClass.getMethod("current").invoke(null);
            Object ctx = span.getClass().getMethod("getSpanContext").invoke(span);
            Boolean valid = (Boolean) ctx.getClass().getMethod("isValid").invoke(ctx);
            if (valid == null || !valid) return null;
            String id = (String) ctx.getClass().getMethod("getSpanId").invoke(ctx);
            if (id != null && !id.equals("0000000000000000")) return id;
        } catch (Exception ignored) {}
        return null;
    }

    public static void setMdc(@Nullable String traceId, @Nullable String spanId) {
        if (traceId != null) MDC.put(TRACE_ID_KEY, traceId);
        if (spanId != null) MDC.put(SPAN_ID_KEY, spanId);
    }

    public static void clearMdc() {
        MDC.remove(TRACE_ID_KEY); MDC.remove(SPAN_ID_KEY);
    }
}
