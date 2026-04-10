package org.a2aproject.sdk.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AErrorCodes;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
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
