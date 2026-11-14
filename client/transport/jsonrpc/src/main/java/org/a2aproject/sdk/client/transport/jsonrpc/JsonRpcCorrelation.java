package org.a2aproject.sdk.client.transport.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.a2aproject.sdk.spec.A2AClientException;

/** Validates that a JSON-RPC response belongs to the current request. */
public final class JsonRpcCorrelation {
    private JsonRpcCorrelation() {}

    /** Validate standard JSON-RPC error envelopes while leaving HTTP middleware errors unchanged. */
    public static void validateIfErrorEnvelope(String body, String expectedId) {
        final JsonObject response;
        try {
            response = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        if (response.has("jsonrpc") && response.get("jsonrpc").isJsonPrimitive()
                && "2.0".equals(response.get("jsonrpc").getAsString())
                && response.has("error") && !response.get("error").isJsonNull()) {
            validate(body, expectedId);
        }
    }

    public static void validate(String body, String expectedId) {
        final JsonObject response;
        try {
            response = JsonParser.parseString(body).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new A2AClientException("Invalid JSON-RPC response", e);
        }
        JsonElement id = response.get("id");
        boolean error = response.has("error") && !response.get("error").isJsonNull();
        if (id != null && ((id.isJsonNull() && error)
                || (id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() && expectedId.equals(id.getAsString())))) {
            return;
        }
        throw new A2AClientException("JSON-RPC response ID does not match the request ID");
    }
}
