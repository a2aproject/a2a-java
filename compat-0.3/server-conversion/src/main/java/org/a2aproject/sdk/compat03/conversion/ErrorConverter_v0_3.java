package org.a2aproject.sdk.compat03.conversion;

import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;

/**
 * Utility for converting v1.0 A2AError instances to v0.3 JSONRPCError instances.
 * <p>
 * This converter preserves specific error types to ensure proper status code mapping
 * in transport handlers (REST HTTP status codes, gRPC status codes, etc.).
 * </p>
 */
public final class ErrorConverter_v0_3 {

    private ErrorConverter_v0_3() {
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
    public static JSONRPCError_v0_3 convertA2AError(A2AError v10Error) {
        // A2AError from v1.0 has: code, message (via getMessage()), details
        // JSONRPCError from v0.3 has: code, message (via getMessage()), data
        // Preserve exact error code, message, and details from v1.0 error

        // Preserve specific error types by mapping v1.0 errors to v0.3 equivalents
        if (v10Error instanceof TaskNotFoundError) {
            return new TaskNotFoundError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof UnsupportedOperationError) {
            return new UnsupportedOperationError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof TaskNotCancelableError) {
            return new TaskNotCancelableError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof InvalidParamsError) {
            return new InvalidParamsError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof InvalidRequestError) {
            return new InvalidRequestError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof InternalError) {
            return new InternalError_v0_3(v10Error.getMessage());
        } else if (v10Error instanceof InvalidAgentResponseError) {
            return new InvalidAgentResponseError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof ContentTypeNotSupportedError) {
            return new ContentTypeNotSupportedError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof PushNotificationNotSupportedError) {
            return new PushNotificationNotSupportedError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof MethodNotFoundError) {
            return new MethodNotFoundError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof JSONParseError) {
            return new JSONParseError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        } else if (v10Error instanceof ExtendedAgentCardNotConfiguredError) {
            return new AuthenticatedExtendedCardNotConfiguredError_v0_3(
                    v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
        }

        // Fallback to generic JSONRPCError for unmapped types
        return new JSONRPCError_v0_3(v10Error.getCode(), v10Error.getMessage(), v10Error.getDetails());
    }
}
