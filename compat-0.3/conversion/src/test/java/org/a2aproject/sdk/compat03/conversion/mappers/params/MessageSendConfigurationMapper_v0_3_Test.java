package org.a2aproject.sdk.compat03.conversion.mappers.params;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskPushNotificationConfigMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class MessageSendConfigurationMapper_v0_3_Test {

    @ParameterizedTest
    @NullAndEmptySource
    void inlinePushConfigurationDoesNotRequireTaskId(@Nullable String taskId) {
        TaskPushNotificationConfig push = new TaskPushNotificationConfig(
                "config", taskId, "https://callback.example.test", "token",
                new AuthenticationInfo("Bearer", "credentials"), null);
        MessageSendConfiguration current = new MessageSendConfiguration(List.of("text/plain"), 3, push, false);

        MessageSendConfiguration_v0_3 legacy = MessageSendConfigurationMapper_v0_3.INSTANCE.fromV10(current);

        assertEquals(new MessageSendConfiguration_v0_3(List.of("text/plain"), 3,
                new PushNotificationConfig_v0_3("https://callback.example.test", "token",
                        new PushNotificationAuthenticationInfo_v0_3(List.of("Bearer"), "credentials"), "config"),
                true), legacy);
        MessageSendConfiguration roundTrip = MessageSendConfigurationMapper_v0_3.INSTANCE.toV10(legacy);
        assertEquals(new TaskPushNotificationConfig("config", "", "https://callback.example.test", "token",
                new AuthenticationInfo("Bearer", "credentials"), ""), roundTrip.taskPushNotificationConfig());
        assertEquals(List.of("text/plain"), roundTrip.acceptedOutputModes());
        assertEquals(3, roundTrip.historyLength());
        assertEquals(false, roundTrip.returnImmediately());
    }

    @Test
    void standalonePushConfigurationStillRequiresTaskId() {
        TaskPushNotificationConfig push = TaskPushNotificationConfig.builder()
                .url("https://callback.example.test").build();

        assertThrows(IllegalArgumentException.class,
                () -> TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(push));
    }

    @Test
    void standalonePushConfigurationRetainsTaskIdAndCallbackFields() {
        TaskPushNotificationConfig current = new TaskPushNotificationConfig(
                "config", "task", "https://callback.example.test", "token",
                new AuthenticationInfo("Bearer", "credentials"), "");

        TaskPushNotificationConfig_v0_3 legacy = TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(current);

        assertEquals(new TaskPushNotificationConfig_v0_3("task",
                new PushNotificationConfig_v0_3("https://callback.example.test", "token",
                        new PushNotificationAuthenticationInfo_v0_3(List.of("Bearer"), "credentials"), "config")), legacy);
        assertEquals(current, TaskPushNotificationConfigMapper_v0_3.INSTANCE.toV10(legacy));
    }
}
