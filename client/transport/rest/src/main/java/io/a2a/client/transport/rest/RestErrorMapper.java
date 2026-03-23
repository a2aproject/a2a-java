package io.a2a.client.transport.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AErrorCodes;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.spec.VersionNotSupportedError;

/**
 * Utility class to A2AHttpResponse to appropriate A2A error types
 */
public class RestErrorMapper {

    private record ReasonAndMetadata(String reason, @org.jspecify.annotations.Nullable Map<String, Object> metadata) {}

    private static final Map<String, A2AErrorCodes> REASON_MAP = Map.ofEntries(
            Map.entry("TASK_NOT_FOUND", A2AErrorCodes.TASK_NOT_FOUND),
            Map.entry("TASK_NOT_CANCELABLE", A2AErrorCodes.TASK_NOT_CANCELABLE),
            Map.entry("PUSH_NOTIFICATION_NOT_SUPPORTED", A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED),
            Map.entry("UNSUPPORTED_OPERATION", A2AErrorCodes.UNSUPPORTED_OPERATION),
            Map.entry("CONTENT_TYPE_NOT_SUPPORTED", A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED),
            Map.entry("INVALID_AGENT_RESPONSE", A2AErrorCodes.INVALID_AGENT_RESPONSE),
            Map.entry("EXTENDED_AGENT_CARD_NOT_CONFIGURED", A2AErrorCodes.EXTENDED_AGENT_CARD_NOT_CONFIGURED),
            Map.entry("EXTENSION_SUPPORT_REQUIRED", A2AErrorCodes.EXTENSION_SUPPORT_REQUIRED),
            Map.entry("VERSION_NOT_SUPPORTED", A2AErrorCodes.VERSION_NOT_SUPPORTED),
            Map.entry("INVALID_REQUEST", A2AErrorCodes.INVALID_REQUEST),
            Map.entry("METHOD_NOT_FOUND", A2AErrorCodes.METHOD_NOT_FOUND),
            Map.entry("INVALID_PARAMS", A2AErrorCodes.INVALID_PARAMS),
            Map.entry("INTERNAL", A2AErrorCodes.INTERNAL),
            Map.entry("JSON_PARSE", A2AErrorCodes.JSON_PARSE)
    );

    public static A2AClientException mapRestError(A2AHttpResponse response) {
        return RestErrorMapper.mapRestError(response.body(), response.status());
    }

    public static A2AClientException mapRestError(String body, int code) {
        try {
            if (body != null && !body.isBlank()) {
                JsonObject node = JsonUtil.fromJson(body, JsonObject.class);
                // Google Cloud API error format: { "error": { "code", "status", "message", "details" } }
                if (node.has("error") && node.get("error").isJsonObject()) {
                    JsonObject errorObj = node.getAsJsonObject("error");
                    String errorMessage = errorObj.has("message") ? errorObj.get("message").getAsString() : "";
                    ReasonAndMetadata reasonAndMetadata = extractReasonAndMetadata(errorObj);
                    if (reasonAndMetadata != null) {
                        return mapRestErrorByReason(reasonAndMetadata.reason(), errorMessage, reasonAndMetadata.metadata());
                    }
                    return new A2AClientException(errorMessage);
                }
                // Legacy format (error class name, message)
                String className = node.has("error") ? node.get("error").getAsString() : "";
                String errorMessage = node.has("message") ? node.get("message").getAsString() : "";
                return mapRestErrorByClassName(className, errorMessage, code);
            }
            return mapRestErrorByClassName("", "", code);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(RestErrorMapper.class.getName()).log(Level.SEVERE, null, ex);
            return new A2AClientException("Failed to parse error response: " + ex.getMessage());
        }
    }

    public static A2AClientException mapRestError(String className, String errorMessage, int code) {
        return mapRestErrorByClassName(className, errorMessage, code);
    }

    /**
     * Extracts the "reason" and "metadata" fields from the first entry in the "details" array.
     */
    private static @org.jspecify.annotations.Nullable ReasonAndMetadata extractReasonAndMetadata(JsonObject errorObj) {
        if (errorObj.has("details") && errorObj.get("details").isJsonArray()) {
            var details = errorObj.getAsJsonArray("details");
            if (!details.isEmpty() && details.get(0).isJsonObject()) {
                JsonObject detail = details.get(0).getAsJsonObject();
                if (detail.has("reason")) {
                    String reason = detail.get("reason").getAsString();
                    Map<String, Object> metadata = null;
                    if (detail.has("metadata") && detail.get("metadata").isJsonObject()) {
                        JsonObject metaObj = detail.getAsJsonObject("metadata");
                        Map<String, Object> metaMap = new HashMap<>();
                        for (var entry : metaObj.entrySet()) {
                            metaMap.put(entry.getKey(), JsonUtil.OBJECT_MAPPER.fromJson(entry.getValue(), Object.class));
                        }
                        metadata = metaMap;
                    }
                    return new ReasonAndMetadata(reason, metadata);
                }
            }
        }
        return null;
    }

