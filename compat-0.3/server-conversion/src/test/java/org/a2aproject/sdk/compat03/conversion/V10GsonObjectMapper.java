package org.a2aproject.sdk.compat03.conversion;

import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;

/**
 * REST-Assured ObjectMapper adapter for v1.0 JSON serialization.
 * <p>
 * Used by test utilities to communicate with server test endpoints that expect v1.0 JSON format.
 * The v0.3 compatibility tests use v0.3 client types, but the server test infrastructure
 * (TestUtilsBean endpoints) operates on v1.0 types.
 */
public class V10GsonObjectMapper implements ObjectMapper {
    public static final V10GsonObjectMapper INSTANCE = new V10GsonObjectMapper();

    private V10GsonObjectMapper() {
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
