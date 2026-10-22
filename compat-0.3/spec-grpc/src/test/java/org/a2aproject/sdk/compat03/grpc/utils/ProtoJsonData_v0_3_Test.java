package org.a2aproject.sdk.compat03.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.a2aproject.sdk.compat03.grpc.DataPart;
import org.a2aproject.sdk.compat03.grpc.FilePart;
import org.a2aproject.sdk.compat03.grpc.Part;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

class ProtoJsonData_v0_3_Test {
    @Test
    void convertsBinaryFilesUsingRawProtobufBytes() {
        byte[] bytes = {0, (byte) 0xff, (byte) 0x80, 42};
        String encoded = Base64.getEncoder().encodeToString(bytes);
        var outgoing = ProtoUtils_v0_3.ToProto.part(new FilePart_v0_3(
                new FileWithBytes_v0_3("application/octet-stream", "binary", encoded)));
        assertArrayEquals(bytes, outgoing.getFile().getFileWithBytes().toByteArray());

        Part incoming = Part.newBuilder().setFile(FilePart.newBuilder()
                .setMimeType("application/octet-stream").setFileWithBytes(ByteString.copyFrom(bytes))).build();
        var file = assertInstanceOf(FilePart_v0_3.class, ProtoUtils_v0_3.FromProto.part(incoming));
        assertEquals(encoded, assertInstanceOf(FileWithBytes_v0_3.class, file.file()).bytes());
    }

    @Test
    void readsEmptyDataObjectWithoutTreatingItAsAbsent() {
        Part incoming = Part.newBuilder().setData(DataPart.newBuilder().setData(Struct.getDefaultInstance())).build();
        var data = assertInstanceOf(DataPart_v0_3.class, ProtoUtils_v0_3.FromProto.part(incoming));
        assertEquals(Map.of(), data.data());
    }

    @Test
    void preservesNestedEmptyObjectsAndJsonNullsInBothDirections() {
        Value empty = Value.newBuilder().setStructValue(Struct.getDefaultInstance()).build();
        Value nil = Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        Struct fixture = Struct.newBuilder().putFields("empty", empty).putFields("nil", nil)
                .putFields("items", Value.newBuilder().setListValue(ListValue.newBuilder()
                        .addValues(empty).addValues(nil)).build()).build();
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("empty", Map.of());
        expected.put("nil", null);
        expected.put("items", Arrays.asList(Map.of(), null));

        var incoming = Part.newBuilder().setData(DataPart.newBuilder().setData(fixture)).setMetadata(fixture).build();
        var data = assertInstanceOf(DataPart_v0_3.class, ProtoUtils_v0_3.FromProto.part(incoming));
        assertEquals(expected, data.data());
        assertEquals(expected, data.metadata());
        var outgoing = ProtoUtils_v0_3.ToProto.part(new DataPart_v0_3(expected, expected));
        assertEquals(fixture, outgoing.getData().getData());
        assertEquals(fixture, outgoing.getMetadata());
    }

    @Test
    void retainsAbsentMessageMetadataAndPreservesExtensions() {
        var message = new Message_v0_3.Builder().role(Message_v0_3.Role.USER).messageId("message")
                .parts(List.of(new TextPart_v0_3("hello"))).extensions(List.of("https://example.test/ext")).build();
        var proto = ProtoUtils_v0_3.ToProto.message(message);
        assertEquals(List.of("https://example.test/ext"), proto.getExtensionsList());
        assertNull(ProtoUtils_v0_3.FromProto.message(proto).metadata());
    }
}
