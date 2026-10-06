package org.a2aproject.sdk.compat03.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParser;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

class JsonNullValues_v0_3_Test {
    @Test
    void preservesNullObjectEntriesAndOmitsAbsentProtocolFields() throws Exception {
        String fixture = """
                {"kind":"message","role":"user","messageId":"message",
                 "parts":[{"kind":"data","data":{"nil":null,"nested":{"nil":null},"empty":{}},
                           "metadata":{"nil":null}}],"metadata":{"nil":null}}
                """;
        Message_v0_3 message = JsonUtil_v0_3.fromJson(fixture, Message_v0_3.class);
        var json = JsonParser.parseString(JsonUtil_v0_3.toJson(message)).getAsJsonObject();
        var part = json.getAsJsonArray("parts").get(0).getAsJsonObject();
        assertEquals(JsonParser.parseString("{\"nil\":null,\"nested\":{\"nil\":null},\"empty\":{}}"), part.get("data"));
        assertTrue(part.getAsJsonObject("metadata").get("nil").isJsonNull());
        assertTrue(json.getAsJsonObject("metadata").get("nil").isJsonNull());
        assertFalse(json.has("taskId"));
        assertFalse(json.has("contextId"));
    }

    @Test
    void preservesNullMetadataAcrossPartsTasksArtifactsAndEvents() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nil", null);
        var text = new TextPart_v0_3("hello", metadata);
        var file = new FilePart_v0_3(new FileWithBytes_v0_3("text/plain", "file", "aGVsbG8="), metadata);
        var status = new TaskStatus_v0_3(TaskState_v0_3.WORKING);
        var artifact = new Artifact_v0_3.Builder().artifactId("artifact").parts(text).metadata(metadata).build();
        var task = new Task_v0_3("task", "context", status, List.of(artifact), List.of(), metadata);
        var statusEvent = new TaskStatusUpdateEvent_v0_3("task", status, "context", false, metadata);
        var artifactEvent = new TaskArtifactUpdateEvent_v0_3("task", artifact, "context", false, false, metadata);
        for (Map<String, Object> copy : List.of(text.metadata(), file.metadata(), artifact.metadata(), task.metadata(),
                statusEvent.metadata(), artifactEvent.metadata())) {
            assertEquals(metadata, copy);
            assertThrows(UnsupportedOperationException.class, () -> copy.put("change", true));
        }
        metadata.put("later", "change");
        assertEquals(1, artifact.metadata().size());
        assertEquals(1, task.metadata().size());
    }

    @Test
    void doesNotEmitAbsentFieldsInObjectsStoredInMaps() throws Exception {
        var json = JsonParser.parseString(JsonUtil_v0_3.toJson(Map.of("value", new OptionalFields("present", null))))
                .getAsJsonObject();
        assertEquals("present", json.getAsJsonObject("value").get("required").getAsString());
        assertFalse(json.getAsJsonObject("value").has("optional"));
    }

    private record OptionalFields(String required, String optional) {
    }

    @Test
    void defensivelyCopiesMapsWithoutRejectingNullValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("nil", null);
        var data = new DataPart_v0_3(values, values);
        var message = new Message_v0_3.Builder().role(Message_v0_3.Role.USER).messageId("message")
                .parts(List.of(data)).metadata(values).build();
        values.put("later", "change");
        assertEquals(1, data.data().size());
        assertEquals(1, data.metadata().size());
        assertEquals(1, message.metadata().size());
        assertThrows(UnsupportedOperationException.class, () -> data.data().put("change", true));
    }
}
