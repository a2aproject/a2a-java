package org.a2aproject.sdk.grpc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.junit.jupiter.api.Test;

public class A2ACommonFieldMapperTest {

    @Test
    void testStructToMap_WithExplicitNull_PreservesKey() throws InvalidProtocolBufferException {
        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge("{\"attributeId\":null,\"source\":\"ID\"}", builder);

        Map<String, Object> result = A2ACommonFieldMapper.INSTANCE.structToMap(builder.build());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("attributeId"));
        assertNull(result.get("attributeId"));
        assertEquals("ID", result.get("source"));
    }

    @Test
    void testObjectToValue_WithExplicitNull_SetsNullKind() {
        Value result = A2ACommonFieldMapper.INSTANCE.objectToValue(null);

        assertEquals(Value.KindCase.NULL_VALUE, result.getKindCase());
    }

    @Test
    void testStructToMap_WithNestedNulls_RoundTrips() throws InvalidProtocolBufferException {
        String json = """
                {
                  "nested": {"optional": null},
                  "items": [null, {"optional": null}, [], {}],
                  "emptyObject": {},
                  "emptyList": [],
                  "text": "",
                  "number": 0,
                  "flag": false
                }
                """;
        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(json, builder);
        Struct original = builder.build();

        Map<String, Object> result = A2ACommonFieldMapper.INSTANCE.structToMap(original);

        Map<?, ?> nested = assertInstanceOf(Map.class, result.get("nested"));
        assertTrue(nested.containsKey("optional"));
        assertNull(nested.get("optional"));
        List<?> items = assertInstanceOf(List.class, result.get("items"));
        assertEquals(4, items.size());
        assertNull(items.get(0));
        Map<?, ?> item = assertInstanceOf(Map.class, items.get(1));
        assertTrue(item.containsKey("optional"));
        assertNull(item.get("optional"));
        assertEquals(List.of(), items.get(2));
        assertEquals(Map.of(), items.get(3));
        assertEquals(Map.of(), result.get("emptyObject"));
        assertEquals(List.of(), result.get("emptyList"));
        assertEquals("", result.get("text"));
        assertEquals(0.0, result.get("number"));
        assertEquals(false, result.get("flag"));
        assertFalse(result.containsKey("absent"));
        assertEquals(original, A2ACommonFieldMapper.INSTANCE.mapToStruct(result));
        assertEquals(result, A2ACommonFieldMapper.INSTANCE.metadataFromProto(original));
        assertEquals(original, A2ACommonFieldMapper.INSTANCE.metadataToProto(result));
    }

    @Test
    void testValueToObject_WithNullListElement_RoundTrips() throws InvalidProtocolBufferException {
        Value.Builder builder = Value.newBuilder();
        JsonFormat.parser().merge("[null,\"value\",{},[]]", builder);
        Value original = builder.build();

        List<?> result = assertInstanceOf(List.class, A2ACommonFieldMapper.INSTANCE.valueToObject(original));

        assertEquals(Arrays.asList(null, "value", Map.of(), List.of()), result);
        assertEquals(original, A2ACommonFieldMapper.INSTANCE.objectToValue(result));
    }

    @Test
    void testStructConversions_WithAbsentOrEmptyValues_PreserveDefaults() {
        A2ACommonFieldMapper mapper = A2ACommonFieldMapper.INSTANCE;
        Struct empty = Struct.getDefaultInstance();

        assertNull(mapper.structToMap(null));
        assertNull(mapper.structToMap(empty));
        assertEquals(Map.of(), mapper.metadataFromProto(null));
        assertEquals(Map.of(), mapper.metadataFromProto(empty));
        assertEquals(empty, mapper.mapToStruct(null));
        assertEquals(empty, mapper.mapToStruct(Map.of()));
        assertEquals(empty, mapper.metadataToProto(null));
        assertEquals(empty, mapper.metadataToProto(Map.of()));
    }

    /**
     * Test that valueToObject handles empty struct correctly without throwing NullPointerException.
     *
     * This test verifies the fix for the bug where an empty struct in the JSON
     * (e.g., "response": {}) would cause a NullPointerException because structToMap
     * returns null for empty structs.
     */
    @Test
    void testValueToObject_WithEmptyStruct_ReturnsEmptyMap() throws InvalidProtocolBufferException {
        // JSON containing an empty struct in "response" field
        String json = "{\n" +
                "  \"message\": {\n" +
                "    \"messageId\": \"b3b1ab58-c3d0-4e6d-9e47-9d8a12fe0809\",\n" +
                "    \"role\": \"ROLE_USER\",\n" +
                "    \"parts\": [{\n" +
                "      \"text\": \"Hello\"\n" +
                "    }, {\n" +
                "      \"data\": {\n" +
                "        \"data\": {\n" +
                "          \"id\": \"call_94yo5ymj3qi5glbpkw5eicfd\",\n" +
                "          \"args\": {\n" +
                "            \"agent_name\": \"Default Agent\"\n" +
                "          },\n" +
                "          \"name\": \"transfer_to_agent\"\n" +
                "        }\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"data\": {\n" +
                "        \"data\": {\n" +
                "          \"response\": {\n" +
                "          },\n" +
                "          \"id\": \"call_94yo5ymj3qi5glbpkw5eicfd\",\n" +
                "          \"name\": \"transfer_to_agent\"\n" +
                "        }\n" +
                "      }\n" +
                "    }, {\n" +
                "      \"text\": \"World\"\n" +
                "    }],\n" +
                "    \"metadata\": {\n" +
                "    }\n" +
                "  },\n" +
                "  \"configuration\": {\n" +
                "    \"returnImmediately\": false\n" +
                "  },\n" +
                "  \"metadata\": {\n" +
                "  }\n" +
                "}";

        org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
        JsonFormat.parser().merge(json, builder);

        // This should not throw NullPointerException
        MessageSendParams messageSendParams = ProtoUtils.FromProto.messageSendParams(builder);

        assertNotNull(messageSendParams);
        assertNotNull(messageSendParams.message());
        assertEquals(4, messageSendParams.message().parts().size());
    }

    /**
     * Test that valueToObject handles nested empty struct correctly.
     */
    @Test
    void testValueToObject_WithNestedEmptyStruct_ReturnsEmptyMap() throws InvalidProtocolBufferException {
        String json = "{\n" +
                "  \"message\": {\n" +
                "    \"messageId\": \"test-id\",\n" +
                "    \"role\": \"ROLE_USER\",\n" +
                "    \"parts\": [{\n" +
                "      \"data\": {\n" +
                "        \"data\": {\n" +
                "          \"nested\": {\n" +
                "            \"empty\": {\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }]\n" +
                "  }\n" +
                "}";

        org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
        JsonFormat.parser().merge(json, builder);

        // This should not throw NullPointerException
        MessageSendParams messageSendParams = ProtoUtils.FromProto.messageSendParams(builder);

        assertNotNull(messageSendParams);
        assertNotNull(messageSendParams.message());
    }
}
