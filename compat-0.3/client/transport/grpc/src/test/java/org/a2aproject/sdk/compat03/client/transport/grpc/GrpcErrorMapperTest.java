package org.a2aproject.sdk.compat03.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.compat03.spec.InternalError;
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
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * Tests for GrpcErrorMapper - verifies correct mapping of gRPC StatusRuntimeException
 * to v0.3 A2A error types based on description string matching and status codes.
 */
public class GrpcErrorMapperTest {

    @Test
    public void testTaskNotFoundErrorByDescription() {
        String errorMessage = "TaskNotFoundError: Task task-123 not found";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
        assertTrue(result.getMessage().contains(errorMessage));
    }

    @Test
    public void testTaskNotFoundErrorByStatusCode() {
        // Test fallback to status code mapping when description doesn't contain error type
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription("Generic not found error")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorByDescription() {
        String errorMessage = "UnsupportedOperationError: Operation not supported";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription("Generic unimplemented error")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorByDescription() {
        String errorMessage = "InvalidParamsError: Invalid parameters provided";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription("Generic invalid argument")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError.class, result.getCause());
    }

    @Test
    public void testInvalidRequestError() {
        String errorMessage = "InvalidRequestError: Request is malformed";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidRequestError.class, result.getCause());
    }

    @Test
    public void testMethodNotFoundError() {
        String errorMessage = "MethodNotFoundError: Method does not exist";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(MethodNotFoundError.class, result.getCause());
    }

    @Test
    public void testTaskNotCancelableError() {
        String errorMessage = "TaskNotCancelableError: Task cannot be cancelled";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotCancelableError.class, result.getCause());
    }

    @Test
    public void testPushNotificationNotSupportedError() {
        String errorMessage = "PushNotificationNotSupportedError: Push notifications not supported";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(PushNotificationNotSupportedError.class, result.getCause());
    }

    @Test
    public void testJSONParseError() {
        String errorMessage = "JSONParseError: Failed to parse JSON";
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(JSONParseError.class, result.getCause());
    }

    @Test
    public void testContentTypeNotSupportedError() {
        String errorMessage = "ContentTypeNotSupportedError: Content type application/xml not supported";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ContentTypeNotSupportedError.class, result.getCause());

        ContentTypeNotSupportedError contentTypeError = (ContentTypeNotSupportedError) result.getCause();
        assertNotNull(contentTypeError.getMessage());
        assertTrue(contentTypeError.getMessage().contains("Content type application/xml not supported"));
    }

    @Test
    public void testInvalidAgentResponseError() {
        String errorMessage = "InvalidAgentResponseError: Agent response is invalid";
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidAgentResponseError.class, result.getCause());

        InvalidAgentResponseError agentResponseError = (InvalidAgentResponseError) result.getCause();
        assertNotNull(agentResponseError.getMessage());
        assertTrue(agentResponseError.getMessage().contains("Agent response is invalid"));
    }

    @Test
    public void testInternalErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InternalError.class, result.getCause());
    }

    @Test
    public void testCustomErrorPrefix() {
        String errorMessage = "TaskNotFoundError: Task not found";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        String customPrefix = "Custom Error: ";
        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException, customPrefix);

        assertNotNull(result);
        assertTrue(result.getMessage().startsWith(customPrefix));
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }

    @Test
    public void testAuthenticationFailed() {
        StatusRuntimeException grpcException = Status.UNAUTHENTICATED
                .withDescription("Authentication failed")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Authentication failed"));
    }

    @Test
    public void testAuthorizationFailed() {
        StatusRuntimeException grpcException = Status.PERMISSION_DENIED
                .withDescription("Permission denied")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Authorization failed"));
    }

    @Test
    public void testUnknownStatusCode() {
        StatusRuntimeException grpcException = Status.DEADLINE_EXCEEDED
                .withDescription("Request timeout")
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Request timeout"));
    }

    @Test
    public void testNullDescription() {
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .asRuntimeException();

        A2AClientException result = GrpcErrorMapper.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError.class, result.getCause());
    }
}
