package io.a2a.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import io.a2a.spec.A2AError;
import io.a2a.spec.A2AErrorCodes;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.UnsupportedOperationError;
import org.junit.jupiter.api.Test;


public class A2AErrorSerializationTest {
    @Test
    public void shouldDeserializeToCorrectA2AErrorSubclass() throws JsonProcessingException {
        String jsonTemplate = """
                {"code": %s, "message": "error", "details": {"key": "anything"}}
                """;

        record ErrorCase(int code, Class<? extends A2AError> clazz) {}

        List<ErrorCase> cases = List.of(
                new ErrorCase(A2AErrorCodes.JSON_PARSE.code(), JSONParseError.class),
                new ErrorCase(A2AErrorCodes.INVALID_REQUEST.code(), InvalidRequestError.class),
                new ErrorCase(A2AErrorCodes.METHOD_NOT_FOUND.code(), MethodNotFoundError.class),
                new ErrorCase(A2AErrorCodes.INVALID_PARAMS.code(), InvalidParamsError.class),
                new ErrorCase(A2AErrorCodes.INTERNAL.code(), InternalError.class),
                new ErrorCase(A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED.code(), PushNotificationNotSupportedError.class),
                new ErrorCase(A2AErrorCodes.UNSUPPORTED_OPERATION.code(), UnsupportedOperationError.class),
                new ErrorCase(A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED.code(), ContentTypeNotSupportedError.class),
                new ErrorCase(A2AErrorCodes.INVALID_AGENT_RESPONSE.code(), InvalidAgentResponseError.class),
                new ErrorCase(A2AErrorCodes.TASK_NOT_CANCELABLE.code(), TaskNotCancelableError.class),
                new ErrorCase(A2AErrorCodes.TASK_NOT_FOUND.code(), TaskNotFoundError.class),
                new ErrorCase(Integer.MAX_VALUE, A2AError.class) // Any unknown code will be treated as A2AError
        );

        for (ErrorCase errorCase : cases) {
            String json = jsonTemplate.formatted(errorCase.code());
            A2AError error = JsonUtil.fromJson(json, A2AError.class);
            assertInstanceOf(errorCase.clazz(), error);
            assertEquals("error", error.getMessage());
            assertEquals("anything", error.getDetails().get("key"));
        }
    }


}
