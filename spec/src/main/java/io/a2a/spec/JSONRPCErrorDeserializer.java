package io.a2a.spec;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

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

public class JSONRPCErrorDeserializer extends StdDeserializer<JSONRPCError> {

    private static final Map<Integer, TriFunction<Integer, String, Object, JSONRPCError>> ERROR_MAP = new HashMap<>();

    static {
        ERROR_MAP.put(JSON_PARSE_ERROR_CODE, JSONParseError::new);
        ERROR_MAP.put(INVALID_REQUEST_ERROR_CODE, InvalidRequestError::new);
        ERROR_MAP.put(METHOD_NOT_FOUND_ERROR_CODE, MethodNotFoundError::new);
        ERROR_MAP.put(INVALID_PARAMS_ERROR_CODE, InvalidParamsError::new);
        ERROR_MAP.put(INTERNAL_ERROR_CODE, InternalError::new);
        ERROR_MAP.put(PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE, PushNotificationNotSupportedError::new);
        ERROR_MAP.put(UNSUPPORTED_OPERATION_ERROR_CODE, UnsupportedOperationError::new);
        ERROR_MAP.put(CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE, ContentTypeNotSupportedError::new);
        ERROR_MAP.put(INVALID_AGENT_RESPONSE_ERROR_CODE, InvalidAgentResponseError::new);
        ERROR_MAP.put(TASK_NOT_CANCELABLE_ERROR_CODE, TaskNotCancelableError::new);
        ERROR_MAP.put(TASK_NOT_FOUND_ERROR_CODE, TaskNotFoundError::new);
    }

    public JSONRPCErrorDeserializer() {
        this(null);
    }

    public JSONRPCErrorDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JSONRPCError deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int code = node.get("code").asInt();
        String message = node.get("message").asText();
        JsonNode dataNode = node.get("data");
        Object data = dataNode != null ? jsonParser.getCodec().treeToValue(dataNode, Object.class) : null;
        TriFunction<Integer, String, Object, JSONRPCError> constructor = ERROR_MAP.get(code);
        if (constructor != null) {
            return constructor.apply(code, message, data);
        } else {
            return new JSONRPCError(code, message, data);
        }
    }

    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
