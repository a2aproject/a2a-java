package org.a2aproject.sdk.jsonrpc.common.logging;

import org.a2aproject.sdk.common.logging.A2AEventType;
import org.a2aproject.sdk.common.logging.StructuredPayloadLogger;
import org.a2aproject.sdk.server.util.JsonUtil;

public final class JsonPayloadLoggingHelper {
    private JsonPayloadLoggingHelper() {}
    private static String toJsonSafe(Object obj) {
        try { return JsonUtil.toJson(obj); } catch (Exception e) { return String.valueOf(obj); }
    }
    public static void logTaskRequest(String op, Object params) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.log(A2AEventType.TASK_REQUEST, toJsonSafe(params), "a2a.operation", op);
    }
    public static void logTaskResult(String op, Object result) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.log(A2AEventType.TASK_RESULT, toJsonSafe(result), "a2a.operation", op);
    }
    public static void logStreamingEvent(Object event) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.log(A2AEventType.STREAMING_EVENT, toJsonSafe(event));
    }
    public static void logPushSubscription(String op, Object params) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.log(A2AEventType.NOTIFICATION_SUBSCRIPTION, toJsonSafe(params), "a2a.operation", op);
    }
    public static void logNegotiation(String op, Object payload) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.log(A2AEventType.NEGOTIATION, toJsonSafe(payload), "a2a.operation", op);
    }
}
