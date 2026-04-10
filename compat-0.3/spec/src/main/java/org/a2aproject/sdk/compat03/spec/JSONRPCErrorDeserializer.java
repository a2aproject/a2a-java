package org.a2aproject.sdk.compat03.spec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JSONRPCErrorDeserializer extends StdDeserializer<JSONRPCError> {

    private static final Map<Integer, TriFunction<Integer, String, Object, JSONRPCError>> ERROR_MAP = new HashMap<>();

    static {
        ERROR_MAP.put(JSONParseError.DEFAULT_CODE, JSONParseError::new);
        ERROR_MAP.put(InvalidRequestError.DEFAULT_CODE, InvalidRequestError::new);
        ERROR_MAP.put(MethodNotFoundError.DEFAULT_CODE, MethodNotFoundError::new);
        ERROR_MAP.put(InvalidParamsError.DEFAULT_CODE, InvalidParamsError::new);
        ERROR_MAP.put(InternalError.DEFAULT_CODE, InternalError::new);
        ERROR_MAP.put(PushNotificationNotSupportedError.DEFAULT_CODE, PushNotificationNotSupportedError::new);
        ERROR_MAP.put(UnsupportedOperationError.DEFAULT_CODE, UnsupportedOperationError::new);
        ERROR_MAP.put(ContentTypeNotSupportedError.DEFAULT_CODE, ContentTypeNotSupportedError::new);
        ERROR_MAP.put(InvalidAgentResponseError.DEFAULT_CODE, InvalidAgentResponseError::new);
        ERROR_MAP.put(TaskNotCancelableError.DEFAULT_CODE, TaskNotCancelableError::new);
        ERROR_MAP.put(TaskNotFoundError.DEFAULT_CODE, TaskNotFoundError::new);
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
