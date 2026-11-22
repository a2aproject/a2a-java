package org.a2aproject.sdk.server.util.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

class SseFormatterTest {
    @Test
    void directStreamingMessageOmitsTaskId() {
        Message message = Message.builder().messageId("message").contextId("context")
                .role(Message.Role.ROLE_AGENT).parts(List.of(new TextPart("hello"))).build();
        String event = SseFormatter.formatResponseAsSSE(new SendStreamingMessageResponse("request", message), 0);
        String data = event.lines().filter(line -> line.startsWith("data: ")).findFirst().orElseThrow().substring(6);
        JsonObject result = JsonParser.parseString(data).getAsJsonObject().getAsJsonObject("result")
                .getAsJsonObject("message");
        assertFalse(result.has("taskId"));
        assertEquals("context", result.get("contextId").getAsString());
    }
    @Test
    void taskAssociatedStreamingMessageKeepsTaskId() {
        Message message = Message.builder().messageId("message").taskId("task").contextId("context")
                .role(Message.Role.ROLE_AGENT).parts(List.of(new TextPart("hello"))).build();
        String event = SseFormatter.formatResponseAsSSE(new SendStreamingMessageResponse("request", message), 0);
        String data = event.lines().filter(line -> line.startsWith("data: ")).findFirst().orElseThrow().substring(6);
        JsonObject result = JsonParser.parseString(data).getAsJsonObject().getAsJsonObject("result")
                .getAsJsonObject("message");
        assertEquals("task", result.get("taskId").getAsString());
    }
}
