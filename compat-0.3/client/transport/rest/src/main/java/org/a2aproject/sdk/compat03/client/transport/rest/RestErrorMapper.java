package org.a2aproject.sdk.compat03.client.transport.rest;

import com.google.gson.JsonObject;
import org.a2aproject.sdk.compat03.json.JsonProcessingException;
import org.a2aproject.sdk.compat03.json.JsonUtil;
import org.a2aproject.sdk.compat03.client.http.A2AHttpResponse;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.compat03.spec.InternalError;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.JSONParseError;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to A2AHttpResponse to appropriate A2A error types
 */
public class RestErrorMapper {

    public static A2AClientException mapRestError(A2AHttpResponse response) {
        return RestErrorMapper.mapRestError(response.body(), response.status());
    }

    public static A2AClientException mapRestError(String body, int code) {
        try {
            if (body != null && !body.isBlank()) {
                JsonObject node = JsonUtil.fromJson(body, JsonObject.class);
                String className = node.has("error") ? node.get("error").getAsString() : "";
                String errorMessage = node.has("message") ? node.get("message").getAsString() : "";
                return mapRestError(className, errorMessage, code);
            }
            return mapRestError("", "", code);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(RestErrorMapper.class.getName()).log(Level.SEVERE, null, ex);
            return new A2AClientException("Failed to parse error response: " + ex.getMessage());
        }
    }

    public static A2AClientException mapRestError(String className, String errorMessage, int code) {
        return switch (className) {
            case "org.a2aproject.sdk.compat03.spec.TaskNotFoundError" -> new A2AClientException(errorMessage, new TaskNotFoundError());
            case "org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError" -> new A2AClientException(errorMessage, new AuthenticatedExtendedCardNotConfiguredError(null, errorMessage, null));
            case "org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError" -> new A2AClientException(errorMessage, new ContentTypeNotSupportedError(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InternalError" -> new A2AClientException(errorMessage, new InternalError(errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError" -> new A2AClientException(errorMessage, new InvalidAgentResponseError(null, null, errorMessage));
            case "org.a2aproject.sdk.compat03.spec.InvalidParamsError" -> new A2AClientException(errorMessage, new InvalidParamsError());
            case "org.a2aproject.sdk.compat03.spec.InvalidRequestError" -> new A2AClientException(errorMessage, new InvalidRequestError());
            case "org.a2aproject.sdk.compat03.spec.JSONParseError" -> new A2AClientException(errorMessage, new JSONParseError());
            case "org.a2aproject.sdk.compat03.spec.MethodNotFoundError" -> new A2AClientException(errorMessage, new MethodNotFoundError());
            case "org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError" -> new A2AClientException(errorMessage, new PushNotificationNotSupportedError());
            case "org.a2aproject.sdk.compat03.spec.TaskNotCancelableError" -> new A2AClientException(errorMessage, new TaskNotCancelableError());
            case "org.a2aproject.sdk.compat03.spec.UnsupportedOperationError" -> new A2AClientException(errorMessage, new UnsupportedOperationError());
            default -> new A2AClientException(errorMessage);
        };
    }
}
