package org.a2aproject.sdk.compat03.conversion;

import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.a2aproject.sdk.compat03.json.JsonProcessingException;
import org.a2aproject.sdk.compat03.json.JsonUtil;

/**
 * REST-Assured ObjectMapper adapter for v0.3 JSON serialization.
 * <p>
 * Used to deserialize v0.3 server JSONRPC responses that contain v0.3 types
 * (JSONRPCError, JSONRPCErrorResponse, etc.). Complements {@link V10GsonObjectMapper}
 * which is used for test utility endpoints that expect v1.0 types.
 */
public class V03GsonObjectMapper implements ObjectMapper {
    public static final V03GsonObjectMapper INSTANCE = new V03GsonObjectMapper();

    private V03GsonObjectMapper() {
    }

    @Override
    public Object deserialize(ObjectMapperDeserializationContext context) {
        try {
            return JsonUtil.fromJson(context.getDataToDeserialize().asString(), context.getType());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object serialize(ObjectMapperSerializationContext context) {
        try {
            return JsonUtil.toJson(context.getObjectToSerialize());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
