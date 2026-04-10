package org.a2aproject.sdk.compat03.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

/**
 * Tests for GrpcErrorMapper - verifies correct mapping of gRPC StatusRuntimeException
 * to v0.3 A2A error types based on description string matching and status codes.
 */
public class GrpcErrorMapper_v0_3_Test {

    @Test
    public void testTaskNotFoundErrorByDescription() {
        String errorMessage = "TaskNotFoundError: Task task-123 not found";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError_v0_3.class, result.getCause());
        assertTrue(result.getMessage().contains(errorMessage));
    }

    @Test
    public void testTaskNotFoundErrorByStatusCode() {
        // Test fallback to status code mapping when description doesn't contain error type
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription("Generic not found error")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError_v0_3.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorByDescription() {
        String errorMessage = "UnsupportedOperationError: Operation not supported";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError_v0_3.class, result.getCause());
    }

    @Test
    public void testUnsupportedOperationErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription("Generic unimplemented error")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(UnsupportedOperationError_v0_3.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorByDescription() {
        String errorMessage = "InvalidParamsError: Invalid parameters provided";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError_v0_3.class, result.getCause());
    }

    @Test
    public void testInvalidParamsErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription("Generic invalid argument")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidParamsError_v0_3.class, result.getCause());
    }

    @Test
    public void testInvalidRequestError() {
        String errorMessage = "InvalidRequestError: Request is malformed";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidRequestError_v0_3.class, result.getCause());
    }

    @Test
    public void testMethodNotFoundError() {
        String errorMessage = "MethodNotFoundError: Method does not exist";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(MethodNotFoundError_v0_3.class, result.getCause());
    }

    @Test
    public void testTaskNotCancelableError() {
        String errorMessage = "TaskNotCancelableError: Task cannot be cancelled";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotCancelableError_v0_3.class, result.getCause());
    }

    @Test
    public void testPushNotificationNotSupportedError() {
        String errorMessage = "PushNotificationNotSupportedError: Push notifications not supported";
        StatusRuntimeException grpcException = Status.UNIMPLEMENTED
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(PushNotificationNotSupportedError_v0_3.class, result.getCause());
    }

    @Test
    public void testJSONParseError() {
        String errorMessage = "JSONParseError: Failed to parse JSON";
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(JSONParseError_v0_3.class, result.getCause());
    }

    @Test
    public void testContentTypeNotSupportedError() {
        String errorMessage = "ContentTypeNotSupportedError: Content type application/xml not supported";
        StatusRuntimeException grpcException = Status.INVALID_ARGUMENT
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(ContentTypeNotSupportedError_v0_3.class, result.getCause());

        ContentTypeNotSupportedError_v0_3 contentTypeError = (ContentTypeNotSupportedError_v0_3) result.getCause();
        assertNotNull(contentTypeError.getMessage());
        assertTrue(contentTypeError.getMessage().contains("Content type application/xml not supported"));
    }

    @Test
    public void testInvalidAgentResponseError() {
        String errorMessage = "InvalidAgentResponseError: Agent response is invalid";
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription(errorMessage)
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InvalidAgentResponseError_v0_3.class, result.getCause());

        InvalidAgentResponseError_v0_3 agentResponseError = (InvalidAgentResponseError_v0_3) result.getCause();
        assertNotNull(agentResponseError.getMessage());
        assertTrue(agentResponseError.getMessage().contains("Agent response is invalid"));
    }

    @Test
    public void testInternalErrorByStatusCode() {
        StatusRuntimeException grpcException = Status.INTERNAL
                .withDescription("Internal server error")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(InternalError_v0_3.class, result.getCause());
    }

    @Test
    public void testCustomErrorPrefix() {
        String errorMessage = "TaskNotFoundError: Task not found";
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .withDescription(errorMessage)
                .asRuntimeException();

        String customPrefix = "Custom Error: ";
        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException, customPrefix);

        assertNotNull(result);
        assertTrue(result.getMessage().startsWith(customPrefix));
        assertInstanceOf(TaskNotFoundError_v0_3.class, result.getCause());
    }

    @Test
    public void testAuthenticationFailed() {
        StatusRuntimeException grpcException = Status.UNAUTHENTICATED
                .withDescription("Authentication failed")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Authentication failed"));
    }

    @Test
    public void testAuthorizationFailed() {
        StatusRuntimeException grpcException = Status.PERMISSION_DENIED
                .withDescription("Permission denied")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Authorization failed"));
    }

    @Test
    public void testUnknownStatusCode() {
        StatusRuntimeException grpcException = Status.DEADLINE_EXCEEDED
                .withDescription("Request timeout")
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertTrue(result.getMessage().contains("Request timeout"));
    }

    @Test
    public void testNullDescription() {
        StatusRuntimeException grpcException = Status.NOT_FOUND
                .asRuntimeException();

        A2AClientException_v0_3 result = GrpcErrorMapper_v0_3.mapGrpcError(grpcException);

        assertNotNull(result);
        assertNotNull(result.getCause());
        assertInstanceOf(TaskNotFoundError_v0_3.class, result.getCause());
    }
}
