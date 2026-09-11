package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TaskPushNotificationConfigTest {

    @Test
    void builderAllowsAnOmittedConfigurationId() {
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .taskId("task-123")
                .url("https://example.com/callback")
                .build();

        assertNull(config.id());
        assertEquals("task-123", config.taskId());
    }
}
