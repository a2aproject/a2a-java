package org.a2aproject.sdk.compat03.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.a2aproject.sdk.compat03.grpc.DataPart;
import org.a2aproject.sdk.compat03.grpc.Message;
import org.a2aproject.sdk.compat03.grpc.Part;
import org.a2aproject.sdk.compat03.grpc.Role;
import org.a2aproject.sdk.compat03.grpc.Task;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class NullValues_v0_3_Test {

    @Test
    void outgoingStructPreservesExplicitNulls() throws Exception {
        Map<String, Object> data =
                Collections.singletonMap(
                        "items",
                        Arrays.asList(
                                null,
                                Collections.singletonMap("optional", null),
                                Map.of(),
                                Collections.emptyList()));
        Struct proto = ProtoUtils_v0_3.ToProto.struct(data);
        Struct.Builder expected = Struct.newBuilder();
        JsonFormat.parser().merge("{\"items\":[null,{\"optional\":null},{},[]]}", expected);
        assertEquals(expected.build(), proto);
        JsonFormat.printer().print(proto);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"optional\":null,\"nested\":{},\"items\":[null,{\"optional\":null},{},[]]}"
            })
    void dataObjectAndMetadataSurviveProtoRoundTrip(String data) throws Exception {
        String json =
                """
                {"id":"task","contextId":"context","status":{"state":"TASK_STATE_COMPLETED"},
                 "metadata":{"optional":null},
                 "history":[{"messageId":"message","role":"ROLE_AGENT","metadata":{"optional":null},
                   "content":[{"data":{"data":%s},"metadata":{"optional":null}}]}]}
                """
                        .formatted(data);
        var builder = Task.newBuilder();
        JsonFormat.parser().merge(json, builder);
        var task = ProtoUtils_v0_3.FromProto.task(builder);
        var part = assertInstanceOf(DataPart_v0_3.class, task.history().get(0).parts().get(0));
        assertEquals(Collections.singletonMap("optional", null), task.metadata());
        assertEquals(Collections.singletonMap("optional", null), task.history().get(0).metadata());
        assertEquals(Collections.singletonMap("optional", null), part.metadata());
        Struct.Builder expected = Struct.newBuilder();
        JsonFormat.parser().merge(data, expected);
        assertEquals(expected.build(), ProtoUtils_v0_3.ToProto.struct(part.data()));
        var roundTripped = ProtoUtils_v0_3.ToProto.task(task);
        assertEquals(
                builder.getHistory(0).getContent(0).getData().getData(),
                roundTripped.getHistory(0).getContent(0).getData().getData());
        assertEquals(builder.getMetadata(), roundTripped.getMetadata());
    }

    @Test
    void absentProtobufDataObjectRemainsInvalid() {
        var message =
                Message.newBuilder()
                        .setMessageId("message")
                        .setRole(Role.ROLE_AGENT)
                        .addContent(Part.newBuilder().setData(DataPart.getDefaultInstance()))
                        .build();
        assertThrows(
                InvalidRequestError_v0_3.class, () -> ProtoUtils_v0_3.FromProto.message(message));
    }

    static Stream<Value> invalidDataValues() {
        Value invalidNull = Value.newBuilder().setNullValueValue(99).build();
        Value unset = Value.getDefaultInstance();
        return Stream.of(invalidNull, unset)
                .flatMap(
                        value ->
                                Stream.of(
                                        value,
                                        Value.newBuilder()
                                                .setStructValue(
                                                        Struct.newBuilder()
                                                                .putFields("optional", value))
                                                .build(),
                                        Value.newBuilder()
                                                .setListValue(
                                                        ListValue.newBuilder().addValues(value))
                                                .build()));
    }

    @ParameterizedTest
    @MethodSource("invalidDataValues")
    void malformedProtobufDataValuesAreRejected(Value value) {
        var message =
                Message.newBuilder()
                        .setMessageId("message")
                        .setRole(Role.ROLE_AGENT)
                        .addContent(
                                Part.newBuilder()
                                        .setData(
                                                DataPart.newBuilder()
                                                        .setData(
                                                                Struct.newBuilder()
                                                                        .putFields(
                                                                                "optional",
                                                                                value))))
                        .build();
        assertThrows(
                InvalidRequestError_v0_3.class, () -> ProtoUtils_v0_3.FromProto.message(message));
    }
}
