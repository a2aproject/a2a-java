package io.a2a.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.Any;
import com.google.rpc.ErrorInfo;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.spec.VersionNotSupportedError;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.junit.jupiter.api.Test;

/**
 * Tests for GrpcErrorMapper - verifies correct unmarshalling of gRPC errors to A2A error types
 * using google.rpc.ErrorInfo in status details.
 */
public class GrpcErrorMapperTest {

    private static StatusRuntimeException createA2AStatusException(int grpcCode, String message, String reason) {
        ErrorInfo errorInfo = ErrorInfo.newBuilder()
                .setReason(reason)
                .setDomain("a2a-protocol.org")
                .build();

        com.google.rpc.Status rpcStatus = com.google.rpc.Status.newBuilder()
                .setCode(grpcCode)
                .setMessage(message)
                .addDetails(Any.pack(errorInfo))
                .build();

        return StatusProto.toStatusRuntimeException(rpcStatus);
    }

    @Test
    public void testExtensionSupportRequiredErrorUnmarshalling() {
        String errorMessage = "Extension required: https://example.com/test-extension";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.FAILED_PRECONDITION.value(), errorMessage, "EXTENSION_SUPPORT_REQUIRED");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ExtensionSupportRequiredError.class, result.getCause());

        ExtensionSupportRequiredError extensionError = (ExtensionSupportRequiredError) result.getCause();
        assertNotNull(extensionError.getMessage());
        assertTrue(extensionError.getMessage().contains("https://example.com/test-extension"));
        assertTrue(result.getMessage().contains(errorMessage));
    }

    @Test
    public void testVersionNotSupportedErrorUnmarshalling() {
        String errorMessage = "Version 2.0 is not supported";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.UNIMPLEMENTED.value(), errorMessage, "VERSION_NOT_SUPPORTED");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(VersionNotSupportedError.class, result.getCause());

        VersionNotSupportedError versionError = (VersionNotSupportedError) result.getCause();
        assertNotNull(versionError.getMessage());
        assertTrue(versionError.getMessage().contains("Version 2.0 is not supported"));
    }

    @Test
    public void testExtendedCardNotConfiguredErrorUnmarshalling() {
        String errorMessage = "Extended card not configured for this agent";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.FAILED_PRECONDITION.value(), errorMessage, "EXTENDED_AGENT_CARD_NOT_CONFIGURED");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ExtendedAgentCardNotConfiguredError.class, result.getCause());

        ExtendedAgentCardNotConfiguredError extendedCardError = (ExtendedAgentCardNotConfiguredError) result.getCause();
        assertNotNull(extendedCardError.getMessage());
        assertTrue(extendedCardError.getMessage().contains("Extended card not configured"));
    }

    @Test
    public void testTaskNotFoundErrorUnmarshalling() {
        String errorMessage = "Task task-123 not found";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.NOT_FOUND.value(), errorMessage, "TASK_NOT_FOUND");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorUnmarshalling() {
        String errorMessage = "Operation not supported";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.UNIMPLEMENTED.value(), errorMessage, "UNSUPPORTED_OPERATION");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorUnmarshalling() {
        String errorMessage = "Invalid parameters provided";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.INVALID_ARGUMENT.value(), errorMessage, "INVALID_PARAMS");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError.class, result.getCause());
    }

    @Test
    public void testContentTypeNotSupportedErrorUnmarshalling() {
        String errorMessage = "Content type application/xml not supported";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.INVALID_ARGUMENT.value(), errorMessage, "CONTENT_TYPE_NOT_SUPPORTED");

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ContentTypeNotSupportedError.class, result.getCause());

        ContentTypeNotSupportedError contentTypeError = (ContentTypeNotSupportedError) result.getCause();
        assertNotNull(contentTypeError.getMessage());
        assertTrue(contentTypeError.getMessage().contains("Content type application/xml not supported"));
    }

    @Test
    public void testFallbackToStatusCodeMapping() {
        // Create a gRPC StatusRuntimeException without ErrorInfo details
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription("Generic not found error")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify fallback to status code mapping
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testCustomErrorPrefix() {
        String errorMessage = "Extension required: https://example.com/ext";
        StatusRuntimeException grpcException = createA2AStatusException(
                Status.Code.FAILED_PRECONDITION.value(), errorMessage, "EXTENSION_SUPPORT_REQUIRED");

        String customPrefix = "Custom Error: ";
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException, customPrefix);

        assertNotNull(result);
        assertTrue(result.getMessage().startsWith(customPrefix));
        assertInstanceOf(ExtensionSupportRequiredError.class, result.getCause());
    }
}
