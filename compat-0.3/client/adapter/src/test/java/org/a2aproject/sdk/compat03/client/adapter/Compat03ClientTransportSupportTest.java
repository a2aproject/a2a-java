package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

class Compat03ClientTransportSupportTest {

    @Test
    void rejectsUnsupportedOperationsBeforeDelegateUse() {
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validateListTasks(null));
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validateExtendedAgentCard(null));
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validateTenant("getTask", "tenant"));
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validatePushConfig(
                        TaskPushNotificationConfig.builder().taskId("task").url("https://example.test")
                                .tenant("tenant").build()));
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validatePushList(
                        new ListTaskPushNotificationConfigsParams("task", 10, "", null)));
    }

    @Test
    void acceptsDefaultPushListAndReturnsEmptyPageToken() {
        var result = Compat03ClientTransportSupport.toV10PushList(java.util.List.of());

        assertEquals(java.util.List.of(), result.configs());
        assertEquals("", result.nextPageToken());
        Compat03ClientTransportSupport.validatePushList(
                new ListTaskPushNotificationConfigsParams("task", 0, "", null));
    }

    @Test
    void mapsContextsAndRequestParametersInBothDirections() {
        ClientCallContext context = new ClientCallContext(
                Map.of("trace", "one"), Map.of("Authorization", "Bearer token"));

        ClientCallContext_v0_3 legacyContext = Compat03ClientTransportSupport.toV03Context(context);

        assertEquals(context.getState(), legacyContext.getState());
        assertEquals(context.getHeaders(), legacyContext.getHeaders());
        assertEquals("task", Compat03ClientTransportSupport.toV03(
                new TaskQueryParams("task", 3, null)).id());
        assertEquals("task", Compat03ClientTransportSupport.toV03(
                new TaskIdParams("task", null)).id());
        assertEquals("task", Compat03ClientTransportSupport.toV03(
                new CancelTaskParams("task", null, Map.of())).id());
        MessageSendParams message = new MessageSendParams(
                new org.a2aproject.sdk.spec.Message(
                        org.a2aproject.sdk.spec.Message.Role.ROLE_USER,
                        java.util.List.of(new TextPart("hello")), "message", null, null, null, null, null),
                null, null, null);
        assertEquals(null, Compat03ClientTransportSupport.toV03(message).metadata());
    }

    @Test
    void rejectsGenericParametersAndMapsLegacyErrors() {
        assertThrows(A2AClientException.class,
                () -> Compat03ClientTransportSupport.validateParameters(Map.of("unsupported", true)));
        Compat03ClientTransportSupport.validateParameters(Map.of());

        A2AClientException mapped = Compat03ClientTransportSupport.mapLegacyException(
                new org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3(
                        "legacy failure", new org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3()));
        assertTrue(mapped.getMessage().contains("legacy failure"));
        assertTrue(mapped.getCause() instanceof org.a2aproject.sdk.spec.TaskNotFoundError);
    }
}
