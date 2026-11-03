package org.a2aproject.sdk.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NullDataTest {

    @ParameterizedTest
    @ValueSource(
            strings = {"null", "{\"optional\":null,\"items\":[null,{\"optional\":null},{},[]]}"})
    void dataAndMetadataNullsSurviveJsonRoundTrip(String data) throws Exception {
        String json = "{\"data\":" + data + ",\"metadata\":{\"optional\":null}}";
        DataPart part = assertInstanceOf(DataPart.class, JsonUtil.fromJson(json, Part.class));
        assertEquals(JsonParser.parseString(json), JsonParser.parseString(JsonUtil.toJson(part)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"data\":null}",
                "{\"text\":\"\",\"data\":null}",
                "{\"text\":null,\"data\":null}"
            })
    void explicitNullDataIsPresentContent(String json) throws Exception {
        DataPart part = assertInstanceOf(DataPart.class, JsonUtil.fromJson(json, Part.class));
        assertNull(part.data());
        assertEquals(
                JsonParser.parseString("{\"data\":null}"),
                JsonParser.parseString(JsonUtil.toJson(part)));
    }

    @Test
    void absentContentRemainsInvalid() {
        assertThrows(JsonProcessingException.class, () -> JsonUtil.fromJson("{}", Part.class));
    }

    @Test
    void absentMetadataRemainsOmitted() throws Exception {
        var task =
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build();
        var json = JsonParser.parseString(JsonUtil.toJson(task)).getAsJsonObject();
        assertFalse(json.has("metadata"));
    }

    @Test
    void recordsInsideMapsOmitAbsentFields() throws Exception {
        var task =
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build();
        var json =
                JsonParser.parseString(
                                JsonUtil.toJson(
                                        Map.of("task", task, "params", new ListTasksParams())))
                        .getAsJsonObject();
        assertFalse(json.getAsJsonObject("task").has("metadata"));
        assertFalse(json.getAsJsonObject("task").getAsJsonObject("status").has("message"));
        assertFalse(json.getAsJsonObject("params").has("tenant"));
    }

    @Test
    void nullMapEntriesDoNotChangeFollowingRecords() throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("optional", null);
        values.put(
                "items",
                Arrays.asList(
                        null, Collections.singletonMap("optional", null), new ListTasksParams()));
        var json = JsonParser.parseString(JsonUtil.toJson(values));
        assertEquals(
                JsonParser.parseString(
                        """
                        {"optional":null,"items":[null,{"optional":null},{}]}
                        """),
                json);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mapSerializationRestoresWriterPolicy(boolean serializeNulls) throws Exception {
        var output = new StringWriter();
        var writer = new JsonWriter(output);
        writer.setSerializeNulls(serializeNulls);
        writer.beginArray();
        JsonUtil.OBJECT_MAPPER
                .getAdapter(Map.class)
                .write(writer, Collections.singletonMap("optional", null));
        assertEquals(serializeNulls, writer.getSerializeNulls());
        writer.beginObject().name("absent").nullValue().endObject();
        writer.endArray();
        assertEquals(
                serializeNulls
                        ? "[{\"optional\":null},{\"absent\":null}]"
                        : "[{\"optional\":null},{}]",
                output.toString());
    }

    @Test
    void explicitNullDataSurvivesTreeSerialization() {
        assertEquals(
                JsonParser.parseString("{\"data\":null}"),
                JsonUtil.OBJECT_MAPPER.toJsonTree(new DataPart(null)));
    }

    @Test
    void typedEventMapsRetainTheirPolymorphicAdapter() {
        var task =
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build();
        var type = new TypeToken<Map<String, EventKind>>() {}.getType();
        var json =
                JsonParser.parseString(JsonUtil.OBJECT_MAPPER.toJson(Map.of("event", task), type));
        assertEquals(
                "task",
                json.getAsJsonObject()
                        .getAsJsonObject("event")
                        .getAsJsonObject("task")
                        .get("id")
                        .getAsString());
    }

    @Test
    void typedEventListMapsRetainTheirPolymorphicAdapter() {
        var task =
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build();
        var type = new TypeToken<Map<String, List<EventKind>>>() {}.getType();
        var json =
                JsonParser.parseString(
                        JsonUtil.OBJECT_MAPPER.toJson(Map.of("events", List.of(task)), type));
        assertEquals(
                "task",
                json.getAsJsonObject()
                        .getAsJsonArray("events")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("task")
                        .get("id")
                        .getAsString());
    }

    @Test
    void mapSubclassesRetainTheirDeclaredValueAdapter() {
        var task =
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build();
        var values = new EventMap();
        values.put("event", task);
        var json = JsonParser.parseString(JsonUtil.OBJECT_MAPPER.toJson(values));
        assertEquals(
                "task",
                json.getAsJsonObject()
                        .getAsJsonObject("event")
                        .getAsJsonObject("task")
                        .get("id")
                        .getAsString());
    }

    private static class EventMap extends HashMap<String, EventKind> {}

    @Test
    void failedNullMapWriteRestoresWriterPolicy() throws Exception {
        var writer =
                new JsonWriter(new StringWriter()) {
                    @Override
                    public JsonWriter nullValue() throws IOException {
                        throw new IOException("write failed");
                    }
                };
        writer.setSerializeNulls(false);
        assertThrows(
                IOException.class,
                () ->
                        JsonUtil.OBJECT_MAPPER
                                .getAdapter(Map.class)
                                .write(writer, Collections.singletonMap("optional", null)));
        assertFalse(writer.getSerializeNulls());
    }

    @Test
    void genericObjectMapSubclassesPreserveNullEntries() {
        var values = new GenericMap<Object>();
        values.put("optional", null);
        var type = new TypeToken<GenericMap<Object>>() {}.getType();
        assertEquals("{\"optional\":null}", JsonUtil.OBJECT_MAPPER.toJson(values, type));
    }

    @Test
    void genericEventMapSubclassesRetainTheirDeclaredValueAdapter() {
        var values = new GenericMap<EventKind>();
        values.put(
                "event",
                Task.builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .build());
        var type = new TypeToken<GenericMap<EventKind>>() {}.getType();
        var json = JsonParser.parseString(JsonUtil.OBJECT_MAPPER.toJson(values, type));
        assertEquals(
                "task",
                json.getAsJsonObject()
                        .getAsJsonObject("event")
                        .getAsJsonObject("task")
                        .get("id")
                        .getAsString());
    }

    @Test
    void failedNullDataWriteRestoresWriterPolicy() {
        var writer =
                new JsonWriter(new StringWriter()) {
                    @Override
                    public JsonWriter nullValue() throws IOException {
                        throw new IOException("write failed");
                    }
                };
        writer.setSerializeNulls(false);
        assertThrows(
                IOException.class,
                () ->
                        JsonUtil.OBJECT_MAPPER
                                .getAdapter(DataPart.class)
                                .write(writer, new DataPart(null)));
        assertFalse(writer.getSerializeNulls());
    }

    private static class GenericMap<V> extends HashMap<String, V> {}
}
