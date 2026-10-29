package org.a2aproject.sdk.compat03.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.junit.jupiter.api.Test;

class FromProtoExplicitNull_v0_3_Test {

    private static Struct struct(String json) throws Exception {
        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(json, builder);
        return builder.build();
    }

    @Test
    void partDataPartWithExplicitNullField() throws Exception {
        var dataPart = org.a2aproject.sdk.compat03.grpc.DataPart.newBuilder()
            .setData(struct("{\"some_optional_field\":null,\"other_field\":\"value\"}"))
            .build();
        var part = org.a2aproject.sdk.compat03.grpc.Part.newBuilder().setData(dataPart).build();

        Part_v0_3<?> result = ProtoUtils_v0_3.FromProto.part(part);

        assertTrue(result instanceof DataPart_v0_3);
        Map<String, Object> data = ((DataPart_v0_3) result).data();
        assertNotNull(data);
        assertEquals(2, data.size());
        assertTrue(data.containsKey("some_optional_field"));
        assertNull(data.get("some_optional_field"));
        assertEquals("value", data.get("other_field"));
    }

    @Test
    void partMetadataWithExplicitNullField() throws Exception {
        var textPart = org.a2aproject.sdk.compat03.grpc.Part.newBuilder()
            .setText("hello")
            .setMetadata(struct("{\"nullable\":null,\"source\":\"ID\"}"))
            .build();

        Part_v0_3<?> result = ProtoUtils_v0_3.FromProto.part(textPart);

        Map<String, Object> metadata = ((org.a2aproject.sdk.compat03.spec.TextPart_v0_3) result).metadata();
        assertNotNull(metadata);
        assertEquals(2, metadata.size());
        assertTrue(metadata.containsKey("nullable"));
        assertNull(metadata.get("nullable"));
        assertEquals("ID", metadata.get("source"));
    }

    @Test
    void partDataPartWithNestedNullAndNullInList() throws Exception {
        var dataPart = org.a2aproject.sdk.compat03.grpc.DataPart.newBuilder()
            .setData(struct("{\"nested\":{\"optional\":null},\"items\":[null,1]}"))
            .build();
        var part = org.a2aproject.sdk.compat03.grpc.Part.newBuilder().setData(dataPart).build();

        Part_v0_3<?> result = ProtoUtils_v0_3.FromProto.part(part);

        Map<String, Object> data = ((DataPart_v0_3) result).data();
        assertNotNull(data.get("nested"));
        assertNull(((Map<?, ?>) data.get("nested")).get("optional"));
    }

    @Test
    void taskWithDataPartThatHasExplicitNullField() throws Exception {
        var message = org.a2aproject.sdk.compat03.grpc.Message.newBuilder()
            .setRole(org.a2aproject.sdk.compat03.grpc.Role.ROLE_AGENT)
            .setMessageId("message-1")
            .addContent(org.a2aproject.sdk.compat03.grpc.Part.newBuilder()
                .setData(org.a2aproject.sdk.compat03.grpc.DataPart.newBuilder()
                    .setData(struct("{\"some_optional_field\":null,\"other_field\":\"value\"}"))))
            .build();
        var task = org.a2aproject.sdk.compat03.grpc.Task.newBuilder()
            .setId("task-1")
            .setContextId("context-1")
            .setStatus(org.a2aproject.sdk.compat03.grpc.TaskStatus.newBuilder()
                .setState(org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_COMPLETED))
            .addHistory(message)
            .build();

        var result = ProtoUtils_v0_3.FromProto.task(task);

        assertEquals("task-1", result.id());
        assertEquals(1, result.history().size());
        var dataPart = (DataPart_v0_3) result.history().get(0).parts().get(0);
        assertTrue(dataPart.data().containsKey("some_optional_field"));
        assertNull(dataPart.data().get("some_optional_field"));
        assertEquals("value", dataPart.data().get("other_field"));
    }
}
