package org.a2aproject.sdk.common.logging;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

/**
 * Utility to correlate log entries with traces via trace_id and span_id.
 * <p>
 * Tries to retrieve trace context from:
 * <ol>
 *   <li>OpenTelemetry API via reflection (if on classpath): {@code io.opentelemetry.api.trace.Span.current().getSpanContext()}</li>
 *   <li>SLF4J MDC keys: {@code traceId}, {@code trace_id}, {@code traceID}, {@code X-B3-TraceId}</li>
 *   <li>Similarly for span: {@code spanId}, {@code span_id}</li>
 * </ol>
 * </p>
 * <p>
 * Reflection is used to avoid a hard dependency on OpenTelemetry. If OTel is not
 * on the classpath, the lookup gracefully falls back to MDC.
 * </p>
 */
public final class TraceCorrelation {

    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000";
    private static final String INVALID_SPAN_ID = "0000000000000000";

    private TraceCorrelation() {
    }

    /**
     * Retrieves current trace ID from OpenTelemetry or MDC.
     *
     * @return trace ID or null if not available
     */
    public static @Nullable String getTraceId() {
        String traceId = getTraceIdFromOtel();
        if (traceId != null && !traceId.isBlank() && !INVALID_TRACE_ID.equals(traceId)) {
            return traceId;
        }
        return getFromMdc("traceId", "trace_id", "traceID", "X-B3-TraceId", "X-B3-TraceID", "trace-id");
    }

    /**
     * Retrieves current span ID from OpenTelemetry or MDC.
     *
     * @return span ID or null if not available
     */
    public static @Nullable String getSpanId() {
        String spanId = getSpanIdFromOtel();
        if (spanId != null && !spanId.isBlank() && !INVALID_SPAN_ID.equals(spanId)) {
            return spanId;
        }
        return getFromMdc("spanId", "span_id", "spanID", "X-B3-SpanId", "X-B3-SpanID", "span-id");
    }

    private static @Nullable String getFromMdc(String... keys) {
        for (String key : keys) {
            String value = MDC.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static @Nullable String getTraceIdFromOtel() {
        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Method currentMethod = spanClass.getMethod("current");
            Object span = currentMethod.invoke(null);
            if (span == null) {
                return null;
            }
            Method getSpanContextMethod = spanClass.getMethod("getSpanContext");
            Object spanContext = getSpanContextMethod.invoke(span);
            if (spanContext == null) {
                return null;
            }
            Class<?> spanContextClass = spanContext.getClass();
            Method isValidMethod = spanContextClass.getMethod("isValid");
            Boolean isValid = (Boolean) isValidMethod.invoke(spanContext);
            if (isValid == null || !isValid) {
                return null;
            }
            Method getTraceIdMethod = spanContextClass.getMethod("getTraceId");
            Object traceId = getTraceIdMethod.invoke(spanContext);
            if (traceId instanceof String str) {
                return str.isBlank() ? null : str;
            }
            return null;
        } catch (ClassNotFoundException e) {
            // OTel not on classpath, ignore
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable String getSpanIdFromOtel() {
        try {
            Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
            Method currentMethod = spanClass.getMethod("current");
            Object span = currentMethod.invoke(null);
            if (span == null) {
                return null;
            }
            Method getSpanContextMethod = spanClass.getMethod("getSpanContext");
            Object spanContext = getSpanContextMethod.invoke(span);
            if (spanContext == null) {
                return null;
            }
            Class<?> spanContextClass = spanContext.getClass();
            Method isValidMethod = spanContextClass.getMethod("isValid");
            Boolean isValid = (Boolean) isValidMethod.invoke(spanContext);
            if (isValid == null || !isValid) {
                return null;
            }
            Method getSpanIdMethod = spanContextClass.getMethod("getSpanId");
            Object spanId = getSpanIdMethod.invoke(spanContext);
            if (spanId instanceof String str) {
                return str.isBlank() ? null : str;
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Injects trace correlation IDs into MDC for log correlation.
     * Useful for manual propagation when OTel context is not automatically in MDC.
     *
     * @param traceId trace ID to set, may be null to remove
     * @param spanId span ID to set, may be null to remove
     */
    public static void setMdc(@Nullable String traceId, @Nullable String spanId) {
        if (traceId != null) {
            MDC.put("trace_id", traceId);
            MDC.put("traceId", traceId);
        } else {
            MDC.remove("trace_id");
            MDC.remove("traceId");
        }
        if (spanId != null) {
            MDC.put("span_id", spanId);
            MDC.put("spanId", spanId);
        } else {
            MDC.remove("span_id");
            MDC.remove("spanId");
        }
    }

    /**
     * Clears trace correlation from MDC.
     */
    public static void clearMdc() {
        MDC.remove("trace_id");
        MDC.remove("traceId");
        MDC.remove("span_id");
        MDC.remove("spanId");
        MDC.remove("traceID");
        MDC.remove("spanID");
        MDC.remove("X-B3-TraceId");
        MDC.remove("X-B3-SpanId");
    }
}
