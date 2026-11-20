package org.a2aproject.sdk.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.grpc.AgentCard;
import org.a2aproject.sdk.grpc.Role;
import org.a2aproject.sdk.grpc.SendMessageRequest;
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

    @Test
    public void toJson_keepsALiteralBackslashBeforeUEscapeInMessageText() throws Exception {
        String text = "use " + "\\" + "u003c to escape a tag";
        MessageSendParams params = new MessageSendParams(
            Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(Collections.singletonList(new TextPart(text)))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        var proto = ProtoUtils.ToProto.sendMessageRequest(params);

        String json = ProtoJsonUtils.toJson(JsonFormat.printer(), proto);

        // The body is still valid JSON, and the text survives the round trip untouched.
        var parsed = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
        JsonFormat.parser().merge(json, parsed);
        assertEquals(text, parsed.getMessage().getParts(0).getText());
    }

    @Test
    public void removeUnknownFields_removesUnknownKeysAtEveryLevel() {
        JsonElement json = JsonParser.parseString("""
            {
              "message": {
                "messageId": "message-1234",
                "context_id": "context-1234",
                "role": "ROLE_USER",
                "parts": [{"text": "hi", "unknownPartField": 1}],
                "metadata": {"unknownLooking": {"nested": true}},
                "unknownMessageField": "x"
              },
              "unknownParam": 42
            }
            """);
        String before = json.toString();

        JsonElement result = ProtoJsonUtils.removeUnknownFields(json, SendMessageRequest.getDescriptor());

        // Both the JSON name and the proto name of a field are kept, and the contents of a
        // google.protobuf.Struct are not described by a descriptor, so they are kept too.
        assertEquals(JsonParser.parseString("""
            {
              "message": {
                "messageId": "message-1234",
                "context_id": "context-1234",
                "role": "ROLE_USER",
                "parts": [{"text": "hi"}],
                "metadata": {"unknownLooking": {"nested": true}}
              }
            }
            """), result);
        assertEquals(before, json.toString(), "the input must not be modified");
    }

    @Test
    public void removeUnknownFields_removesUnknownKeysInMapValues() {
        JsonElement json = JsonParser.parseString("""
            {
              "name": "agent",
              "securitySchemes": {
                "bearer": {
                  "httpAuthSecurityScheme": {"scheme": "Bearer", "unknownSchemeField": 1},
                  "unknownSecuritySchemeField": 2
                }
              }
            }
            """);

        JsonElement result = ProtoJsonUtils.removeUnknownFields(json, AgentCard.getDescriptor());

        assertEquals(JsonParser.parseString("""
            {
              "name": "agent",
              "securitySchemes": {
                "bearer": {
                  "httpAuthSecurityScheme": {"scheme": "Bearer"}
                }
              }
            }
            """), result);
    }

    @Test
    public void mergeIgnoringUnknownFields_ignoresUnknownFields() throws Exception {
        SendMessageRequest.Builder builder = SendMessageRequest.newBuilder();

        ProtoJsonUtils.mergeIgnoringUnknownFields("""
            {"message": {"messageId": "m1", "role": "ROLE_USER", "parts": [{"text": "hi"}], "unknown": 1}, "unknownParam": 2}
            """, builder);

        assertEquals("m1", builder.getMessage().getMessageId());
        assertEquals(Role.ROLE_USER, builder.getMessage().getRole());
    }

    @Test
    public void mergeIgnoringUnknownFields_rejectsUnrecognizedEnumValue() {
        InvalidProtocolBufferException exception = assertThrows(InvalidProtocolBufferException.class,
            () -> ProtoJsonUtils.mergeIgnoringUnknownFields("""
                {"message": {"messageId": "m1", "role": "NOT_A_ROLE", "parts": [{"text": "hi"}]}}
                """, SendMessageRequest.newBuilder()));
        assertTrue(exception.getMessage().contains("NOT_A_ROLE"), exception.getMessage());
    }

    @Test
    public void mergeIgnoringUnknownFields_rejectsMalformedJson() {
        assertThrows(InvalidProtocolBufferException.class,
            () -> ProtoJsonUtils.mergeIgnoringUnknownFields("{\"message\": ", SendMessageRequest.newBuilder()));
    }
}
