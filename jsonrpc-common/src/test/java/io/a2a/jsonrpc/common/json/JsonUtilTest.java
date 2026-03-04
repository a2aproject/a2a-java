package io.a2a.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

public class JsonUtilTest {

    // readMetadata(String) tests

    @Test
    public void testReadMetadataStringExtractsMetadataField() throws Exception {
        String body = "{\"id\":\"task-1\",\"metadata\":{\"reason\":\"user_requested\",\"source\":\"web_ui\"}}";
        Map<String, Object> metadata = JsonUtil.readMetadata(body);
        assertEquals(2, metadata.size());
        assertEquals("user_requested", metadata.get("reason"));
        assertEquals("web_ui", metadata.get("source"));
    }

    @Test
    public void testReadMetadataStringReturnsEmptyMapWhenNoMetadataField() throws Exception {
        String body = "{\"id\":\"task-1\"}";
        Map<String, Object> metadata = JsonUtil.readMetadata(body);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testReadMetadataStringReturnsEmptyMapForEmptyMetadataObject() throws Exception {
        String body = "{\"metadata\":{}}";
        Map<String, Object> metadata = JsonUtil.readMetadata(body);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testReadMetadataStringReturnsEmptyMapForNullInput() throws Exception {
        assertTrue(JsonUtil.readMetadata((String) null).isEmpty());
    }

    @Test
    public void testReadMetadataStringReturnsEmptyMapForBlankInput() throws Exception {
        assertTrue(JsonUtil.readMetadata("   ").isEmpty());
    }

    // readMetadata(JsonObject) tests — verify String overload is consistent with it

    @Test
    public void testReadMetadataStringConsistentWithJsonObjectOverload() throws Exception {
        String body = "{\"metadata\":{\"key\":\"value\"}}";
        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();

        Map<String, Object> fromString = JsonUtil.readMetadata(body);
        Map<String, Object> fromJsonObject = JsonUtil.readMetadata(jsonObject);

        assertEquals(fromJsonObject, fromString);
    }
}
