package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.junit.jupiter.api.Test;

class A2APushNotificationConfigRequestMapperTest {

    private final A2APushNotificationConfigRequestMapper mapper = new A2APushNotificationConfigRequestMapper();

    @Test
    void parsesCreateRequestWithOptionalIdAndTenant() {
        TaskPushNotificationConfig config = mapper.parseCreateRequest("""
                {
                  "url": "https://example.com/webhook",
                  "token": "token-1",
                  "tenant": "tenant-a"
                }
                """, "task-123", "tenant-a");

        assertEquals("", config.id());
        assertEquals("task-123", config.taskId());
        assertEquals("https://example.com/webhook", config.url());
        assertEquals("token-1", config.token());
        assertEquals("tenant-a", config.tenant());
    }

    @Test
    void rejectsTaskIdConflicts() {
        assertThrows(InvalidParamsError.class, () -> mapper.parseCreateRequest("""
                {
                  "taskId": "task-999",
                  "url": "https://example.com/webhook"
                }
                """, "task-123", "tenant-a"));
    }

    @Test
    void rejectsTenantConflicts() {
        assertThrows(InvalidParamsError.class, () -> mapper.parseCreateRequest("""
                {
                  "tenant": "tenant-b",
                  "url": "https://example.com/webhook"
                }
                """, "task-123", "tenant-a"));
    }
}
