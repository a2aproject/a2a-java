package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
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
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Utility class to map gRPC StatusRuntimeException to appropriate A2A error types
 */
public class GrpcErrorMapper_v0_3 {

    // Overload for StatusRuntimeException (original 0.3.x signature)
    public static A2AClientException_v0_3 mapGrpcError(StatusRuntimeException e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException_v0_3 mapGrpcError(StatusRuntimeException e, String errorPrefix) {
        return mapGrpcErrorInternal(e.getStatus().getCode(), e.getStatus().getDescription(), e, errorPrefix);
    }

    // Overload for StatusException (gRPC 1.77+ compatibility)
    public static A2AClientException_v0_3 mapGrpcError(StatusException e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException_v0_3 mapGrpcError(StatusException e, String errorPrefix) {
        return mapGrpcErrorInternal(e.getStatus().getCode(), e.getStatus().getDescription(), e, errorPrefix);
    }

    // Dispatcher for multi-catch (StatusRuntimeException | StatusException)
    public static A2AClientException_v0_3 mapGrpcError(Exception e, String errorPrefix) {
        if (e instanceof StatusRuntimeException) {
            return mapGrpcError((StatusRuntimeException) e, errorPrefix);
        } else if (e instanceof StatusException) {
            return mapGrpcError((StatusException) e, errorPrefix);
        } else {
            return new A2AClientException_v0_3(errorPrefix + e.getMessage(), e);
        }
    }

    private static A2AClientException_v0_3 mapGrpcErrorInternal(Status.Code code, @org.jspecify.annotations.Nullable String description, @org.jspecify.annotations.Nullable Throwable cause, String errorPrefix) {
        
        // Extract the actual error type from the description if possible
        // (using description because the same code can map to multiple errors -
        // see GrpcHandler#handleError)
        if (description != null) {
            if (description.contains("TaskNotFoundError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new TaskNotFoundError_v0_3());
            } else if (description.contains("UnsupportedOperationError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new UnsupportedOperationError_v0_3());
            } else if (description.contains("InvalidParamsError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new InvalidParamsError_v0_3());
            } else if (description.contains("InvalidRequestError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new InvalidRequestError_v0_3());
            } else if (description.contains("MethodNotFoundError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new MethodNotFoundError_v0_3());
            } else if (description.contains("TaskNotCancelableError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new TaskNotCancelableError_v0_3());
            } else if (description.contains("PushNotificationNotSupportedError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new PushNotificationNotSupportedError_v0_3());
            } else if (description.contains("JSONParseError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new JSONParseError_v0_3());
            } else if (description.contains("ContentTypeNotSupportedError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new ContentTypeNotSupportedError_v0_3(null, description, null));
            } else if (description.contains("InvalidAgentResponseError")) {
                return new A2AClientException_v0_3(errorPrefix + description, new InvalidAgentResponseError_v0_3(null, description, null));
            }
        }
        
        // Fall back to mapping based on status code
        String message = description != null ? description : (cause != null ? cause.getMessage() : "Unknown error");
        switch (code) {
            case NOT_FOUND:
                return new A2AClientException_v0_3(errorPrefix + message, new TaskNotFoundError_v0_3());
            case UNIMPLEMENTED:
                return new A2AClientException_v0_3(errorPrefix + message, new UnsupportedOperationError_v0_3());
            case INVALID_ARGUMENT:
                return new A2AClientException_v0_3(errorPrefix + message, new InvalidParamsError_v0_3());
            case INTERNAL:
                return new A2AClientException_v0_3(errorPrefix + message, new InternalError_v0_3(null, message, null));
            case UNAUTHENTICATED:
                return new A2AClientException_v0_3(errorPrefix + A2AErrorMessages.AUTHENTICATION_FAILED);
            case PERMISSION_DENIED:
                return new A2AClientException_v0_3(errorPrefix + A2AErrorMessages.AUTHORIZATION_FAILED);
            default:
                return new A2AClientException_v0_3(errorPrefix + message, cause);
        }
    }
}
