package io.a2a.client.transport.grpc;

import io.a2a.common.A2AErrorMessages;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ExtensionSupportRequiredError;
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
import io.grpc.Status;

/**
 * Utility class to map gRPC exceptions to appropriate A2A error types
 */
public class GrpcErrorMapper {

    public static A2AClientException mapGrpcError(Throwable e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException mapGrpcError(Throwable e, String errorPrefix) {
        Status status = Status.fromThrowable(e);
        Status.Code code = status.getCode();
        String description = status.getDescription();
        
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
            } else if (description.contains("ExtendedCardNotConfiguredError")) {
                return new A2AClientException(errorPrefix + description, new ExtendedAgentCardNotConfiguredError(null, description, null));
            } else if (description.contains("ExtensionSupportRequiredError")) {
                return new A2AClientException(errorPrefix + description, new ExtensionSupportRequiredError(null, description, null));
            } else if (description.contains("VersionNotSupportedError")) {
                return new A2AClientException(errorPrefix + description, new VersionNotSupportedError(null, description, null));
            }
        }
        
        // Fall back to mapping based on status code
        switch (code) {
            case NOT_FOUND:
                return new A2AClientException(errorPrefix + (description != null ? description : e.getMessage()), new TaskNotFoundError());
            case UNIMPLEMENTED:
                return new A2AClientException(errorPrefix + (description != null ? description : e.getMessage()), new UnsupportedOperationError());
            case INVALID_ARGUMENT:
                return new A2AClientException(errorPrefix + (description != null ? description : e.getMessage()), new InvalidParamsError());
            case INTERNAL:
                return new A2AClientException(errorPrefix + (description != null ? description : e.getMessage()), new io.a2a.spec.InternalError(null, e.getMessage(), null));
            case UNAUTHENTICATED:
                return new A2AClientException(errorPrefix + A2AErrorMessages.AUTHENTICATION_FAILED);
            case PERMISSION_DENIED:
                return new A2AClientException(errorPrefix + A2AErrorMessages.AUTHORIZATION_FAILED);
            default:
                return new A2AClientException(errorPrefix + e.getMessage(), e);
        }
    }
}
