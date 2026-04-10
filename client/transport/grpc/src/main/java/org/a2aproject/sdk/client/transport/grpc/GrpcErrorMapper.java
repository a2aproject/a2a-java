package org.a2aproject.sdk.client.transport.grpc;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jspecify.annotations.Nullable;
import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AErrorCodes;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.ExtensionSupportRequiredError;
import org.a2aproject.sdk.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.VersionNotSupportedError;
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;

/**
 * Utility class to map gRPC exceptions to appropriate A2A error types.
 * <p>
 * Extracts {@code google.rpc.ErrorInfo} from gRPC status details to identify the
 * specific A2A error type via the {@code reason} field.
 */
public class GrpcErrorMapper {

    private static final Map<String, A2AErrorCodes> REASON_MAP = Map.ofEntries(
            Map.entry("TASK_NOT_FOUND", A2AErrorCodes.TASK_NOT_FOUND),
            Map.entry("TASK_NOT_CANCELABLE", A2AErrorCodes.TASK_NOT_CANCELABLE),
            Map.entry("PUSH_NOTIFICATION_NOT_SUPPORTED", A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED),
            Map.entry("UNSUPPORTED_OPERATION", A2AErrorCodes.UNSUPPORTED_OPERATION),
            Map.entry("CONTENT_TYPE_NOT_SUPPORTED", A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED),
            Map.entry("INVALID_AGENT_RESPONSE", A2AErrorCodes.INVALID_AGENT_RESPONSE),
            Map.entry("EXTENDED_AGENT_CARD_NOT_CONFIGURED", A2AErrorCodes.EXTENDED_AGENT_CARD_NOT_CONFIGURED),
            Map.entry("EXTENSION_SUPPORT_REQUIRED", A2AErrorCodes.EXTENSION_SUPPORT_REQUIRED),
            Map.entry("VERSION_NOT_SUPPORTED", A2AErrorCodes.VERSION_NOT_SUPPORTED),
            Map.entry("INVALID_REQUEST", A2AErrorCodes.INVALID_REQUEST),
            Map.entry("METHOD_NOT_FOUND", A2AErrorCodes.METHOD_NOT_FOUND),
            Map.entry("INVALID_PARAMS", A2AErrorCodes.INVALID_PARAMS),
            Map.entry("INTERNAL", A2AErrorCodes.INTERNAL),
            Map.entry("JSON_PARSE", A2AErrorCodes.JSON_PARSE)
    );

    public static A2AClientException mapGrpcError(Throwable e) {
        return mapGrpcError(e, "gRPC error: ");
    }

    public static A2AClientException mapGrpcError(Throwable e, String errorPrefix) {
        Status status = Status.fromThrowable(e);
        Status.Code code = status.getCode();
        String message = status.getDescription();

        // Try to extract ErrorInfo from status details
        com.google.rpc.@Nullable ErrorInfo errorInfo = extractErrorInfo(e);
        if (errorInfo != null) {
            A2AErrorCodes errorCode = REASON_MAP.get(errorInfo.getReason());
            if (errorCode != null) {
                String errorMessage = message != null ? message : (e.getMessage() != null ? e.getMessage() : "");
                Map<String, Object> metadata = errorInfo.getMetadataMap().isEmpty() ? null
                        : new HashMap<String, Object>(errorInfo.getMetadataMap());
                return mapByErrorCode(errorCode, errorPrefix + errorMessage, errorMessage, metadata);
            }
        }

        // Fall back to mapping based on status code
        String desc = message != null ? message : e.getMessage() == null ? "" : e.getMessage();
        return switch (code) {
            case NOT_FOUND -> new A2AClientException(errorPrefix + desc, new TaskNotFoundError());
            case UNIMPLEMENTED -> new A2AClientException(errorPrefix + desc, new UnsupportedOperationError());
            case INVALID_ARGUMENT -> new A2AClientException(errorPrefix + desc, new InvalidParamsError());
            case INTERNAL -> new A2AClientException(errorPrefix + desc, new org.a2aproject.sdk.spec.InternalError(null, desc, null));
            case UNAUTHENTICATED -> new A2AClientException(errorPrefix + A2AErrorMessages.AUTHENTICATION_FAILED);
            case PERMISSION_DENIED -> new A2AClientException(errorPrefix + A2AErrorMessages.AUTHORIZATION_FAILED);
            default -> new A2AClientException(errorPrefix + e.getMessage(), e);
        };
    }

    private static com.google.rpc.@Nullable ErrorInfo extractErrorInfo(Throwable e) {
        try {
            com.google.rpc.Status rpcStatus = StatusProto.fromThrowable(e);
            if (rpcStatus != null) {
                for (com.google.protobuf.Any detail : rpcStatus.getDetailsList()) {
                    if (detail.is(com.google.rpc.ErrorInfo.class)) {
                        com.google.rpc.ErrorInfo errorInfo = detail.unpack(com.google.rpc.ErrorInfo.class);
                        if ("a2a-protocol.org".equals(errorInfo.getDomain())) {
                            return errorInfo;
                        }
                    }
                }
            }
        } catch (InvalidProtocolBufferException ignored) {
            // Fall through to status code-based mapping
        }
        return null;
    }

    private static A2AClientException mapByErrorCode(A2AErrorCodes errorCode, String fullMessage, String errorMessage, @Nullable Map<String, Object> metadata) {
        return switch (errorCode) {
            case TASK_NOT_FOUND -> new A2AClientException(fullMessage, new TaskNotFoundError(errorMessage, metadata));
            case TASK_NOT_CANCELABLE -> new A2AClientException(fullMessage, new TaskNotCancelableError(null, errorMessage, metadata));
            case PUSH_NOTIFICATION_NOT_SUPPORTED -> new A2AClientException(fullMessage, new PushNotificationNotSupportedError(null, errorMessage, metadata));
            case UNSUPPORTED_OPERATION -> new A2AClientException(fullMessage, new UnsupportedOperationError(null, errorMessage, metadata));
            case CONTENT_TYPE_NOT_SUPPORTED -> new A2AClientException(fullMessage, new ContentTypeNotSupportedError(null, errorMessage, metadata));
            case INVALID_AGENT_RESPONSE -> new A2AClientException(fullMessage, new InvalidAgentResponseError(null, errorMessage, metadata));
            case EXTENDED_AGENT_CARD_NOT_CONFIGURED -> new A2AClientException(fullMessage, new ExtendedAgentCardNotConfiguredError(null, errorMessage, metadata));
            case EXTENSION_SUPPORT_REQUIRED -> new A2AClientException(fullMessage, new ExtensionSupportRequiredError(null, errorMessage, metadata));
            case VERSION_NOT_SUPPORTED -> new A2AClientException(fullMessage, new VersionNotSupportedError(null, errorMessage, metadata));
            case INVALID_REQUEST -> new A2AClientException(fullMessage, new InvalidRequestError(null, errorMessage, metadata));
            case JSON_PARSE -> new A2AClientException(fullMessage, new JSONParseError(null, errorMessage, metadata));
            case METHOD_NOT_FOUND -> new A2AClientException(fullMessage, new MethodNotFoundError(null, errorMessage, metadata));
            case INVALID_PARAMS -> new A2AClientException(fullMessage, new InvalidParamsError(null, errorMessage, metadata));
            case INTERNAL -> new A2AClientException(fullMessage, new org.a2aproject.sdk.spec.InternalError(null, errorMessage, metadata));
        };
    }
}
