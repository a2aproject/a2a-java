package org.a2aproject.sdk.compat03.spec;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @SuppressWarnings("unchecked")
    void deleteTaskPushNotificationConfigSuccessResponseSerializesResultAsNull() throws JsonProcessingException_v0_3 {
        DeleteTaskPushNotificationConfigResponse_v0_3 response =
                new DeleteTaskPushNotificationConfigResponse_v0_3("req-123");

        String json = JsonUtil_v0_3.toJson(response);
        Map<String, Object> map = JsonUtil_v0_3.fromJson(json, Map.class);

        assertEquals("2.0", map.get("jsonrpc"));
        assertEquals("req-123", map.get("id"));
        assertTrue(map.containsKey("result"), "result field must be present in success response");
        assertEquals(null, map.get("result"), "result must be null for delete response");
        assertFalse(map.containsKey("error"), "error field must not be present in success response");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteTaskPushNotificationConfigErrorResponseSerializesErrorWithoutResult() throws JsonProcessingException_v0_3 {
        DeleteTaskPushNotificationConfigResponse_v0_3 response =
                new DeleteTaskPushNotificationConfigResponse_v0_3("req-456", new TaskNotFoundError_v0_3());

        String json = JsonUtil_v0_3.toJson(response);
        Map<String, Object> map = JsonUtil_v0_3.fromJson(json, Map.class);

        assertEquals("2.0", map.get("jsonrpc"));
        assertEquals("req-456", map.get("id"));
        assertTrue(map.containsKey("error"), "error field must be present in error response");
        assertFalse(map.containsKey("result"), "result field must not be present in error response");
    }


}
