package io.a2a.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.a2a.spec.A2AClientException;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.ExtendedCardNotConfiguredError;
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
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * Tests for GrpcErrorMapper - verifies correct unmarshalling of gRPC errors to A2A error types
 */
public class GrpcErrorMapperTest {

    @Test
    public void testExtensionSupportRequiredErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with ExtensionSupportRequiredError in description
        String errorMessage = "ExtensionSupportRequiredError: Extension required: https://example.com/test-extension";
        StatusRuntimeException grpcException = Status.FAILED_PRECONDITION
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
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
        // Create a gRPC StatusRuntimeException with VersionNotSupportedError in description
        String errorMessage = "VersionNotSupportedError: Version 2.0 is not supported";
        StatusRuntimeException grpcException = Status.FAILED_PRECONDITION
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(VersionNotSupportedError.class, result.getCause());

        VersionNotSupportedError versionError = (VersionNotSupportedError) result.getCause();
        assertNotNull(versionError.getMessage());
        assertTrue(versionError.getMessage().contains("Version 2.0 is not supported"));
    }

    @Test
    public void testExtendedCardNotConfiguredErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with ExtendedCardNotConfiguredError in description
        String errorMessage = "ExtendedCardNotConfiguredError: Extended card not configured for this agent";
        StatusRuntimeException grpcException = Status.FAILED_PRECONDITION
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ExtendedCardNotConfiguredError.class, result.getCause());

        ExtendedCardNotConfiguredError extendedCardError = (ExtendedCardNotConfiguredError) result.getCause();
        assertNotNull(extendedCardError.getMessage());
        assertTrue(extendedCardError.getMessage().contains("Extended card not configured"));
    }

    @Test
    public void testTaskNotFoundErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with TaskNotFoundError in description
        String errorMessage = "TaskNotFoundError: Task task-123 not found";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with UnsupportedOperationError in description
        String errorMessage = "UnsupportedOperationError: Operation not supported";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with InvalidParamsError in description
        String errorMessage = "InvalidParamsError: Invalid parameters provided";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError.class, result.getCause());
    }

    @Test
    public void testContentTypeNotSupportedErrorUnmarshalling() {
        // Create a gRPC StatusRuntimeException with ContentTypeNotSupportedError in description
        String errorMessage = "ContentTypeNotSupportedError: Content type application/xml not supported";
        StatusRuntimeException grpcException = Status.FAILED_PRECONDITION
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify the result
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ContentTypeNotSupportedError.class, result.getCause());

        ContentTypeNotSupportedError contentTypeError = (ContentTypeNotSupportedError) result.getCause();
        assertNotNull(contentTypeError.getMessage());
        assertTrue(contentTypeError.getMessage().contains("Content type application/xml not supported"));
    }

    @Test
    public void testFallbackToStatusCodeMapping() {
        // Create a gRPC StatusRuntimeException without specific error type in description
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription("Generic not found error")
                .asRuntimeException();

        // Map the gRPC error to A2A error
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        // Verify fallback to status code mapping
        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testCustomErrorPrefix() {
        // Create a gRPC StatusRuntimeException
        String errorMessage = "ExtensionSupportRequiredError: Extension required: https://example.com/ext";
        StatusRuntimeException grpcException = Status.FAILED_PRECONDITION
                .withDescription(errorMessage)
                .asRuntimeException();

        // Map with custom error prefix
        String customPrefix = "Custom Error: ";
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException, customPrefix);

        // Verify custom prefix is used
        assertNotNull(result);
        assertTrue(result.getMessage().startsWith(customPrefix));
        assertInstanceOf(ExtensionSupportRequiredError.class, result.getCause());
    }
}
