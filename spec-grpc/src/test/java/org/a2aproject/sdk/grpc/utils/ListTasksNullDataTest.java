package org.a2aproject.sdk.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.stream.Stream;
import org.a2aproject.sdk.grpc.Part;
import org.a2aproject.sdk.grpc.mapper.PartMapper;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResponse;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ListTasksNullDataTest {

    @Test
    void nullDataSurvivesHistoryArtifactsAndStatusMessageRoundTrip() throws Exception {
        String json =
                """
                {"jsonrpc":"2.0","id":"null-data","result":{"tasks":[{
                  "id":"task","contextId":"context",
                  "status":{"state":"TASK_STATE_COMPLETED","timestamp":"2026-10-28T00:00:00Z",
                    "message":{"messageId":"status","role":"ROLE_AGENT","parts":[{"data":null}]}},
                  "history":[{"messageId":"history","role":"ROLE_AGENT","parts":[{"data":null}]}],
                  "artifacts":[{"artifactId":"artifact","parts":[{"data":null}]}]
                }],"totalSize":1,"pageSize":1}}
                """;
        var response =
                assertInstanceOf(
                        ListTasksResponse.class, JSONRPCUtils.parseResponseBody(json, "ListTasks"));
        var task = response.getResult().tasks().get(0);
        assertNull(assertInstanceOf(DataPart.class, task.status().message().parts().get(0)).data());
        assertNull(assertInstanceOf(DataPart.class, task.history().get(0).parts().get(0)).data());
        assertNull(assertInstanceOf(DataPart.class, task.artifacts().get(0).parts().get(0)).data());

        var proto = ProtoUtils.ToProto.listTasksResult(response.getResult());
        var protoTask = proto.getTasks(0);
        assertTrue(protoTask.getHistory(0).getParts(0).hasData());
        assertEquals(
                Value.KindCase.NULL_VALUE,
                protoTask.getHistory(0).getParts(0).getData().getKindCase());
        assertEquals(
                Value.KindCase.NULL_VALUE,
                protoTask.getArtifacts(0).getParts(0).getData().getKindCase());
        assertEquals(
                Value.KindCase.NULL_VALUE,
                protoTask.getStatus().getMessage().getParts(0).getData().getKindCase());

        String serialized = JSONRPCUtils.toJsonRPCResultResponse("null-data", proto);
        var wirePart =
                JsonParser.parseString(serialized)
                        .getAsJsonObject()
                        .getAsJsonObject("result")
                        .getAsJsonArray("tasks")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("history")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("parts")
                        .get(0)
                        .getAsJsonObject();
        assertTrue(wirePart.has("data"));
        assertTrue(wirePart.get("data").isJsonNull());
        var roundTripped =
                assertInstanceOf(
                        ListTasksResponse.class,
                        JSONRPCUtils.parseResponseBody(serialized, "ListTasks"));
        assertEquals(proto, ProtoUtils.ToProto.listTasksResult(roundTripped.getResult()));
    }

    @Test
    void unsetProtobufDataValueRemainsInvalid() {
        var part = Part.newBuilder().setData(Value.getDefaultInstance()).build();
        assertThrows(InvalidRequestError.class, () -> PartMapper.INSTANCE.fromProto(part));
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
        var part = Part.newBuilder().setData(value).build();
        assertThrows(InvalidRequestError.class, () -> PartMapper.INSTANCE.fromProto(part));
    }

    @Test
    void validExplicitProtobufNullIsAccepted() {
        var part =
                Part.newBuilder()
                        .setData(Value.newBuilder().setNullValue(NullValue.NULL_VALUE))
                        .build();
        assertNull(assertInstanceOf(DataPart.class, PartMapper.INSTANCE.fromProto(part)).data());
    }
}
