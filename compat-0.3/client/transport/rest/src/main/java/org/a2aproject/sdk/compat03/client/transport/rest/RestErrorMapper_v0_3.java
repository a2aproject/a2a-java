package org.a2aproject.sdk.compat03.client.transport.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
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

    public static A2AClientException_v0_3 mapRestError(A2AHttpResponse response) {
        return RestErrorMapper_v0_3.mapRestError(response.body(), response.status());
    }

    public static A2AClientException_v0_3 mapRestError(String body, int code) {
        try {
            if (body != null && !body.isBlank()) {
                JsonObject node = JsonUtil_v0_3.fromJson(body, JsonObject.class);
                String className = safeGetString(node, "error");
                String errorMessage = safeGetString(node, "message");
                return mapRestError(className, errorMessage, code);
            }
            return mapRestError("", "", code);
        } catch (JsonProcessingException_v0_3 ex) {
            Logger.getLogger(RestErrorMapper_v0_3.class.getName()).log(Level.SEVERE, null, ex);
            return new A2AClientException_v0_3(
                    "Failed to parse error response: " + ex.getMessage());
        }
    }

    private static String safeGetString(JsonObject obj, String fieldName) {
        if (obj.has(fieldName)) {
            JsonElement element = obj.get(fieldName);
            if (element != null
                    && element.isJsonPrimitive()
                    && element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }
        return "";
    }

    public static A2AClientException_v0_3 mapRestError(
            String className, String errorMessage, int code) {
        return switch (className) {
            case "org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new TaskNotFoundError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3" ->
                    new A2AClientException_v0_3(
                            errorMessage,
                            new AuthenticatedExtendedCardNotConfiguredError_v0_3(
                                    null, errorMessage, null));
            case "org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3" ->
                    new A2AClientException_v0_3(
                            errorMessage,
                            new ContentTypeNotSupportedError_v0_3(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InternalError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new InternalError_v0_3(errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3" ->
                    new A2AClientException_v0_3(
                            errorMessage,
                            new InvalidAgentResponseError_v0_3(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new InvalidParamsError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new InvalidRequestError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new JSONParseError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new MethodNotFoundError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3" ->
                    new A2AClientException_v0_3(
                            errorMessage, new PushNotificationNotSupportedError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new TaskNotCancelableError_v0_3());
            case "org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3" ->
                    new A2AClientException_v0_3(errorMessage, new UnsupportedOperationError_v0_3());
            default -> new A2AClientException_v0_3(errorMessage);
        };
    }
}
