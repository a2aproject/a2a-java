package org.a2aproject.sdk.compat03.conversion;

import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.compat03.spec.InternalError;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.JSONParseError;
import org.a2aproject.sdk.compat03.spec.JSONRPCError;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.A2AError;

/**
 * Utility for converting v1.0 A2AError instances to v0.3 JSONRPCError instances.
 * <p>
 * This converter preserves specific error types to ensure proper status code mapping
 * in transport handlers (REST HTTP status codes, gRPC status codes, etc.).
 * </p>
 */
public final class ErrorConverter {

    private ErrorConverter() {
        // Utility class
    }

    /**
     * Converts a v1.0 A2AError to a v0.3 JSONRPCError.
     * <p>
     * Since A2AError in v0.3 is an interface and JSONRPCError is the concrete implementation,
     * we need to convert the v1.0 A2AError to the v0.3 JSONRPCError type.
     * This method preserves specific error types by using instanceof checks to map
     * v1.0 errors to their v0.3 equivalents.
     * </p>
     *
     * @param v10Error the v1.0 A2AError to convert
     * @return the equivalent v0.3 JSONRPCError, preserving the specific error type
     */
    public static JSONRPCError convertA2AError(A2AError v10Error) {
        // A2AError from v1.0 has: code, message (via getMessage()), details
        // JSONRPCError from v0.3 has: code, message (via getMessage()), data
        // Preserve exact error code, message, and details from v1.0 error

        // Preserve specific error types by mapping v1.0 errors to v0.3 equivalents
        if (v10Error instanceof org.a2aproject.sdk.spec.TaskNotFoundError) {
            return new TaskNotFoundError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.UnsupportedOperationError) {
            return new UnsupportedOperationError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.TaskNotCancelableError) {
            return new TaskNotCancelableError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.InvalidParamsError) {
            return new InvalidParamsError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.InvalidRequestError) {
            return new InvalidRequestError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.InternalError) {
            return new InternalError(v10Error.getMessage());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.InvalidAgentResponseError) {
            return new InvalidAgentResponseError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.ContentTypeNotSupportedError) {
            return new ContentTypeNotSupportedError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.PushNotificationNotSupportedError) {
            return new PushNotificationNotSupportedError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.MethodNotFoundError) {
            return new MethodNotFoundError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.JSONParseError) {
            return new JSONParseError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError) {
            return new AuthenticatedExtendedCardNotConfiguredError(
                    v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        }

        // Fallback to generic JSONRPCError for unmapped types
        return new JSONRPCError(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
    }
}
