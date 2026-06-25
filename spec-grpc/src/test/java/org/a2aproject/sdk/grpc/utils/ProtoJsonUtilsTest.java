package org.a2aproject.sdk.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

public class ProtoJsonUtilsTest {

    @Test
    public void toJson_doesNotHtmlEscapeAngleBrackets() throws Exception {
        MessageSendParams params = new MessageSendParams(
            Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(Collections.singletonList(new TextPart("<event-topic>")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils.ToProto.sendMessageRequest(params);

        String json = ProtoJsonUtils.toJson(JsonFormat.printer(), proto);

        assertTrue(json.contains("<event-topic>"),
            "JSON should preserve literal '<event-topic>' but got: " + json);
        assertFalse(json.contains("\\u003c"),
            "JSON must not contain HTML-escaped '<' (\\u003c) but got: " + json);
        assertFalse(json.contains("\\u003e"),
            "JSON must not contain HTML-escaped '>' (\\u003e) but got: " + json);
    }

    @Test
    public void toJson_doesNotHtmlEscapeAmpersand() throws Exception {
        MessageSendParams params = new MessageSendParams(
            Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(Collections.singletonList(new TextPart("foo&bar")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils.ToProto.sendMessageRequest(params);

        String json = ProtoJsonUtils.toJson(JsonFormat.printer(), proto);

        assertTrue(json.contains("foo&bar"),
            "JSON should preserve literal '&' but got: " + json);
        assertFalse(json.contains("\\u0026"),
            "JSON must not contain HTML-escaped '&' (\\u0026) but got: " + json);
    }

    @Test
    public void toJson_respectsAlwaysPrintFieldsWithNoPresence() throws Exception {
        MessageSendParams params = new MessageSendParams(
            Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(Collections.singletonList(new TextPart("hello")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils.ToProto.sendMessageRequest(params);

        String withDefaults = ProtoJsonUtils.toJson(
            JsonFormat.printer().alwaysPrintFieldsWithNoPresence(), proto);
        String withoutDefaults = ProtoJsonUtils.toJson(JsonFormat.printer(), proto);

        assertTrue(withDefaults.length() > withoutDefaults.length(),
            "alwaysPrintFieldsWithNoPresence should produce more fields, got:\n  with: " + withDefaults + "\n  without: " + withoutDefaults);
    }

    @Test
    public void toJson_returnsEmptyStringForNull() throws Exception {
        String result = ProtoJsonUtils.toJson(JsonFormat.printer(), null);

        assertEquals("", result);
    }
}
