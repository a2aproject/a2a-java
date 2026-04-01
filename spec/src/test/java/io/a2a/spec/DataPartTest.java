package io.a2a.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DataPartTest {

    @Test
    void testFromJson_object() {
        DataPart part = DataPart.fromJson("""
                {"temperature": 22.5, "humidity": 65}""");

        Map<String, Object> data = assertInstanceOf(Map.class, part.data());
        assertEquals(22.5, data.get("temperature"));
        assertEquals(65L, data.get("humidity"));
        assertNull(part.metadata());
    }

    @Test
    void testFromJson_array() {
        DataPart part = DataPart.fromJson("""
                ["a", "b", "c"]""");

        List<Object> data = assertInstanceOf(List.class, part.data());
        assertEquals(List.of("a", "b", "c"), data);
    }

    @Test
    void testFromJson_string() {
        DataPart part = DataPart.fromJson("\"hello\"");

        assertEquals("hello", part.data());
    }

    @Test
    void testFromJson_integerNumber() {
        DataPart part = DataPart.fromJson("42");

        assertEquals(42L, part.data());
    }

    @Test
    void testFromJson_decimalNumber() {
        DataPart part = DataPart.fromJson("3.14");

        assertEquals(3.14, part.data());
    }

    @Test
    void testFromJson_boolean() {
        DataPart part = DataPart.fromJson("true");

        assertEquals(true, part.data());
    }

    @Test
    void testFromJson_withMetadata() {
        Map<String, Object> metadata = Map.of("source", "sensor");
        DataPart part = DataPart.fromJson("""
                {"temperature": 22.5}""", metadata);

        assertInstanceOf(Map.class, part.data());
        assertEquals("sensor", part.metadata().get("source"));
    }

    @Test
    void testFromJson_nestedObject() {
        DataPart part = DataPart.fromJson("""
                {"outer": {"inner": [1, 2, 3]}}""");

        Map<String, Object> data = assertInstanceOf(Map.class, part.data());
        Map<String, Object> outer = assertInstanceOf(Map.class, data.get("outer"));
        assertEquals(List.of(1L, 2L, 3L), outer.get("inner"));
    }

    @Test
    void testFromJson_nullJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> DataPart.fromJson(null));
    }

    @Test
    void testFromJson_nullLiteralThrows() {
        assertThrows(IllegalArgumentException.class, () -> DataPart.fromJson("null"));
    }

    @Test
    void testFromJson_invalidJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> DataPart.fromJson("{invalid}"));
    }
}
