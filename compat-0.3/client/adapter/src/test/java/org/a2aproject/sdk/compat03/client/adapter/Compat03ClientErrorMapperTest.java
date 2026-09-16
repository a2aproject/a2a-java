package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
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
