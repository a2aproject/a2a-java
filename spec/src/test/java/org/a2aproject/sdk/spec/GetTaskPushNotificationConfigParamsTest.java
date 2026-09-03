package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GetTaskPushNotificationConfigParamsTest {

    @Test
    void testConstructionAllowsOmittedConfigurationId() {
        GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams("task-1");

        assertEquals("task-1", params.taskId());
        assertNull(params.id());
    }

    @Test
    void testBuilderAllowsOmittedConfigurationId() {
        GetTaskPushNotificationConfigParams params = GetTaskPushNotificationConfigParams.builder()
                .taskId("task-1")
                .build();

        assertNull(params.id());
    }
}
