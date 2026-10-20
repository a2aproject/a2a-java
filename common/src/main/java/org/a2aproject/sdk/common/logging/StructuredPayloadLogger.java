package org.a2aproject.sdk.common.logging;

import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StructuredPayloadLogger {
    private static final Logger PAYLOAD_LOGGER = LoggerFactory.getLogger("org.a2aproject.sdk.payload");
    private static final AtomicReference<A2APayloadLoggingConfig> CONFIG = new AtomicReference<>(A2APayloadLoggingConfig.fromSystemProperties());
    private static final AtomicReference<PayloadMaskingStrategy> STRATEGY = new AtomicReference<>(new DefaultPayloadMaskingStrategy());
    private StructuredPayloadLogger() {}
    public static void setConfig(A2APayloadLoggingConfig cfg) {
        CONFIG.set(cfg);
        if (cfg.getMaskingStrategy() != null) STRATEGY.set(cfg.getMaskingStrategy());
    }
    public static void setMaskingStrategy(PayloadMaskingStrategy s) { STRATEGY.set(s); }
    public static void resetToSystemProperties() {
        CONFIG.set(A2APayloadLoggingConfig.fromSystemProperties());
        STRATEGY.set(new DefaultPayloadMaskingStrategy());
    }
    public static boolean isEnabled() {
        A2APayloadLoggingConfig c = CONFIG.get();
        return c.isEnabled() && PAYLOAD_LOGGER.isDebugEnabled();
    }
    public static void log(A2AEventType type, String payload, String... kvs) {
        if (!isEnabled()) return;
        A2APayloadLoggingConfig cfg = CONFIG.get();
        String masked = cfg.isMaskingEnabled() ? STRATEGY.get().mask(payload) : payload;
        int max = cfg.getMaxSize();
        boolean truncated = masked != null && masked.length() > max;
        String out = PayloadTruncator.truncate(masked, max);
        int originalSize = masked != null ? masked.length() : 0;
        String traceId = TraceCorrelation.currentTraceId();
        String spanId = TraceCorrelation.currentSpanId();
        try {
            var builder = PAYLOAD_LOGGER.atDebug();
            builder.addKeyValue("a2a.event.type", type.value());
            builder.addKeyValue("a2a.payload", out);
            builder.addKeyValue("a2a.payload.size", String.valueOf(originalSize));
            builder.addKeyValue("a2a.payload.truncated", String.valueOf(truncated));
            if (traceId != null) builder.addKeyValue("trace_id", traceId);
            if (spanId != null) builder.addKeyValue("span_id", spanId);
            for (int i = 0; i + 1 < kvs.length; i += 2) builder.addKeyValue(kvs[i], kvs[i+1]);
            builder.log("a2a.payload event_type=" + type.value());
        } catch (NoSuchMethodError | Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("a2a.payload event_type=").append(type.value());
            sb.append(" payload=").append(out);
            sb.append(" a2a.payload.size=").append(originalSize);
            sb.append(" a2a.payload.truncated=").append(truncated);
            if (traceId != null) sb.append(" trace_id=").append(traceId);
            if (spanId != null) sb.append(" span_id=").append(spanId);
            for (int i = 0; i + 1 < kvs.length; i += 2) sb.append(" ").append(kvs[i]).append("=").append(kvs[i+1]);
            PAYLOAD_LOGGER.debug(sb.toString());
        }
    }
    public static void logTaskRequest(String op, String payload, @Nullable String taskId) {
        if (taskId != null) log(A2AEventType.TASK_REQUEST, payload, "a2a.operation", op, "a2a.task_id", taskId);
        else log(A2AEventType.TASK_REQUEST, payload, "a2a.operation", op);
    }
    public static void logTaskResult(String op, String payload, @Nullable String taskId) {
        if (taskId != null) log(A2AEventType.TASK_RESULT, payload, "a2a.operation", op, "a2a.task_id", taskId);
        else log(A2AEventType.TASK_RESULT, payload, "a2a.operation", op);
    }
    public static void logTaskStateChange(String payload, @Nullable String taskId, @Nullable String status) {
        if (taskId != null && status != null) log(A2AEventType.TASK_STATE_CHANGE, payload, "a2a.task_id", taskId, "a2a.task.status", status);
        else if (taskId != null) log(A2AEventType.TASK_STATE_CHANGE, payload, "a2a.task_id", taskId);
        else log(A2AEventType.TASK_STATE_CHANGE, payload);
    }
    public static void logTaskArtifact(String payload, @Nullable String taskId, @Nullable String artifactId) {
        if (taskId != null) log(A2AEventType.TASK_ARTIFACT, payload, "a2a.task_id", taskId, "a2a.artifact_id", artifactId != null ? artifactId : "");
        else log(A2AEventType.TASK_ARTIFACT, payload);
    }
    public static void logAuthDecision(String op, boolean allowed, String reason) {
        log(A2AEventType.AUTHORIZATION, reason, "a2a.operation", op, "a2a.auth.allowed", String.valueOf(allowed));
    }
    public static void logPushSubscription(String op, String payload) {
        log(A2AEventType.NOTIFICATION_SUBSCRIPTION, payload, "a2a.operation", op);
    }
    public static void logPushDelivery(String url, boolean success, String payload) {
        log(A2AEventType.NOTIFICATION_DELIVERY, payload, "a2a.push.url", url, "a2a.push.success", String.valueOf(success));
    }
    public static void logNegotiation(String op, String payload) {
        log(A2AEventType.NEGOTIATION, payload, "a2a.operation", op);
    }
    public static void logStreamingEvent(String payload, @Nullable String taskId) {
        if (taskId != null) log(A2AEventType.STREAMING_EVENT, payload, "a2a.task_id", taskId);
        else log(A2AEventType.STREAMING_EVENT, payload);
    }
}
