package org.a2aproject.sdk.compat03.spec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.a2aproject.sdk.compat03.json.JsonProcessingException;
import org.a2aproject.sdk.compat03.json.JsonUtil;


public class JSONRPCErrorSerializationTest {
    @Test
    public void shouldDeserializeToCorrectJSONRPCErrorSubclass() throws JsonProcessingException {
        String jsonTemplate = """
                {"code": %s, "message": "error", "data": "anything"}
                """;

        record ErrorCase(int code, Class<? extends JSONRPCError> clazz) {}

        List<ErrorCase> cases = List.of(
                new ErrorCase(JSONParseError.DEFAULT_CODE, JSONParseError.class),
                new ErrorCase(InvalidRequestError.DEFAULT_CODE, InvalidRequestError.class),
                new ErrorCase(MethodNotFoundError.DEFAULT_CODE, MethodNotFoundError.class),
                new ErrorCase(InvalidParamsError.DEFAULT_CODE, InvalidParamsError.class),
                new ErrorCase(InternalError.DEFAULT_CODE, InternalError.class),
                new ErrorCase(PushNotificationNotSupportedError.DEFAULT_CODE, PushNotificationNotSupportedError.class),
                new ErrorCase(UnsupportedOperationError.DEFAULT_CODE, UnsupportedOperationError.class),
                new ErrorCase(ContentTypeNotSupportedError.DEFAULT_CODE, ContentTypeNotSupportedError.class),
                new ErrorCase(InvalidAgentResponseError.DEFAULT_CODE, InvalidAgentResponseError.class),
                new ErrorCase(TaskNotCancelableError.DEFAULT_CODE, TaskNotCancelableError.class),
                new ErrorCase(TaskNotFoundError.DEFAULT_CODE, TaskNotFoundError.class),
                new ErrorCase(Integer.MAX_VALUE, JSONRPCError.class) // Any unknown code will be treated as JSONRPCError
        );

        for (ErrorCase errorCase : cases) {
            String json = jsonTemplate.formatted(errorCase.code());
            JSONRPCError error = JsonUtil.fromJson(json, JSONRPCError.class);
            assertInstanceOf(errorCase.clazz(), error);
            assertEquals("error", error.getMessage());
            assertEquals("anything", error.getData().toString());
        }
    }


}
