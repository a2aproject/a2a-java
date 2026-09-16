package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

class JSONRPCCompat03ClientTransportTest {
    @Test
    void providerTargetsJsonRpcAndOrdinaryJsonRpcConfiguration() {
        JSONRPCCompat03ClientTransportProvider provider = new JSONRPCCompat03ClientTransportProvider();

        assertEquals("JSONRPC", provider.protocolBinding());
        assertEquals("0.3", provider.protocolVersion());
        assertEquals(JSONRPCTransport.class, provider.configuredTransportClass());
        assertTrue(provider.getClass().getPackageName().contains("adapter.jsonrpc"));
    }

    @Test
    void rejectsTenantBeforeCallingLegacyDelegate() {
        RecordingDelegate delegate = new RecordingDelegate();
        JSONRPCCompat03ClientTransport transport = new JSONRPCCompat03ClientTransport(
                delegate, testCard(), List.of());

        assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.subscribeToTask(new org.a2aproject.sdk.spec.TaskIdParams("task", "tenant"),
                        event -> { }, error -> { }, (ClientCallContext) null));
        assertFalse(delegate.called);
    }

    private static AgentCard testCard() {
        return AgentCard.builder().name("agent").description("description").version("1")
                .capabilities(new AgentCapabilities(false, false, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of()).supportedInterfaces(List.of(new AgentInterface("JSONRPC", "https://example.test", null, "0.3")))
                .build();
    }

    private static final class RecordingDelegate implements ClientTransport_v0_3 {
        boolean called;

        @Override public EventKind_v0_3 sendMessage(MessageSendParams_v0_3 request, ClientCallContext_v0_3 context) { return null; }
        @Override public void sendMessageStreaming(MessageSendParams_v0_3 request, java.util.function.Consumer<StreamingEventKind_v0_3> events,
                java.util.function.Consumer<Throwable> errors, ClientCallContext_v0_3 context) { }
        @Override public org.a2aproject.sdk.compat03.spec.Task_v0_3 getTask(TaskQueryParams_v0_3 request, ClientCallContext_v0_3 context) { return null; }
        @Override public org.a2aproject.sdk.compat03.spec.Task_v0_3 cancelTask(TaskIdParams_v0_3 request, ClientCallContext_v0_3 context) { return null; }
        @Override public TaskPushNotificationConfig_v0_3 setTaskPushNotificationConfiguration(TaskPushNotificationConfig_v0_3 request, ClientCallContext_v0_3 context) { return null; }
        @Override public TaskPushNotificationConfig_v0_3 getTaskPushNotificationConfiguration(GetTaskPushNotificationConfigParams_v0_3 request, ClientCallContext_v0_3 context) { return null; }
        @Override public List<TaskPushNotificationConfig_v0_3> listTaskPushNotificationConfigurations(ListTaskPushNotificationConfigParams_v0_3 request, ClientCallContext_v0_3 context) { return List.of(); }
        @Override public void deleteTaskPushNotificationConfigurations(DeleteTaskPushNotificationConfigParams_v0_3 request, ClientCallContext_v0_3 context) { }
        @Override public void resubscribe(TaskIdParams_v0_3 request, java.util.function.Consumer<StreamingEventKind_v0_3> events,
                java.util.function.Consumer<Throwable> errors, ClientCallContext_v0_3 context) { called = true; }
        @Override public AgentCard_v0_3 getAgentCard(ClientCallContext_v0_3 context) { return null; }
        @Override public void close() { }
    }
}
