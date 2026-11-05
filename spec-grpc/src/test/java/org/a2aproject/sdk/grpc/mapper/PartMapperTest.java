package org.a2aproject.sdk.grpc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.Value;
import org.a2aproject.sdk.grpc.Part;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

class PartMapperTest {

    @Test
    void constructedPartsMapMediaTypesToProto() {
        assertEquals("text/markdown", PartMapper.INSTANCE.toProto(new TextPart("Hello", null, "text/markdown")).getMediaType());
        assertEquals("application/json", PartMapper.INSTANCE.toProto(new DataPart(null, null, "application/json")).getMediaType());
    }

    @Test
    void textMediaTypeSurvivesProtoRoundTrip() {
        Part original = Part.newBuilder().setText("Hello").setMediaType("text/markdown").build();
        Part result = PartMapper.INSTANCE.toProto(PartMapper.INSTANCE.fromProto(original));
        assertEquals("text/markdown", result.getMediaType());
        assertEquals("Hello", result.getText());
    }

    @Test
    void dataMediaTypeSurvivesProtoRoundTrip() {
        Part original = Part.newBuilder().setData(Value.newBuilder().setStringValue("answer"))
                .setMediaType("application/vnd.example+json").build();
        Part result = PartMapper.INSTANCE.toProto(PartMapper.INSTANCE.fromProto(original));
        assertEquals("application/vnd.example+json", result.getMediaType());
        assertEquals("answer", result.getData().getStringValue());
    }
}
