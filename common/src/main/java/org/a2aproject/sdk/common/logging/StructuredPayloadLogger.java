package org.a2aproject.sdk.common.logging;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central logger for structured A2A payload logging.
 * <p>
 * Provides DEBUG-level structured log events for key protocol messages:
 * task requests, state changes, result artifacts, negotiation payloads,
 * authorization decisions, notification subscriptions and deliveries.
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable payload size truncation (default 4KB) with {@code [truncated]} marker</li>
 *   <li>Sensitive data masking with customizable strategies</li>
 *   <li>Trace correlation via {@code trace_id} / {@code span_id} from OpenTelemetry or MDC</li>
 *   <li>Structured fields via SLF4J fluent API (keyValue) when available</li>
 * </ul>
 * </p>
 * <p>
 * Logs are emitted only when:
 * <ul>
 *   <li>Payload logging is enabled via {@link A2APayloadLoggingConfig} (system property {@code a2a.logging.payload.enabled=true} or programmatic config)</li>
 *   <li>Underlying SLF4J logger is DEBUG enabled</li>
 * </ul>
 * </p>
 */
public final class StructuredPayloadLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.a2aproject.sdk.payload");

    /**
     * System property to enable payload logging via logger level without config.
     * For backward compatibility, also checks {@code org.a2aproject.sdk.payload.enabled}.
     */
    private static final String LEGACY_ENABLED_PROP = "org.a2aproject.sdk.payload.enabled";

    private static final AtomicReference<A2APayloadLoggingConfig> CONFIG_REF =
            new AtomicReference<>(A2APayloadLoggingConfig.fromSystemProperties());

    private static volatile PayloadMaskingStrategy maskingStrategy = createDefaultMaskingStrategy();

    private StructuredPayloadLogger() {
    }

    /**
     * Returns current config (volatile read).
     */
    public static A2APayloadLoggingConfig getConfig() {
        return CONFIG_REF.get();
    }

    /**
     * Sets config programmatically. Useful for tests or runtime reconfiguration.
     *
     * @param config new config
     */
    public static void setConfig(A2APayloadLoggingConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        CONFIG_REF.set(config);
        // Refresh masking strategy if sensitive keys changed
        if (config.getMaskingStrategy() != null) {
            maskingStrategy = config.getMaskingStrategy();
        } else if (config.getSensitiveKeys() != null) {
            maskingStrategy = new DefaultPayloadMaskingStrategy(config.getSensitiveKeys());
        } else {
            maskingStrategy = createDefaultMaskingStrategy();
        }
    }

    /**
     * Resets to system properties (useful for tests).
     */
    public static void resetToSystemProperties() {
        CONFIG_REF.set(A2APayloadLoggingConfig.fromSystemProperties());
        maskingStrategy = createDefaultMaskingStrategy();
        // Try to load strategy from system property if present
        String strategyClass = System.getProperty(A2APayloadLoggingConfig.MASKING_STRATEGY_PROPERTY);
        if (strategyClass != null && !strategyClass.isBlank()) {
            try {
                Class<?> clazz = Class.forName(strategyClass.trim());
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof PayloadMaskingStrategy pms) {
                    maskingStrategy = pms;
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Sets custom masking strategy.
     */
    public static void setMaskingStrategy(PayloadMaskingStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("strategy must not be null");
        }
        maskingStrategy = strategy;
    }

    public static PayloadMaskingStrategy getMaskingStrategy() {
        return maskingStrategy;
    }

    /**
     * Returns underlying SLF4J logger for payload events.
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Checks if payload logging should be emitted.
     * True when config enabled (or legacy property) and DEBUG enabled.
     */
    public static boolean isEnabled() {
        A2APayloadLoggingConfig cfg = CONFIG_REF.get();
        boolean enabled = cfg.isEnabled() || Boolean.getBoolean(LEGACY_ENABLED_PROP);
        return enabled && LOGGER.isDebugEnabled();
    }

    /**
     * Checks if logging would be enabled ignoring logger level (for testing).
     */
    public static boolean isConfigEnabled() {
        A2APayloadLoggingConfig cfg = CONFIG_REF.get();
        return cfg.isEnabled() || Boolean.getBoolean(LEGACY_ENABLED_PROP);
    }

    private static PayloadMaskingStrategy createDefaultMaskingStrategy() {
        A2APayloadLoggingConfig cfg = CONFIG_REF.get();
        Set<String> sensitiveKeys = cfg.getSensitiveKeys();
        if (sensitiveKeys != null) {
            return new DefaultPayloadMaskingStrategy(sensitiveKeys);
        }
        // Check system property for sensitive keys override
        String keysProp = System.getProperty(A2APayloadLoggingConfig.MASKING_SENSITIVE_KEYS_PROPERTY);
        if (keysProp != null && !keysProp.isBlank()) {
            Set<String> keys = Set.of(keysProp.split(","));
            java.util.Set<String> trimmed = new java.util.HashSet<>();
            for (String k : keys) {
                if (k != null && !k.isBlank()) {
                    trimmed.add(k.trim());
                }
            }
            if (!trimmed.isEmpty()) {
                return new DefaultPayloadMaskingStrategy(trimmed);
            }
        }
        return new DefaultPayloadMaskingStrategy();
    }

    // ==================== Core logging methods ====================

    /**
     * Logs a structured payload event.
     *
     * @param eventType the event type
     * @param payload the payload as JSON string (or any string); may be null
     */
    public static void log(A2AEventType eventType, @Nullable String payload) {
        log(eventType, payload, null);
    }

    /**
     * Logs a structured payload event with additional context fields.
     *
     * @param eventType the event type
     * @param payload the payload as JSON string; may be null
     * @param context additional context key-values (e.g., taskId, operation), may be null
     */
    public static void log(A2AEventType eventType, @Nullable String payload, @Nullable Map<String, String> context) {
        if (!isEnabled()) {
            return;
        }
        A2APayloadLoggingConfig cfg = CONFIG_REF.get();
        String masked = payload;
        if (cfg.isMaskingEnabled() && payload != null) {
            try {
                masked = maskingStrategy.mask(payload);
            } catch (Exception e) {
                // Fallback to original payload on masking failure to avoid log loss
                masked = payload;
            }
        }
        String truncated = PayloadTruncator.truncate(masked, cfg.getMaxSize());
        String traceId = TraceCorrelation.getTraceId();
        String spanId = TraceCorrelation.getSpanId();

        // Build structured message
        StringBuilder msg = new StringBuilder();
        msg.append("a2a.payload event_type=").append(eventType.value());
        if (truncated != null) {
            msg.append(" payload=").append(truncated);
        }
        if (context != null && !context.isEmpty()) {
            for (Map.Entry<String, String> e : context.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    msg.append(" ").append(e.getKey()).append("=").append(e.getValue());
                }
            }
        }
        if (traceId != null) {
            msg.append(" trace_id=").append(traceId);
        }
        if (spanId != null) {
            msg.append(" span_id=").append(spanId);
        }

        // Attempt fluent API for structured logging (SLF4J 2.0+)
        try {
            var builder = LOGGER.atDebug();
            // addKeyValue returns LoggingEventBuilder; we can chain
            builder = addKeyValueSafe(builder, "a2a.event.type", eventType.value());
            if (truncated != null) {
                builder = addKeyValueSafe(builder, "a2a.payload", truncated);
                builder = addKeyValueSafe(builder, "a2a.payload.size", String.valueOf(payload != null ? payload.length() : 0));
                if (PayloadTruncator.isTruncated(truncated)) {
                    builder = addKeyValueSafe(builder, "a2a.payload.truncated", "true");
                }
            }
            if (traceId != null) {
                builder = addKeyValueSafe(builder, "trace_id", traceId);
            }
            if (spanId != null) {
                builder = addKeyValueSafe(builder, "span_id", spanId);
            }
            if (context != null) {
                for (Map.Entry<String, String> e : context.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        builder = addKeyValueSafe(builder, e.getKey(), e.getValue());
                    }
                }
            }
            builder.log(msg.toString());
            return;
        } catch (NoSuchMethodError | NoClassDefFoundError | Exception ignored) {
            // Fallback to classic debug
        }
        // Fallback
        LOGGER.debug(msg.toString());
    }

    /**
     * Logs with object payload; attempts JSON serialization via toString() fallback.
     * Callers that have access to JsonUtil should serialize first and call string overload.
     *
     * @param eventType event type
     * @param payloadObject payload object; will be converted via toString() or JSON if possible
     * @param context extra context
     */
    public static void logObject(A2AEventType eventType, @Nullable Object payloadObject, @Nullable Map<String, String> context) {
        if (!isEnabled()) {
            return;
        }
        String payloadStr = null;
        if (payloadObject != null) {
            // Try to use JsonUtil if available via reflection to get proper JSON
            payloadStr = trySerializeToJson(payloadObject);
            if (payloadStr == null) {
                payloadStr = payloadObject.toString();
            }
        }
        log(eventType, payloadStr, context);
    }

    /**
     * Convenience: logs without context.
     */
    public static void logObject(A2AEventType eventType, @Nullable Object payloadObject) {
        logObject(eventType, payloadObject, null);
    }

    /**
     * Attempts to serialize object to JSON using JsonUtil via reflection if available.
     */
    private static @Nullable String trySerializeToJson(Object payloadObject) {
        try {
            Class<?> jsonUtilClass = Class.forName("org.a2aproject.sdk.jsonrpc.common.json.JsonUtil");
            // Try to find toJson method
            var method = jsonUtilClass.getMethod("toJson", Object.class);
            Object result = method.invoke(null, payloadObject);
            if (result instanceof String s) {
                return s;
            }
        } catch (ClassNotFoundException ignored) {
            // JsonUtil not on classpath, fallback to toString
        } catch (Exception ignored) {
        }
        // Try Gson directly if available
        try {
            Class<?> gsonClass = Class.forName("com.google.gson.Gson");
            Object gson = gsonClass.getDeclaredConstructor().newInstance();
            var toJsonMethod = gsonClass.getMethod("toJson", Object.class);
            Object result = toJsonMethod.invoke(gson, payloadObject);
            if (result instanceof String s) {
                return s;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static org.slf4j.spi.LoggingEventBuilder addKeyValueSafe(org.slf4j.spi.LoggingEventBuilder builder, String key, String value) {
        try {
            return builder.addKeyValue(key, value);
        } catch (Exception e) {
            return builder;
        }
    }

    // ==================== Convenience methods for specific event types ====================

    /**
     * Logs a task request payload.
     *
     * @param operation operation name (e.g., "message/send")
     * @param payload JSON payload string
     * @param taskId task ID if available
     * @param contextId context ID if available
     */
    public static void logTaskRequest(String operation, @Nullable String payload, @Nullable String taskId, @Nullable String contextId) {
        Map<String, String> ctx = new java.util.HashMap<>();
        ctx.put("a2a.operation", operation);
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (contextId != null) ctx.put("a2a.context_id", contextId);
        log(A2AEventType.TASK_REQUEST, payload, ctx);
    }

    /**
     * Logs a task request with object payload.
     */
    public static void logTaskRequest(String operation, @Nullable Object payloadObject, @Nullable String taskId, @Nullable String contextId) {
        Map<String, String> ctx = new java.util.HashMap<>();
        ctx.put("a2a.operation", operation);
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (contextId != null) ctx.put("a2a.context_id", contextId);
        logObject(A2AEventType.TASK_REQUEST, payloadObject, ctx);
    }

    /**
     * Logs a state change event.
     *
     * @param payload JSON payload
     * @param taskId task ID
     * @param newState new state name
     */
    public static void logStateChange(@Nullable String payload, @Nullable String taskId, @Nullable String newState) {
        Map<String, String> ctx = new java.util.HashMap<>();
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (newState != null) ctx.put("a2a.task_state", newState);
        log(A2AEventType.STATE_CHANGE, payload, ctx);
    }

    /**
     * Logs an artifact event.
     *
     * @param payload JSON payload
     * @param taskId task ID
     * @param artifactId artifact ID
     */
    public static void logArtifact(@Nullable String payload, @Nullable String taskId, @Nullable String artifactId) {
        Map<String, String> ctx = new java.util.HashMap<>();
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (artifactId != null) ctx.put("a2a.artifact_id", artifactId);
        log(A2AEventType.ARTIFACT, payload, ctx);
    }

    /**
     * Logs an authorization decision.
     *
     * @param operation operation name
     * @param taskId task ID
     * @param allowed true if allowed
     * @param reason reason or user info
     */
    public static void logAuthorization(String operation, @Nullable String taskId, boolean allowed, @Nullable String reason) {
        Map<String, String> ctx = new java.util.HashMap<>();
        ctx.put("a2a.operation", operation);
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        ctx.put("a2a.auth.allowed", String.valueOf(allowed));
        if (reason != null) ctx.put("a2a.auth.reason", reason);
        // Authorization payload is the decision itself; we log with minimal payload to avoid leaking
        String payload = "{\"operation\":\"" + operation + "\",\"taskId\":\"" + taskId + "\",\"allowed\":" + allowed + "}";
        log(A2AEventType.AUTHORIZATION, payload, ctx);
    }

    /**
     * Logs a push subscription event (create/get/list/delete/subscribe).
     *
     * @param operation operation name
     * @param payload JSON payload
     * @param taskId task ID
     * @param configId config ID
     */
    public static void logPushSubscription(String operation, @Nullable String payload, @Nullable String taskId, @Nullable String configId) {
        Map<String, String> ctx = new java.util.HashMap<>();
        ctx.put("a2a.operation", operation);
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (configId != null) ctx.put("a2a.config_id", configId);
        log(A2AEventType.NOTIFICATION_SUBSCRIPTION, payload, ctx);
    }

    /**
     * Logs a push delivery event.
     *
     * @param taskId task ID
     * @param url push URL (will be masked)
     * @param payload event payload
     * @param success delivery success
     */
    public static void logPushDelivery(@Nullable String taskId, @Nullable String url, @Nullable String payload, boolean success) {
        Map<String, String> ctx = new java.util.HashMap<>();
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (url != null) ctx.put("a2a.push.url", url);
        ctx.put("a2a.push.success", String.valueOf(success));
        log(A2AEventType.NOTIFICATION_DELIVERY, payload, ctx);
    }

    /**
     * Logs a negotiation payload (extensions, capabilities).
     *
     * @param operation operation name
     * @param payload JSON payload
     */
    public static void logNegotiation(String operation, @Nullable String payload) {
        Map<String, String> ctx = Map.of("a2a.operation", operation);
        log(A2AEventType.NEGOTIATION, payload, ctx);
    }

    /**
     * Logs a streaming event.
     *
     * @param payload JSON payload
     * @param taskId task ID
     * @param eventKind event kind (task, statusUpdate, artifactUpdate, message)
     */
    public static void logStreamingEvent(@Nullable String payload, @Nullable String taskId, @Nullable String eventKind) {
        Map<String, String> ctx = new java.util.HashMap<>();
        if (taskId != null) ctx.put("a2a.task_id", taskId);
        if (eventKind != null) ctx.put("a2a.streaming.kind", eventKind);
        log(A2AEventType.STREAMING_EVENT, payload, ctx);
    }
}
