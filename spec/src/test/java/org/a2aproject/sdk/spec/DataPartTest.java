package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
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
    void testConstructor_withNullMetadataValuePreservesValueAndIsImmutable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", null);

        DataPart part = new DataPart(Map.of("temperature", 22.5), metadata);

        assertTrue(part.metadata().containsKey("source"));
        assertNull(part.metadata().get("source"));
        assertThrows(UnsupportedOperationException.class, () -> part.metadata().put("another", "value"));
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

    @SuppressWarnings("unchecked")
    @Test
    void testDataMapIsDefensivelyCopiedAndImmutable() {
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 22.5);

        DataPart part = new DataPart(data);

        assertThrows(UnsupportedOperationException.class, () -> ((Map<String, Object>) part.data()).put("humidity", 65));
        data.put("humidity", 65);
        assertEquals(Map.of("temperature", 22.5), part.data());
    }

    @Test
    void testDataListIsDefensivelyCopiedAndImmutable() {
        List<Object> data = new ArrayList<>();
        data.add("a");

        DataPart part = new DataPart(data);

        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) part.data()).add("b"));
        data.add("b");
        assertEquals(List.of("a"), part.data());
    }

    @Test
    void testDataMapPreservesNullValues() {
        Map<String, Object> data = new HashMap<>();
        data.put("source", null);

        DataPart part = new DataPart(data);

        assertTrue(part.data() instanceof Map);
        assertTrue(((Map<?, ?>) part.data()).containsKey("source"));
        assertNull(((Map<?, ?>) part.data()).get("source"));
    }

    @Test
    void testDataListPreservesNullElements() {
        List<Object> data = new ArrayList<>();
        data.add(null);

        DataPart part = new DataPart(data);

        assertTrue(part.data() instanceof List);
        assertEquals(1, ((List<?>) part.data()).size());
        assertNull(((List<?>) part.data()).get(0));
    }

    @Test
    void testDataPrimitiveStoredAsIs() {
        Integer data = 42;

        DataPart part = new DataPart(data);

        assertSame(data, part.data());
    }
}
