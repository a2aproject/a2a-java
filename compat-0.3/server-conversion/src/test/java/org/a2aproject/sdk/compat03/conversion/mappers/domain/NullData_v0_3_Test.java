package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NullData_v0_3_Test {

    @Test
    void objectNullsSurviveBothConversionDirections() {
        Map<String, Object> data =
                Collections.singletonMap(
                        "items",
                        Arrays.asList(null, Collections.singletonMap("optional", null), Map.of()));
        Map<String, Object> metadata = Collections.singletonMap("optional", null);
        DataPart original = new DataPart(data, metadata);
        DataPart_v0_3 legacy =
                assertInstanceOf(DataPart_v0_3.class, PartMapper_v0_3.INSTANCE.fromV10(original));
        assertEquals(data, legacy.data());
        assertEquals(metadata, legacy.metadata());
        assertEquals(original, PartMapper_v0_3.INSTANCE.toV10(legacy));
    }

    @ParameterizedTest
    @ValueSource(strings = {"text", "data"})
    void partMetadataNullsSurviveBothConversionDirections(String kind) {
        Map<String, Object> metadata = Collections.singletonMap("optional", null);
        Map<String, Object> data = Map.of("value", "hello");
        Part<?> current =
                kind.equals("text")
                        ? new TextPart("hello", metadata)
                        : new DataPart(data, metadata);
        Part_v0_3<?> legacy =
                kind.equals("text")
                        ? new TextPart_v0_3("hello", metadata)
                        : new DataPart_v0_3(data, metadata);

        Part_v0_3<?> downgraded = PartMapper_v0_3.INSTANCE.fromV10(current);
        Part<?> upgraded = PartMapper_v0_3.INSTANCE.toV10(legacy);
        assertEquals(legacy, downgraded);
        assertEquals(current, upgraded);
        assertEquals(
                metadata,
                kind.equals("text")
                        ? assertInstanceOf(TextPart_v0_3.class, downgraded).metadata()
                        : assertInstanceOf(DataPart_v0_3.class, downgraded).metadata());
        assertEquals(
                metadata,
                kind.equals("text")
                        ? assertInstanceOf(TextPart.class, upgraded).metadata()
                        : assertInstanceOf(DataPart.class, upgraded).metadata());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "42", "true", "\"text\""})
    void nonObjectDataCannotBeDowngraded(String json) {
        DataPart part = DataPart.fromJson(json);
        assertThrows(InvalidRequestError.class, () -> PartMapper_v0_3.INSTANCE.fromV10(part));
    }
}