    /**
     * Maps error reason strings to A2A exceptions.
     *
     * @param reason the error reason (e.g., "TASK_NOT_FOUND")
     * @param errorMessage the error message
     * @param metadata additional metadata extracted from the error details
     * @return an A2AClientException wrapping the appropriate A2A error
     */
    private static A2AClientException mapRestErrorByReason(String reason, String errorMessage, @org.jspecify.annotations.Nullable Map<String, Object> metadata) {
        A2AErrorCodes errorCode = REASON_MAP.get(reason);
        if (errorCode == null) {
            return new A2AClientException(errorMessage);
        }
        return switch (errorCode) {
            case TASK_NOT_FOUND -> new A2AClientException(errorMessage, new TaskNotFoundError(errorMessage, metadata));
            case TASK_NOT_CANCELABLE -> new A2AClientException(errorMessage, new TaskNotCancelableError(null, errorMessage, metadata));
            case PUSH_NOTIFICATION_NOT_SUPPORTED -> new A2AClientException(errorMessage, new PushNotificationNotSupportedError(null, errorMessage, metadata));
            case UNSUPPORTED_OPERATION -> new A2AClientException(errorMessage, new UnsupportedOperationError(null, errorMessage, metadata));
            case CONTENT_TYPE_NOT_SUPPORTED -> new A2AClientException(errorMessage, new ContentTypeNotSupportedError(null, errorMessage, metadata));
            case INVALID_AGENT_RESPONSE -> new A2AClientException(errorMessage, new InvalidAgentResponseError(null, errorMessage, metadata));
            case EXTENDED_AGENT_CARD_NOT_CONFIGURED -> new A2AClientException(errorMessage, new ExtendedAgentCardNotConfiguredError(null, errorMessage, metadata));
            case EXTENSION_SUPPORT_REQUIRED -> new A2AClientException(errorMessage, new ExtensionSupportRequiredError(null, errorMessage, metadata));
            case VERSION_NOT_SUPPORTED -> new A2AClientException(errorMessage, new VersionNotSupportedError(null, errorMessage, metadata));
            case INVALID_REQUEST -> new A2AClientException(errorMessage, new InvalidRequestError(null, errorMessage, metadata));
            case JSON_PARSE -> new A2AClientException(errorMessage, new JSONParseError(null, errorMessage, metadata));
            case METHOD_NOT_FOUND -> new A2AClientException(errorMessage, new MethodNotFoundError(null, errorMessage, metadata));
            case INVALID_PARAMS -> new A2AClientException(errorMessage, new InvalidParamsError(null, errorMessage, metadata));
            case INTERNAL -> new A2AClientException(errorMessage, new InternalError(null, errorMessage, metadata));
        };
    }

    private static A2AClientException mapRestErrorByClassName(String className, String errorMessage, int code) {
        return switch (className) {
            case "io.a2a.spec.TaskNotFoundError" -> new A2AClientException(errorMessage, new TaskNotFoundError());
            case "io.a2a.spec.ExtendedCardNotConfiguredError" -> new A2AClientException(errorMessage, new ExtendedAgentCardNotConfiguredError(null, errorMessage, null));
            case "io.a2a.spec.ContentTypeNotSupportedError" -> new A2AClientException(errorMessage, new ContentTypeNotSupportedError(null, errorMessage, null));
            case "io.a2a.spec.InternalError" -> new A2AClientException(errorMessage, new InternalError(errorMessage));
            case "io.a2a.spec.InvalidAgentResponseError" -> new A2AClientException(errorMessage, new InvalidAgentResponseError(null, errorMessage, null));
            case "io.a2a.spec.InvalidParamsError" -> new A2AClientException(errorMessage, new InvalidParamsError());
            case "io.a2a.spec.InvalidRequestError" -> new A2AClientException(errorMessage, new InvalidRequestError());
            case "io.a2a.spec.JSONParseError" -> new A2AClientException(errorMessage, new JSONParseError());
            case "io.a2a.spec.MethodNotFoundError" -> new A2AClientException(errorMessage, new MethodNotFoundError());
            case "io.a2a.spec.PushNotificationNotSupportedError" -> new A2AClientException(errorMessage, new PushNotificationNotSupportedError());
            case "io.a2a.spec.TaskNotCancelableError" -> new A2AClientException(errorMessage, new TaskNotCancelableError());
            case "io.a2a.spec.UnsupportedOperationError" -> new A2AClientException(errorMessage, new UnsupportedOperationError());
            case "io.a2a.spec.ExtensionSupportRequiredError" -> new A2AClientException(errorMessage, new ExtensionSupportRequiredError(null, errorMessage, null));
            case "io.a2a.spec.VersionNotSupportedError" -> new A2AClientException(errorMessage, new VersionNotSupportedError(null, errorMessage, null));
            default -> new A2AClientException(errorMessage);
        };
    }
}
