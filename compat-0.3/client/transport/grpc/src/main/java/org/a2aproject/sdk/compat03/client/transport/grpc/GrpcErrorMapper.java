package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.JSONParseError;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Utility class to map gRPC StatusRuntimeException to appropriate A2A error types
 */
public class GrpcErrorMapper {

    // Overload for StatusRuntimeException (original 0.3.x signature)
    public static A2AClientException mapGrpcError(StatusRuntimeException e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException mapGrpcError(StatusRuntimeException e, String errorPrefix) {
        return mapGrpcErrorInternal(e.getStatus().getCode(), e.getStatus().getDescription(), e, errorPrefix);
    }

    // Overload for StatusException (gRPC 1.77+ compatibility)
    public static A2AClientException mapGrpcError(StatusException e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException mapGrpcError(StatusException e, String errorPrefix) {
        return mapGrpcErrorInternal(e.getStatus().getCode(), e.getStatus().getDescription(), e, errorPrefix);
    }

    // Dispatcher for multi-catch (StatusRuntimeException | StatusException)
    public static A2AClientException mapGrpcError(Exception e, String errorPrefix) {
        if (e instanceof StatusRuntimeException) {
            return mapGrpcError((StatusRuntimeException) e, errorPrefix);
        } else if (e instanceof StatusException) {
            return mapGrpcError((StatusException) e, errorPrefix);
        } else {
            return new A2AClientException(errorPrefix + e.getMessage(), e);
        }
    }

    private static A2AClientException mapGrpcErrorInternal(Status.Code code, @org.jspecify.annotations.Nullable String description, @org.jspecify.annotations.Nullable Throwable cause, String errorPrefix) {
        
        // Extract the actual error type from the description if possible
        // (using description because the same code can map to multiple errors -
        // see GrpcHandler#handleError)
        if (description != null) {
            if (description.contains("TaskNotFoundError")) {
                return new A2AClientException(errorPrefix + description, new TaskNotFoundError());
            } else if (description.contains("UnsupportedOperationError")) {
                return new A2AClientException(errorPrefix + description, new UnsupportedOperationError());
            } else if (description.contains("InvalidParamsError")) {
                return new A2AClientException(errorPrefix + description, new InvalidParamsError());
            } else if (description.contains("InvalidRequestError")) {
                return new A2AClientException(errorPrefix + description, new InvalidRequestError());
            } else if (description.contains("MethodNotFoundError")) {
                return new A2AClientException(errorPrefix + description, new MethodNotFoundError());
            } else if (description.contains("TaskNotCancelableError")) {
                return new A2AClientException(errorPrefix + description, new TaskNotCancelableError());
            } else if (description.contains("PushNotificationNotSupportedError")) {
                return new A2AClientException(errorPrefix + description, new PushNotificationNotSupportedError());
            } else if (description.contains("JSONParseError")) {
                return new A2AClientException(errorPrefix + description, new JSONParseError());
            } else if (description.contains("ContentTypeNotSupportedError")) {
                return new A2AClientException(errorPrefix + description, new ContentTypeNotSupportedError(null, description, null));
            } else if (description.contains("InvalidAgentResponseError")) {
                return new A2AClientException(errorPrefix + description, new InvalidAgentResponseError(null, description, null));
            }
        }
        
        // Fall back to mapping based on status code
        String message = description != null ? description : (cause != null ? cause.getMessage() : "Unknown error");
        switch (code) {
            case NOT_FOUND:
                return new A2AClientException(errorPrefix + message, new TaskNotFoundError());
            case UNIMPLEMENTED:
                return new A2AClientException(errorPrefix + message, new UnsupportedOperationError());
            case INVALID_ARGUMENT:
                return new A2AClientException(errorPrefix + message, new InvalidParamsError());
            case INTERNAL:
                return new A2AClientException(errorPrefix + message, new org.a2aproject.sdk.compat03.spec.InternalError(null, message, null));
            case UNAUTHENTICATED:
                return new A2AClientException(errorPrefix + A2AErrorMessages.AUTHENTICATION_FAILED);
            case PERMISSION_DENIED:
                return new A2AClientException(errorPrefix + A2AErrorMessages.AUTHORIZATION_FAILED);
            default:
                return new A2AClientException(errorPrefix + message, cause);
        }
    }
}
