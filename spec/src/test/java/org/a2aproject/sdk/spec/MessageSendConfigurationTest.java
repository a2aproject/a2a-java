package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MessageSendConfigurationTest {

    @Test
    void testConstruction_withAllFields() {
        List<String> acceptedOutputModes = List.of("text", "audio");
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .id("id")
                .taskId("task-123")
                .url("https://example.com")
                .build();

        MessageSendConfiguration result = new MessageSendConfiguration(acceptedOutputModes, 3, config, Boolean.TRUE);

        assertEquals(acceptedOutputModes, result.acceptedOutputModes());
        assertEquals(Integer.valueOf(3), result.historyLength());
        assertEquals(config, result.taskPushNotificationConfig());
        assertEquals(Boolean.TRUE, result.returnImmediately());
    }

    @Test
    void testConstruction_withNullAcceptedOutputModes() {
        MessageSendConfiguration result = new MessageSendConfiguration(null, null, null, null);

        assertNull(result.acceptedOutputModes());
        assertNull(result.historyLength());
        assertNull(result.taskPushNotificationConfig());
        assertNull(result.returnImmediately());
    }

    @Test
    void testConstruction_negativeHistoryLength_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new MessageSendConfiguration(List.of("text"), -1, null, null));
    }

    @Test
    void testAcceptedOutputModes_areDefensivelyCopied() {
        List<String> mutableModes = new ArrayList<>();
        mutableModes.add("text");

        MessageSendConfiguration result = new MessageSendConfiguration(mutableModes, null, null, null);
        mutableModes.add("audio");

        assertEquals(List.of("text"), result.acceptedOutputModes());
        assertThrows(UnsupportedOperationException.class, () -> result.acceptedOutputModes().add("video"));
    }

    @Test
    void testBuilderAcceptedOutputModes_areDefensivelyCopiedByRecord() {
        List<String> mutableModes = new ArrayList<>();
        mutableModes.add("text");

        MessageSendConfiguration result = MessageSendConfiguration.builder()
                .acceptedOutputModes(mutableModes)
                .build();
        mutableModes.add("audio");

        assertEquals(List.of("text"), result.acceptedOutputModes());
    }
}
