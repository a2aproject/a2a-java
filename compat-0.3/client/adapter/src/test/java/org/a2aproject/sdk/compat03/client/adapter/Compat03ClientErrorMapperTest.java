package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientHTTPError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.junit.jupiter.api.Test;

class Compat03ClientErrorMapperTest {

    @Test
    void mapsUnsupportedOperationError() {
        A2AClientException_v0_3 legacy = new A2AClientException_v0_3(
                "unsupported", new UnsupportedOperationError_v0_3());

        A2AClientException mapped = Compat03ClientErrorMapper.toV10(legacy);

        assertInstanceOf(UnsupportedOperationError.class, mapped.getCause());
    }

    @Test
    void exposesHttpErrorsWrappedByTheJdkClient() {
        var httpError = new A2AClientHTTPError(401, "authentication failed", "unauthorized",
                Map.of("WWW-Authenticate", List.of("Bearer")));
        var legacy = new A2AClientException_v0_3("getTask failed", new IOException("authentication failed", httpError));
        var exception = Compat03ClientErrorMapper.toV10(legacy);
        var cause = assertInstanceOf(A2AClientHTTPError.class, exception.getCause());
        assertEquals(401, cause.getCode());
        assertEquals("unauthorized", cause.getResponseBody());
        assertEquals(List.of("Bearer"), cause.getResponseHeaders().get("www-authenticate"));
    }

    @Test
    void preservesHttpFailureDetailsForBlockingAndStreamingErrors() {
        var legacy = new A2AClientException_v0_3("HTTP 429", new A2AClientHTTPError_v0_3(
                429, "HTTP 429", "rate limited", Map.of("Retry-After", List.of("30"))));
        AtomicReference<Throwable> streaming = new AtomicReference<>();
        Compat03ClientTransportSupport.mapAsyncError(streaming::set).accept(legacy);
        for (A2AClientException exception : List.of(Compat03ClientErrorMapper.toV10(legacy),
                assertInstanceOf(A2AClientException.class, streaming.get()))) {
            var cause = assertInstanceOf(A2AClientHTTPError.class, exception.getCause());
            assertEquals(429, cause.getCode());
            assertEquals("rate limited", cause.getResponseBody());
            assertEquals(List.of("30"), cause.getResponseHeaders().get("retry-after"));
        }
    }

    @Test
    void preservesScalarAndArrayDataForSynchronousAndStreamingErrors() {
        for (Object data : List.of("missingScope", 42, true, List.of("missingScope", "write"))) {
            JSONRPCError_v0_3 legacy = new JSONRPCError_v0_3(-32602, "invalid parameters", data);

            A2AClientException synchronous = Compat03ClientErrorMapper.toV10(
                    new A2AClientException_v0_3("request failed", legacy));
            assertEquals(Map.of("data", data),
                    assertInstanceOf(A2AError.class, synchronous.getCause()).getDetails());

            AtomicReference<Throwable> received = new AtomicReference<>();
            Compat03ClientTransportSupport.mapAsyncError(received::set).accept(legacy);
            A2AClientException streaming = assertInstanceOf(A2AClientException.class, received.get());
            assertEquals(Map.of("data", data),
                    assertInstanceOf(A2AError.class, streaming.getCause()).getDetails());
        }
    }

    @Test
    void absentErrorDataRemainsEmpty() {
        assertEquals(Map.of(), Compat03ClientErrorMapper.toV10(
                new JSONRPCError_v0_3(-32602, "invalid parameters", null)).getDetails());
    }

    @Test
    void mapsGenericJsonRpcErrorsByTheirLegacyCodes() {
        List<ErrorCase> cases = List.of(
                new ErrorCase(-32700, org.a2aproject.sdk.spec.JSONParseError.class),
                new ErrorCase(-32600, org.a2aproject.sdk.spec.InvalidRequestError.class),
                new ErrorCase(-32601, org.a2aproject.sdk.spec.MethodNotFoundError.class),
                new ErrorCase(-32602, org.a2aproject.sdk.spec.InvalidParamsError.class),
                new ErrorCase(-32603, org.a2aproject.sdk.spec.InternalError.class),
                new ErrorCase(-32001, org.a2aproject.sdk.spec.TaskNotFoundError.class),
                new ErrorCase(-32002, org.a2aproject.sdk.spec.TaskNotCancelableError.class),
                new ErrorCase(-32003, org.a2aproject.sdk.spec.PushNotificationNotSupportedError.class),
                new ErrorCase(-32004, org.a2aproject.sdk.spec.UnsupportedOperationError.class),
                new ErrorCase(-32005, org.a2aproject.sdk.spec.ContentTypeNotSupportedError.class),
                new ErrorCase(-32006, org.a2aproject.sdk.spec.InvalidAgentResponseError.class),
                new ErrorCase(-32007, org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError.class));

        for (ErrorCase errorCase : cases) {
            A2AClientException mapped = Compat03ClientErrorMapper.toV10(new A2AClientException_v0_3(
                    "legacy failure", new JSONRPCError_v0_3(errorCase.code(), "legacy error", Map.of("key", "value"))));

            A2AError error = assertInstanceOf(A2AError.class, mapped.getCause());
            assertInstanceOf(errorCase.expectedType(), error);
            org.junit.jupiter.api.Assertions.assertEquals(Map.of("key", "value"), error.getDetails());
        }
    }

    private record ErrorCase(int code, Class<? extends A2AError> expectedType) {
    }
}
