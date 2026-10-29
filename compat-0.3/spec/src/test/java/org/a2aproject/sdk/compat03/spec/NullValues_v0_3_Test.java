package org.a2aproject.sdk.compat03.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NullValues_v0_3_Test {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"kind\":\"data\",\"data\":{\"optional\":null},\"metadata\":{\"optional\":null}}",
                "{\"kind\":\"text\",\"text\":\"hello\",\"metadata\":{\"optional\":null}}",
                "{\"kind\":\"file\",\"file\":{\"uri\":\"https://example.com/file\"},\"metadata\":{\"optional\":null}}"
            })
    void partNullValuesSurviveJsonRoundTrip(String json) throws Exception {
        Part_v0_3<?> part = JsonUtil_v0_3.fromJson(json, Part_v0_3.class);
        assertEquals(
                JsonParser.parseString(json), JsonParser.parseString(JsonUtil_v0_3.toJson(part)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"kind\":\"task\",\"id\":\"task\",\"contextId\":\"context\",\"status\":{\"state\":\"completed\"},\"metadata\":{\"optional\":null}}",
                "{\"kind\":\"message\",\"messageId\":\"message\",\"role\":\"agent\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}],\"metadata\":{\"optional\":null}}",
                "{\"kind\":\"status-update\",\"taskId\":\"task\",\"contextId\":\"context\",\"status\":{\"state\":\"completed\"},\"final\":true,\"metadata\":{\"optional\":null}}",
                "{\"kind\":\"artifact-update\",\"taskId\":\"task\",\"contextId\":\"context\",\"artifact\":{\"artifactId\":\"artifact\",\"parts\":[{\"kind\":\"text\",\"text\":\"hello\"}]},\"metadata\":{\"optional\":null}}"
            })
    void eventMetadataNullsSurviveJsonRoundTrip(String json) throws Exception {
        StreamingEventKind_v0_3 event = JsonUtil_v0_3.fromJson(json, StreamingEventKind_v0_3.class);
        var serialized = JsonParser.parseString(JsonUtil_v0_3.toJson(event)).getAsJsonObject();
        assertEquals(JsonParser.parseString("{\"optional\":null}"), serialized.get("metadata"));
    }

    @Test
    void artifactBuilderAcceptsMetadataWithNullValues() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("optional", null);
        Artifact_v0_3 artifact =
                new Artifact_v0_3.Builder()
                        .artifactId("artifact")
                        .parts(new TextPart_v0_3("hello"))
                        .metadata(metadata)
                        .build();
        metadata.put("later", true);
        assertTrue(artifact.metadata().containsKey("optional"));
        assertNull(artifact.metadata().get("optional"));
        assertEquals(1, artifact.metadata().size());
        assertThrows(
                UnsupportedOperationException.class, () -> artifact.metadata().put("later", true));
        var serialized = JsonParser.parseString(JsonUtil_v0_3.toJson(artifact)).getAsJsonObject();
        assertEquals(JsonParser.parseString("{\"optional\":null}"), serialized.get("metadata"));
    }

    @Test
    void dataObjectWithNullValuesIsDefensivelyCopied() {
        Map<String, Object> data = new HashMap<>();
        data.put("optional", null);
        DataPart_v0_3 part = new DataPart_v0_3(data);
        data.put("later", true);
        assertTrue(part.data().containsKey("optional"));
        assertNull(part.data().get("optional"));
        assertEquals(1, part.data().size());
        assertThrows(UnsupportedOperationException.class, () -> part.data().put("later", true));
    }

    @Test
    void nullDataObjectRemainsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new DataPart_v0_3(null));
    }

    @Test
    void absentMetadataRemainsOmitted() throws Exception {
        var task =
                new Task_v0_3.Builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                        .build();
        var json = JsonParser.parseString(JsonUtil_v0_3.toJson(task)).getAsJsonObject();
        assertFalse(json.has("metadata"));
    }

    @Test
    void recordsInsideMapsOmitAbsentFields() throws Exception {
        var task =
                new Task_v0_3.Builder()
                        .id("task")
                        .contextId("context")
                        .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                        .build();
        var json =
                JsonParser.parseString(
                                JsonUtil_v0_3.toJson(
                                        Map.of(
                                                "task",
                                                task,
                                                "params",
                                                new TaskQueryParams_v0_3("task"))))
                        .getAsJsonObject();
        assertFalse(json.getAsJsonObject("task").has("metadata"));
        assertFalse(json.getAsJsonObject("task").getAsJsonObject("status").has("message"));
        assertFalse(json.getAsJsonObject("params").has("metadata"));
    }

    @Test
    void nullMapEntriesDoNotChangeFollowingRecords() throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("optional", null);
        values.put(
                "items",
                Arrays.asList(
                        null,
                        Collections.singletonMap("optional", null),
                        new TaskQueryParams_v0_3("task")));
        var json = JsonParser.parseString(JsonUtil_v0_3.toJson(values));
        assertEquals(
                JsonParser.parseString(
                        """
                        {"optional":null,"items":[null,{"optional":null},{"id":"task","historyLength":0}]}
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
        JsonUtil_v0_3.OBJECT_MAPPER
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
    void typedMapsRetainTheirDeclaredValueAdapter() {
        var gson =
                JsonUtil_v0_3.OBJECT_MAPPER
                        .newBuilder()
                        .registerTypeAdapter(
                                Labelled.class,
                                (JsonSerializer<Labelled>)
                                        (value, type, context) -> new JsonPrimitive(value.label()))
                        .create();
        var type = new TypeToken<Map<String, Labelled>>() {}.getType();
        assertEquals(
                "{\"value\":\"label\"}", gson.toJson(Map.of("value", new Label("label")), type));
    }

    private interface Labelled {
        String label();
    }

    private record Label(String label) implements Labelled {}

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
                        JsonUtil_v0_3.OBJECT_MAPPER
                                .getAdapter(Map.class)
                                .write(writer, Collections.singletonMap("optional", null)));
        assertFalse(writer.getSerializeNulls());
    }
}
