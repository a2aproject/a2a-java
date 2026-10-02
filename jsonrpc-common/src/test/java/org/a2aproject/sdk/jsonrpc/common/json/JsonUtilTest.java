package org.a2aproject.sdk.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // writeJsonRpcId tests

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testWriteJsonRpcIdWritesNullIdAndRestoresSerializeNulls(boolean serializeNulls) throws Exception {
        StringWriter result = new StringWriter();
        JsonWriter out = new JsonWriter(result);
        out.setSerializeNulls(serializeNulls);

        out.beginObject();
        JsonUtil.writeJsonRpcId(out, null);
        assertEquals(serializeNulls, out.getSerializeNulls());
        out.name("other").nullValue();
        out.endObject();
        out.close();

        // "id": null is always written; other null members follow the writer's own setting
        JsonObject json = JsonParser.parseString(result.toString()).getAsJsonObject();
        assertTrue(json.has("id"));
        assertTrue(json.get("id").isJsonNull());
        assertEquals(serializeNulls, json.has("other"));
    }
}
