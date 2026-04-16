package org.a2aproject.sdk.compat03.spec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;


public class JSONRPCErrorSerialization_v0_3_Test {
    @Test
    public void shouldDeserializeToCorrectJSONRPCErrorSubclass() throws JsonProcessingException_v0_3 {
        String jsonTemplate = """
                {"code": %s, "message": "error", "data": "anything"}
                """;

        record ErrorCase(int code, Class<? extends JSONRPCError_v0_3> clazz) {}

        List<ErrorCase> cases = List.of(
                new ErrorCase(JSONParseError_v0_3.DEFAULT_CODE, JSONParseError_v0_3.class),
                new ErrorCase(InvalidRequestError_v0_3.DEFAULT_CODE, InvalidRequestError_v0_3.class),
                new ErrorCase(MethodNotFoundError_v0_3.DEFAULT_CODE, MethodNotFoundError_v0_3.class),
                new ErrorCase(InvalidParamsError_v0_3.DEFAULT_CODE, InvalidParamsError_v0_3.class),
                new ErrorCase(InternalError_v0_3.DEFAULT_CODE, InternalError_v0_3.class),
                new ErrorCase(PushNotificationNotSupportedError_v0_3.DEFAULT_CODE, PushNotificationNotSupportedError_v0_3.class),
                new ErrorCase(UnsupportedOperationError_v0_3.DEFAULT_CODE, UnsupportedOperationError_v0_3.class),
                new ErrorCase(ContentTypeNotSupportedError_v0_3.DEFAULT_CODE, ContentTypeNotSupportedError_v0_3.class),
                new ErrorCase(InvalidAgentResponseError_v0_3.DEFAULT_CODE, InvalidAgentResponseError_v0_3.class),
                new ErrorCase(TaskNotCancelableError_v0_3.DEFAULT_CODE, TaskNotCancelableError_v0_3.class),
                new ErrorCase(TaskNotFoundError_v0_3.DEFAULT_CODE, TaskNotFoundError_v0_3.class),
                new ErrorCase(Integer.MAX_VALUE, JSONRPCError_v0_3.class) // Any unknown code will be treated as JSONRPCError
        );

        for (ErrorCase errorCase : cases) {
            String json = jsonTemplate.formatted(errorCase.code());
            JSONRPCError_v0_3 error = JsonUtil_v0_3.fromJson(json, JSONRPCError_v0_3.class);
            assertInstanceOf(errorCase.clazz(), error);
            assertEquals("error", error.getMessage());
            assertEquals("anything", error.getData().toString());
        }
    }


}
