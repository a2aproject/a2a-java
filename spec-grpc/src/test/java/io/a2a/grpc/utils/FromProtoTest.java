package io.a2a.grpc.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FromProtoTest {
    @Test
    public void testFromProtoPartUnsupportedType() {
        io.a2a.grpc.Part emptyPart = io.a2a.grpc.Part.newBuilder().build();
        io.a2a.grpc.Message invalidMessage = io.a2a.grpc.Message.newBuilder()
                                                                .addContent(emptyPart)
                                                                .build();
        io.a2a.spec.InvalidParamsError exception = assertThrows(
                io.a2a.spec.InvalidParamsError.class,
                () -> ProtoUtils.FromProto.message(invalidMessage)
        );

        assertEquals("Invalid parameters", exception.getMessage());

    }

    @Test
    public void testTaskQueryParamsInvalidName() {
        io.a2a.grpc.GetTaskRequest request = io.a2a.grpc.GetTaskRequest.newBuilder()
                                                                       .setName("invalid-name-format")
                                                                       .build();

        var result = ProtoUtils.FromProto.taskQueryParams(request);

        assertNotNull(result);
        assertEquals("invalid-name-format", result.id());
    }

}
