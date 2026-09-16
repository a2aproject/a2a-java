package org.a2aproject.sdk.compat03.client.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;

/** Maps errors from a 0.3 delegate into the public 1.0 exception hierarchy. */
public final class Compat03ClientErrorMapper {

    private Compat03ClientErrorMapper() {
    }

    public static A2AClientException toV10(A2AClientException_v0_3 exception) {
        Throwable cause = exception.getCause();
        String message = exception.getMessage() == null ? "A2A 0.3 client operation failed" : exception.getMessage();
        if (cause instanceof JSONRPCError_v0_3 error) {
            return new A2AClientException(message, toV10(error));
        }
        return cause == null ? new A2AClientException(message) : new A2AClientException(message, cause);
    }

    public static A2AError toV10(JSONRPCError_v0_3 error) {
        Integer code = error.getCode();
        String message = error.getMessage();
        Map<String, Object> details = details(error.getData());
        return switch (code) {
            case A2AErrorCodes_v0_3.JSON_PARSE_ERROR_CODE -> new JSONParseError(code, message, details);
            case A2AErrorCodes_v0_3.INVALID_REQUEST_ERROR_CODE -> new InvalidRequestError(code, message, details);
            case A2AErrorCodes_v0_3.METHOD_NOT_FOUND_ERROR_CODE -> new MethodNotFoundError(code, message, details);
            case A2AErrorCodes_v0_3.INVALID_PARAMS_ERROR_CODE -> new InvalidParamsError(code, message, details);
            case A2AErrorCodes_v0_3.INTERNAL_ERROR_CODE -> new InternalError(code, message, details);
            case A2AErrorCodes_v0_3.TASK_NOT_FOUND_ERROR_CODE -> new TaskNotFoundError(message, details);
            case A2AErrorCodes_v0_3.TASK_NOT_CANCELABLE_ERROR_CODE -> new TaskNotCancelableError(code, message, details);
            case A2AErrorCodes_v0_3.PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE ->
                new PushNotificationNotSupportedError(code, message, details);
            case A2AErrorCodes_v0_3.UNSUPPORTED_OPERATION_ERROR_CODE -> new UnsupportedOperationError(code, message, details);
            case A2AErrorCodes_v0_3.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE ->
                new ContentTypeNotSupportedError(code, message, details);
            case A2AErrorCodes_v0_3.INVALID_AGENT_RESPONSE_ERROR_CODE ->
                new InvalidAgentResponseError(code, message, details);
            case A2AErrorCodes_v0_3.AUTHENTICATED_EXTENDED_CARD_NOT_CONFIGURED_ERROR_CODE ->
                new ExtendedAgentCardNotConfiguredError(code, message, details);
            default -> new A2AError(code, message, details);
        };
    }

    private static Map<String, Object> details(Object data) {
        if (!(data instanceof Map<?, ?> rawDetails)) {
            return Map.of();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        rawDetails.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                details.put(stringKey, value);
            }
        });
        return details;
    }
}
