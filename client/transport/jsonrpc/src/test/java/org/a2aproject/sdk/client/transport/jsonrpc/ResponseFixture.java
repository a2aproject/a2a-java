package org.a2aproject.sdk.client.transport.jsonrpc;
import com.google.gson.JsonParser;
import org.mockserver.model.HttpRequest;
final class ResponseFixture {
    private ResponseFixture() {}
    static String echoId(String body, HttpRequest request) {
        String id = JsonParser.parseString(request.getBodyAsString()).getAsJsonObject().get("id").getAsString();
        if (body.startsWith("event:")) return body.replace("\"id\":1,", "\"id\":\"" + id + "\",");
        var obj = JsonParser.parseString(body).getAsJsonObject();
        obj.addProperty("id", id);
        return obj.toString();
    }
}
