package org.a2aproject.sdk.server.logging;

import org.a2aproject.sdk.common.logging.StructuredPayloadLogger;
import org.a2aproject.sdk.server.util.JsonUtil;

public final class ServerPayloadLoggingHelper {
    private ServerPayloadLoggingHelper() {}
    private static String toJsonSafe(Object obj) {
        try { return JsonUtil.toJson(obj); } catch (Exception e) { return String.valueOf(obj); }
    }
    public static void logTaskRequest(String op, Object params) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.logTaskRequest(op, toJsonSafe(params), extractTaskId(params));
    }
    public static void logTaskResult(String op, Object result) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.logTaskResult(op, toJsonSafe(result), extractTaskId(result));
    }
    public static void logStateChange(Object event) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.logTaskStateChange(toJsonSafe(event), extractTaskId(event), extractStatus(event));
    }
    public static void logArtifact(Object event) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.logTaskArtifact(toJsonSafe(event), extractTaskId(event), null);
    }
    public static void logAuth(String op, boolean allowed, String reason) {
        StructuredPayloadLogger.logAuthDecision(op, allowed, reason);
    }
    public static void logPushSub(String op, Object params) {
        if (!StructuredPayloadLogger.isEnabled()) return;
        StructuredPayloadLogger.logPushSubscription(op, toJsonSafe(params));
    }
    public static void logPushDelivery(String url, boolean success, Object event) {
        StructuredPayloadLogger.logPushDelivery(url, success, toJsonSafe(event));
    }
    private static String extractTaskId(Object o) {
        if (o == null) return null;
        try {
            String json = toJsonSafe(o);
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            if (el.isJsonObject()) {
                com.google.gson.JsonObject jo = el.getAsJsonObject();
                if (jo.has("taskId")) return jo.get("taskId").getAsString();
                if (jo.has("id") && jo.get("id").isJsonPrimitive()) return jo.get("id").getAsString();
                if (jo.has("task") && jo.get("task").isJsonObject()) {
                    com.google.gson.JsonObject t = jo.getAsJsonObject("task");
                    if (t.has("id")) return t.get("id").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    private static String extractStatus(Object o) {
        if (o == null) return null;
        try {
            String json = toJsonSafe(o);
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            if (el.isJsonObject() && el.getAsJsonObject().has("status")) {
                com.google.gson.JsonObject s = el.getAsJsonObject().getAsJsonObject("status");
                if (s != null && s.has("state")) return s.get("state").getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
