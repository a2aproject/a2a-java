package org.a2aproject.sdk.compat03.client.adapter;

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.InternalError;

/** Maps errors from a 0.3 delegate into the public 1.0 exception hierarchy. */
public final class Compat03ClientErrorMapper {

    private Compat03ClientErrorMapper() {
    }

    public static A2AClientException toV10(A2AClientException_v0_3 exception) {
        Throwable cause = exception.getCause();
        String message = exception.getMessage() == null ? "A2A 0.3 client operation failed" : exception.getMessage();
        if (cause instanceof org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3) {
            return new A2AClientException(message,
                    new org.a2aproject.sdk.spec.TaskNotFoundError(null, null));
        }
        if (cause instanceof org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3) {
            return new A2AClientException(message, new InvalidParamsError(message));
        }
        if (cause instanceof org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3) {
            return new A2AClientException(message, new InvalidRequestError(null, message, null));
        }
        if (cause instanceof org.a2aproject.sdk.compat03.spec.InternalError_v0_3) {
            return new A2AClientException(message, new InternalError(message));
        }
        return new A2AClientException(message, new A2AError(
                org.a2aproject.sdk.spec.A2AErrorCodes.INTERNAL.code(), message, null));
    }
}
