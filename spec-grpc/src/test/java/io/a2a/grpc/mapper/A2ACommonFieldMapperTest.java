package io.a2a.grpc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.MessageSendParams;
import org.junit.jupiter.api.Test;

public class A2ACommonFieldMapperTest {

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
                "    \"blocking\": true\n" +
                "  },\n" +
                "  \"metadata\": {\n" +
                "  }\n" +
                "}";

        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
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

        io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
        JsonFormat.parser().merge(json, builder);

        // This should not throw NullPointerException
        MessageSendParams messageSendParams = ProtoUtils.FromProto.messageSendParams(builder);

        assertNotNull(messageSendParams);
        assertNotNull(messageSendParams.message());
    }
}
