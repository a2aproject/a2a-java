package org.a2aproject.sdk.compat03.client.transport.rest;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientHTTPError_v0_3;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;

/**
 * Utility class to A2AHttpResponse to appropriate A2A error types
 */
public class RestErrorMapper_v0_3 {
    private static final Logger LOGGER = Logger.getLogger(RestErrorMapper_v0_3.class.getName());

    public static A2AClientException_v0_3 mapRestError(A2AHttpResponse response) {
        return mapRestError(response.body(), response.status(), response.headers().toMap());
    }

    public static A2AClientException_v0_3 mapRestError(String body, int code) {
        return mapRestError(body, code, Map.of());
    }

    private static A2AClientException_v0_3 mapRestError(String body, int code, Map<String, List<String>> headers) {
        try {
            if (body != null && !body.isBlank()) {
                JsonElement node = JsonUtil_v0_3.fromJson(body, JsonElement.class);
                if (node != null && node.isJsonObject()) {
                    String className = safeGetString(node.getAsJsonObject(), "error");
                    String errorMessage = safeGetString(node.getAsJsonObject(), "message");
                    A2AClientException_v0_3 mapped = mapRestError(className, errorMessage, code);
                    if (!(mapped.getCause() instanceof A2AClientHTTPError_v0_3)) {
                        return mapped;
                    }
                }
            }
        } catch (JsonProcessingException_v0_3 e) {
            // A non-JSON error body is still an HTTP failure with useful response details.
            LOGGER.log(Level.SEVERE, "Failed to parse REST error response body as JSON", e);
        }
        return httpError(body, code, headers);
    }

    private static A2AClientException_v0_3 httpError(String body, int code, Map<String, List<String>> headers) {
        String message = "HTTP " + code + (body == null || body.isBlank() ? "" : ": " + body);
        return new A2AClientException_v0_3(message, new A2AClientHTTPError_v0_3(code, message, body, headers));
    }

    private static String safeGetString(JsonObject obj, String fieldName) {
        if (obj.has(fieldName)) {
            JsonElement element = obj.get(fieldName);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }
        return "";
    }

    public static A2AClientException_v0_3 mapRestError(String className, String errorMessage, int code) {
        return switch (className) {
            case "org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3" -> new A2AClientException_v0_3(errorMessage, new TaskNotFoundError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3" -> new A2AClientException_v0_3(errorMessage, new AuthenticatedExtendedCardNotConfiguredError_v0_3(null, errorMessage, null));
            case "org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3" -> new A2AClientException_v0_3(errorMessage, new ContentTypeNotSupportedError_v0_3(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InternalError_v0_3" -> new A2AClientException_v0_3(errorMessage, new InternalError_v0_3(errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3" -> new A2AClientException_v0_3(errorMessage, new InvalidAgentResponseError_v0_3(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3" -> new A2AClientException_v0_3(errorMessage, new InvalidParamsError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3" -> new A2AClientException_v0_3(errorMessage, new InvalidRequestError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3" -> new A2AClientException_v0_3(errorMessage, new JSONParseError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3" -> new A2AClientException_v0_3(errorMessage, new MethodNotFoundError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3" -> new A2AClientException_v0_3(errorMessage, new PushNotificationNotSupportedError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3" -> new A2AClientException_v0_3(errorMessage, new TaskNotCancelableError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3" -> new A2AClientException_v0_3(errorMessage, new UnsupportedOperationError_v0_3());
            default -> httpError(errorMessage, code, Map.of());
        };
    }
}
