package io.a2a.client.transport.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ContentTypeNotSupportedError;
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
            case "io.a2a.spec.TaskNotFoundError" -> new A2AClientException(errorMessage, new TaskNotFoundError());
            case "io.a2a.spec.ExtendedCardNotConfiguredError" -> new A2AClientException(errorMessage, new ExtendedAgentCardNotConfiguredError(null, errorMessage, null));
            case "io.a2a.spec.ContentTypeNotSupportedError" -> new A2AClientException(errorMessage, new ContentTypeNotSupportedError(null, null, errorMessage));
            case "io.a2a.spec.InternalError" -> new A2AClientException(errorMessage, new InternalError(errorMessage));
            case "io.a2a.spec.InvalidAgentResponseError" -> new A2AClientException(errorMessage, new InvalidAgentResponseError(null, null, errorMessage));
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
