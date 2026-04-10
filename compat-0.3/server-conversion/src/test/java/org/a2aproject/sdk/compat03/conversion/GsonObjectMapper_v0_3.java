package org.a2aproject.sdk.compat03.conversion;

import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;

/**
 * REST-Assured ObjectMapper adapter for v0.3 JSON serialization.
 * <p>
 * Used to deserialize v0.3 server JSONRPC responses that contain v0.3 types
 * (JSONRPCError, JSONRPCErrorResponse, etc.). Complements {@link V10GsonObjectMapper_v0_3}
 * which is used for test utility endpoints that expect v1.0 types.
 */
public class GsonObjectMapper_v0_3 implements ObjectMapper {
    public static final GsonObjectMapper_v0_3 INSTANCE = new GsonObjectMapper_v0_3();

    private GsonObjectMapper_v0_3() {
    }

    @Override
    public Object deserialize(ObjectMapperDeserializationContext context) {
        try {
            return JsonUtil_v0_3.fromJson(context.getDataToDeserialize().asString(), context.getType());
        } catch (JsonProcessingException_v0_3 ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object serialize(ObjectMapperSerializationContext context) {
        try {
            return JsonUtil_v0_3.toJson(context.getObjectToSerialize());
        } catch (JsonProcessingException_v0_3 ex) {
            throw new RuntimeException(ex);
        }
    }
}
