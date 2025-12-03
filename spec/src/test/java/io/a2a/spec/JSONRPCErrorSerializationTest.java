package io.a2a.spec;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static io.a2a.spec.A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INTERNAL_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_PARAMS_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_REQUEST_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_CANCELABLE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE;

public class JSONRPCErrorSerializationTest {
    @Test
    public void shouldDeserializeToCorrectJSONRPCErrorSubclass() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonTemplate = """
                {"code": %s, "message": "error", "data": "anything"}
                """;

        record ErrorCase(int code, Class<? extends JSONRPCError> clazz) {}

        List<ErrorCase> cases = List.of(new ErrorCase(JSON_PARSE_ERROR_CODE, JSONParseError.class),
                new ErrorCase(INVALID_REQUEST_ERROR_CODE, InvalidRequestError.class),
                new ErrorCase(METHOD_NOT_FOUND_ERROR_CODE, MethodNotFoundError.class),
                new ErrorCase(INVALID_PARAMS_ERROR_CODE, InvalidParamsError.class),
                new ErrorCase(INTERNAL_ERROR_CODE, InternalError.class),
                new ErrorCase(PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE, PushNotificationNotSupportedError.class),
                new ErrorCase(UNSUPPORTED_OPERATION_ERROR_CODE, UnsupportedOperationError.class),
                new ErrorCase(CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE, ContentTypeNotSupportedError.class),
                new ErrorCase(INVALID_AGENT_RESPONSE_ERROR_CODE, InvalidAgentResponseError.class),
                new ErrorCase(TASK_NOT_CANCELABLE_ERROR_CODE, TaskNotCancelableError.class),
                new ErrorCase(TASK_NOT_FOUND_ERROR_CODE, TaskNotFoundError.class),
                new ErrorCase(Integer.MAX_VALUE, JSONRPCError.class) // Any unknown code will be treated as JSONRPCError
        );

        for (ErrorCase errorCase : cases) {
            String json = jsonTemplate.formatted(errorCase.code());
            JSONRPCError error;
            try {
                error = objectMapper.readValue(json, JSONRPCError.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            assertInstanceOf(errorCase.clazz(), error);
            assertEquals("error", error.getMessage());
            assertEquals("anything", error.getData().toString());
        }
    }


}
