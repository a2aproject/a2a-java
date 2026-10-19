package org.a2aproject.sdk.jsonrpc.common.logging;

import org.a2aproject.sdk.common.logging.A2AEventType;
import org.a2aproject.sdk.common.logging.StructuredPayloadLogger;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for payload logging with JSON serialization via JsonUtil.
 * <p>
 * Provides convenience methods to log A2A protocol payloads as JSON strings,
 * handling truncation, masking, and trace correlation via {@link StructuredPayloadLogger}.
 * </p>
 */
public final class JsonPayloadLoggingHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPayloadLoggingHelper.class);

    private JsonPayloadLoggingHelper() {
    }

    /**
     * Serializes object to JSON safely, fallback to toString.
     */
    public static @Nullable String toJsonSafe(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return JsonUtil.toJson(obj);
        } catch (Exception e) {
            LOGGER.debug("Failed to serialize payload for logging: {}", e.getMessage());
            try {
                return obj.toString();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static void logTaskRequest(String operation, @Nullable Object payload, @Nullable String taskId, @Nullable String contextId) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(payload);
        StructuredPayloadLogger.logTaskRequest(operation, json, taskId, contextId);
    }

    public static void logTaskResult(@Nullable Object result, @Nullable String taskId) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(result);
        java.util.Map<String, String> ctx = new java.util.HashMap<>();
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        ctx.put("a2a.operation", "task.result");
        StructuredPayloadLogger.log(A2AEventType.TASK_RESULT, json, ctx);
    }

    public static void logStateChange(@Nullable Object event, @Nullable String taskId, @Nullable String newState) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(event);
        StructuredPayloadLogger.logStateChange(json, taskId, newState);
    }

    public static void logArtifact(@Nullable Object event, @Nullable String taskId, @Nullable String artifactId) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(event);
        StructuredPayloadLogger.logArtifact(json, taskId, artifactId);
    }

    public static void logStreamingEvent(@Nullable Object event, @Nullable String taskId, @Nullable String kind) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(event);
        StructuredPayloadLogger.logStreamingEvent(json, taskId, kind);
    }

    public static void logPushSubscription(String operation, @Nullable Object payload, @Nullable String taskId, @Nullable String configId) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(payload);
        StructuredPayloadLogger.logPushSubscription(operation, json, taskId, configId);
    }

    public static void logPushDelivery(@Nullable String taskId, @Nullable String url, @Nullable Object event, boolean success) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(event);
        StructuredPayloadLogger.logPushDelivery(taskId, url, json, success);
    }

    public static void logNegotiation(String operation, @Nullable Object payload) {
        if (!StructuredPayloadLogger.isEnabled()) {
            return;
        }
        String json = toJsonSafe(payload);
        StructuredPayloadLogger.logNegotiation(operation, json);
    }
}
