package org.a2aproject.sdk.compat03.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

public class ProtoJsonUtils_v0_3_Test {

    @Test
    public void toJson_doesNotHtmlEscapeAngleBrackets() throws Exception {
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3(
            new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("<event-topic>")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils_v0_3.ToProto.sendMessageRequest(params);

        String json = ProtoJsonUtils_v0_3.toJson(JsonFormat.printer(), proto);

        assertTrue(json.contains("<event-topic>"),
            "JSON should preserve literal '<event-topic>' but got: " + json);
        assertFalse(json.contains("\\u003c"),
            "JSON must not contain HTML-escaped '<' (\\u003c) but got: " + json);
        assertFalse(json.contains("\\u003e"),
            "JSON must not contain HTML-escaped '>' (\\u003e) but got: " + json);
    }

    @Test
    public void toJson_doesNotHtmlEscapeAmpersand() throws Exception {
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3(
            new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("foo&bar")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils_v0_3.ToProto.sendMessageRequest(params);

        String json = ProtoJsonUtils_v0_3.toJson(JsonFormat.printer(), proto);

        assertTrue(json.contains("foo&bar"),
            "JSON should preserve literal '&' but got: " + json);
        assertFalse(json.contains("\\u0026"),
            "JSON must not contain HTML-escaped '&' (\\u0026) but got: " + json);
    }

    @Test
    public void toJson_respectsAlwaysPrintFieldsWithNoPresence() throws Exception {
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3(
            new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("hello")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils_v0_3.ToProto.sendMessageRequest(params);

        String withDefaults = ProtoJsonUtils_v0_3.toJson(
            JsonFormat.printer().alwaysPrintFieldsWithNoPresence(), proto);
        String withoutDefaults = ProtoJsonUtils_v0_3.toJson(JsonFormat.printer(), proto);

        assertTrue(withDefaults.length() > withoutDefaults.length(),
            "alwaysPrintFieldsWithNoPresence should produce more fields, got:\n  with: " + withDefaults + "\n  without: " + withoutDefaults);
    }

    @Test
    public void toJson_returnsEmptyStringForNull() throws Exception {
        String result = ProtoJsonUtils_v0_3.toJson(JsonFormat.printer(), null);

        assertEquals("", result);
    }
}
