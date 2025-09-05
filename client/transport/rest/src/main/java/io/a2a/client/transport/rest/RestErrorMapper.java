package io.a2a.client.transport.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AuthenticatedExtendedCardNotConfiguredError;
import io.a2a.spec.ContentTypeNotSupportedError;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to A2AHttpResponse to appropriate A2A error types
 */
public class RestErrorMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static A2AClientException mapRestError(A2AHttpResponse response) {
        return RestErrorMapper.mapRestError(response.body(), response.status());
    }

    public static A2AClientException mapRestError(String body, int code) {
        try {
            if (body != null && !body.isBlank()) {
                JsonNode node = OBJECT_MAPPER.readTree(body);
                String className = node.findValue("error").asText();
                String errorMessage = node.findValue("message").asText();
                return mapRestError(className, errorMessage, code);
            }
            return mapRestError("", "", code);
        } catch (JsonProcessingException ex) {
            Logger.getLogger(RestErrorMapper.class.getName()).log(Level.SEVERE, null, ex);
            return new A2AClientException("Failed to parse error response: " + ex.getMessage());
        }
    }

    public static A2AClientException mapRestError(String className, String errorMessage, int code) {
        switch (className) {
            case "io.a2a.spec.TaskNotFoundError":
                return new A2AClientException(errorMessage, new TaskNotFoundError());
            case "io.a2a.spec.AuthenticatedExtendedCardNotConfiguredError":
                return new A2AClientException(errorMessage, new AuthenticatedExtendedCardNotConfiguredError());
            case "io.a2a.spec.ContentTypeNotSupportedError":
                return new A2AClientException(errorMessage, new ContentTypeNotSupportedError(null, null, errorMessage));
            case "io.a2a.spec.InternalError":
                return new A2AClientException(errorMessage, new InternalError(errorMessage));
            case "io.a2a.spec.InvalidAgentResponseError":
                return new A2AClientException(errorMessage, new InvalidAgentResponseError(null, null, errorMessage));
            case "io.a2a.spec.InvalidParamsError":
                return new A2AClientException(errorMessage, new InvalidParamsError());
            case "io.a2a.spec.InvalidRequestError":
                return new A2AClientException(errorMessage, new InvalidRequestError());
            case "io.a2a.spec.JSONParseError":
                return new A2AClientException(errorMessage, new JSONParseError());
            case "io.a2a.spec.MethodNotFoundError":
                return new A2AClientException(errorMessage, new MethodNotFoundError());
            case "io.a2a.spec.PushNotificationNotSupportedError":
                return new A2AClientException(errorMessage, new PushNotificationNotSupportedError());
            case "io.a2a.spec.TaskNotCancelableError":
                return new A2AClientException(errorMessage, new TaskNotCancelableError());
            case "io.a2a.spec.UnsupportedOperationError":
                return new A2AClientException(errorMessage, new UnsupportedOperationError());
            default:
                return new A2AClientException(errorMessage);
        }
    }
}
