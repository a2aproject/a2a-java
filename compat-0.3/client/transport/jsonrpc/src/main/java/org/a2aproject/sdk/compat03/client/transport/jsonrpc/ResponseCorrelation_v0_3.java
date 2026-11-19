package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Validates the JSON-RPC envelope against the ID on the actual wire request. */
public final class ResponseCorrelation_v0_3 {
    private ResponseCorrelation_v0_3() { }

    public static JsonElement requestId(String requestJson) {
        JsonElement id = JsonParser.parseString(requestJson).getAsJsonObject().get("id");
        if (id == null) {
            throw new IllegalArgumentException("JSON-RPC request has no ID");
        }
        return id;
    }

    public static void validate(JsonObject response, JsonElement expectedId) {
        JsonElement id = response.get("id");
        boolean nullError = id != null && id.isJsonNull() && response.has("error") && !response.get("error").isJsonNull();
        if (id == null || (!nullError && !id.equals(expectedId))) {
            throw new IllegalArgumentException("JSON-RPC response ID does not match request ID");
        }
    }
}
