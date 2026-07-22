package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MessageSendParamsTest {

    @Test
    void testConstruction_withAllFields() {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("hello"))
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration(List.of("text"), 2, null, Boolean.TRUE);
        Map<String, Object> metadata = Map.of("source", "web");

        MessageSendParams params = new MessageSendParams(message, configuration, metadata);

        assertEquals(message, params.message());
        assertEquals(configuration, params.configuration());
        assertEquals(metadata, params.metadata());
        assertNull(params.tenant());
    }

    @Test
    void testConstruction_nullMetadata() {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("hello"))
                .build();

        MessageSendParams params = new MessageSendParams(message, null, null);

        assertNull(params.metadata());
    }

    @Test
    void testConstruction_nullMessage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new MessageSendParams(null, null, null));
    }

    @Test
    void testMetadata_isDefensivelyCopied() {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("hello"))
                .build();
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("source", "web");

        MessageSendParams params = new MessageSendParams(message, null, mutableMetadata);
        mutableMetadata.put("write", "should-not-leak");

        assertEquals(Map.of("source", "web"), params.metadata());
        assertThrows(UnsupportedOperationException.class, () -> params.metadata().put("another", "value"));
    }

    @Test
    void testMetadata_preservesNullValues() {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("hello"))
                .build();
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("foo", null);

        MessageSendParams params = new MessageSendParams(message, null, mutableMetadata);

        assertEquals(1, params.metadata().size());
        assertTrue(params.metadata().containsKey("foo"));
        assertNull(params.metadata().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> params.metadata().put("another", "value"));
    }
}
