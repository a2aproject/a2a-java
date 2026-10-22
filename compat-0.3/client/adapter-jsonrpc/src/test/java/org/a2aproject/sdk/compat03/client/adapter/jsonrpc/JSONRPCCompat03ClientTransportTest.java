package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.junit.jupiter.api.Test;

class JSONRPCCompat03ClientTransportTest {
    @Test
    void preservesSingletonArrayNumbersAndAppliesMapAdditionsRemovalsAndReplacements() {
        long id = 9007199254740993L;
        double projectedId = (double) id;
        var original = new TaskIdParams_v0_3("task", Map.of(
                "items", List.of(Map.of("id", id, "counter", 1L)), "remove", "old", "replace", id));
        var before = new TaskIdParams_v0_3("task", Map.of(
                "items", List.of(Map.of("id", projectedId, "counter", 1.0)), "remove", "old", "replace", projectedId));
        var after = new TaskIdParams_v0_3("task", Map.of(
                "items", List.of(Map.of("id", projectedId, "counter", 2.0)), "add", true, "replace", 42.0));
        var result = JSONRPCCompat03PayloadSupport.preserveUnchangedValues(original, before, after);
        assertEquals(Map.of("items", List.of(Map.of("id", id, "counter", 2.0)),
                "add", true, "replace", 42.0), result.metadata());
    }

    @Test
    void acceptsLosslessArrayEditsAndTreatsIdenticalProtobufNumbersAsUnchanged() {
        var original = new TaskIdParams_v0_3("task", Map.of("ids", List.of(1L, 7L)));
        var before = new TaskIdParams_v0_3("task", Map.of("ids", List.of(1.0, 7.0)));
        var after = new TaskIdParams_v0_3("task", Map.of("ids", List.of(7.0)));
        assertEquals(after, JSONRPCCompat03PayloadSupport.preserveUnchangedValues(original, before, after));

        // The interceptor contract treats identical protobuf values as unchanged, even for replacements.
        var exact = new TaskIdParams_v0_3("task", Map.of("id", 9007199254740993L));
        var projected = new TaskIdParams_v0_3("task", Map.of("id", 9007199254740992.0));
        assertEquals(exact, JSONRPCCompat03PayloadSupport.preserveUnchangedValues(exact, projected, projected));
    }

    @Test
    void preservesMetadataWhenAnInterceptorChangesASingletonPartKind() {
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                var request = (org.a2aproject.sdk.grpc.SendMessageRequest) payload;
                var part = request.getMessage().getParts(0).toBuilder().setData(
                        com.google.protobuf.Value.newBuilder().setStructValue(
                                com.google.protobuf.Struct.newBuilder().putFields("new",
                                        com.google.protobuf.Value.newBuilder().setBoolValue(true).build())));
                return new PayloadAndHeaders(request.toBuilder().setMessage(request.getMessage().toBuilder()
                        .setParts(0, part)).build(), headers);
            }
        };
        RecordingDelegate delegate = new RecordingDelegate();
        delegate.response = new Message_v0_3.Builder().role(Message_v0_3.Role.AGENT)
                .messageId("response").parts(List.of(new org.a2aproject.sdk.compat03.spec.TextPart_v0_3("ok"))).build();
        var transport = new JSONRPCCompat03ClientTransport(delegate, testCard(), List.of(interceptor));
        var request = new MessageSendParams(new Message(Message.Role.ROLE_USER,
                List.of(new TextPart("original", Map.of("id", 9007199254740993L))),
                "request", null, null, null, null, null), null, null);
        transport.sendMessage(request, null);
        var part = assertInstanceOf(org.a2aproject.sdk.compat03.spec.DataPart_v0_3.class,
                delegate.sent.message().parts().get(0));
        assertEquals(Map.of("new", true), part.data());
        assertEquals(Map.of("id", 9007199254740993L), part.metadata());
        transport.sendMessageStreaming(request, event -> { }, failure -> { }, null);
        part = assertInstanceOf(org.a2aproject.sdk.compat03.spec.DataPart_v0_3.class,
                delegate.sent.message().parts().get(0));
        assertEquals(Map.of("id", 9007199254740993L), part.metadata());
    }

    @Test
    void rejectsAmbiguousArrayEditsBeforeBlockingOrStreamingDelegation() {
        // Removing A and editing B must neither round B's untouched ID nor restore A's stale ID.
        assertAmbiguousMutationRejected(
                List.of(new DataPart(Map.of("id", 1L, "counter", 1L)),
                        new DataPart(Map.of("id", 9007199254740993L, "counter", 1L))),
                List.of(new DataPart(Map.of("id", 9007199254740992L, "counter", 2L))));
        assertAmbiguousMutationRejected(
                List.of(new DataPart(Map.of("id", 9007199254740993L, "counter", 1L)),
                        new DataPart(Map.of("id", 7L, "counter", 1L))),
                List.of(new DataPart(Map.of("id", 9007199254740992L, "counter", 2L))));
        // Both original integers project to the same double, so exact matching cannot identify the survivor.
        assertAmbiguousMutationRejected(
                List.of(new DataPart(Map.of("nested", List.of(9007199254740992L, 9007199254740993L)))),
                List.of(new DataPart(Map.of("nested", List.of(9007199254740992L)))));
        assertAmbiguousMutationRejected(
                List.of(new DataPart(Map.of("id", 9007199254740993L)), new TextPart("move me")),
                List.of(new TextPart("move me"), new DataPart(Map.of("id", 9007199254740992L))));
    }

    private static void assertAmbiguousMutationRejected(List<org.a2aproject.sdk.spec.Part<?>> before,
            List<org.a2aproject.sdk.spec.Part<?>> after) {
        Message message = new Message(Message.Role.ROLE_USER, before, "request", null, null, null, null, null);
        Message replacement = Message.builder(message).parts(after).build();
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                var request = (org.a2aproject.sdk.grpc.SendMessageRequest) payload;
                return new PayloadAndHeaders(request.toBuilder()
                        .setMessage(org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.message(replacement)).build(), headers);
            }
        };
        RecordingDelegate delegate = new RecordingDelegate();
        var transport = new JSONRPCCompat03ClientTransport(delegate, testCard(), List.of(interceptor));
        var request = new MessageSendParams(message, null, null);
        var error = assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.sendMessage(request, null));
        assertTrue(error.getMessage().contains("non-singleton array"));
        assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.sendMessageStreaming(request, event -> { }, failure -> { }, null));
        assertNull(delegate.sent);
        assertFalse(delegate.called);
    }

    @Test
    void rejectsAmbiguousCancellationMetadataArrayEditsBeforeDelegation() {
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                var request = (org.a2aproject.sdk.grpc.CancelTaskRequest) payload;
                var values = request.getMetadata().getFieldsOrThrow("ids").getListValue();
                var remaining = com.google.protobuf.Value.newBuilder().setListValue(
                        com.google.protobuf.ListValue.newBuilder().addValues(values.getValues(1))).build();
                return new PayloadAndHeaders(request.toBuilder().setMetadata(request.getMetadata().toBuilder()
                        .putFields("ids", remaining)).build(), headers);
            }
        };
        RecordingDelegate delegate = new RecordingDelegate();
        var transport = new JSONRPCCompat03ClientTransport(delegate, testCard(), List.of(interceptor));
        assertThrows(org.a2aproject.sdk.spec.A2AClientException.class, () -> transport.cancelTask(
                new CancelTaskParams("task", null, Map.of("ids", List.of(9007199254740992L, 9007199254740993L))), null));
        assertNull(delegate.cancelled);
        assertFalse(delegate.called);
    }

    @Test
    void preservesLargeIntegersForBlockingStreamingAndCancellation() throws Exception {
        checkNumericPrecision(List.of(), false, false);
        checkNumericPrecision(List.of(new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                return new PayloadAndHeaders(payload, Map.of("A2A-Extensions", "urn:example:extension"));
            }
        }), false, true);
        checkNumericPrecision(List.of(new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                var value = com.google.protobuf.Value.newBuilder().setNumberValue(2).build();
                if (payload instanceof org.a2aproject.sdk.grpc.SendMessageRequest request) {
                    return new PayloadAndHeaders(request.toBuilder().setMessage(request.getMessage().toBuilder()
                            .setMessageId("changed"))
                            .setMetadata(request.getMetadata().toBuilder().putFields("counter", value)).build(), headers);
                }
                var request = (org.a2aproject.sdk.grpc.CancelTaskRequest) payload;
                return new PayloadAndHeaders(request.toBuilder().setId("changed-task")
                        .setMetadata(request.getMetadata().toBuilder().putFields("counter", value)).build(), headers);
            }
        }), true, false);
    }

    private static void checkNumericPrecision(List<ClientCallInterceptor> interceptors,
            boolean changedPayload, boolean extensionHeader) throws Exception {
        long id = 9007199254740993L;
        Map<String, Object> metadata = Map.of("id", id);
        BigDecimal decimal = new BigDecimal("0.12345678901234567890123456789");
        Map<String, Object> requestMetadata = Map.of("id", id, "counter", 1L, "decimal", decimal);
        RecordingDelegate delegate = new RecordingDelegate();
        delegate.response = new Message_v0_3.Builder().role(Message_v0_3.Role.AGENT)
                .messageId("response").parts(List.of(new org.a2aproject.sdk.compat03.spec.TextPart_v0_3("ok"))).build();
        var transport = new JSONRPCCompat03ClientTransport(delegate, testCard(), interceptors);
        var message = new Message(Message.Role.ROLE_USER,
                List.of(new DataPart(Map.of("nested", List.of(metadata)), metadata)),
                "request", null, null, null, metadata, null);
        var request = new MessageSendParams(message, null, requestMetadata);

        transport.sendMessage(request, null);
        assertExactNumbers(delegate.sent, id);
        assertEquals(changedPayload ? "changed" : "request", delegate.sent.message().messageId());
        assertEquals(changedPayload ? 2.0 : 1.0, ((Number) delegate.sent.metadata().get("counter")).doubleValue());
        assertEquals(decimal, delegate.sent.metadata().get("decimal"));
        transport.sendMessageStreaming(request, event -> { }, error -> { throw new AssertionError(error); }, null);
        assertExactNumbers(delegate.sent, id);
        assertEquals(changedPayload ? "changed" : "request", delegate.sent.message().messageId());
        assertEquals(changedPayload ? 2.0 : 1.0, ((Number) delegate.sent.metadata().get("counter")).doubleValue());
        assertEquals(decimal, delegate.sent.metadata().get("decimal"));
        transport.cancelTask(new CancelTaskParams("task", null, requestMetadata), null);
        assertEquals(id, delegate.cancelled.metadata().get("id"));
        assertEquals(decimal, delegate.cancelled.metadata().get("decimal"));
        assertEquals(changedPayload ? 2.0 : 1.0, ((Number) delegate.cancelled.metadata().get("counter")).doubleValue());
        assertTrue(JsonUtil_v0_3.toJson(delegate.cancelled).contains("9007199254740993"));
        assertEquals(changedPayload ? "changed-task" : "task", delegate.cancelled.id());
        if (extensionHeader) {
            assertEquals("urn:example:extension", delegate.context.getHeaders().get("X-A2A-Extensions"));
        }
        assertFalse(delegate.context.getHeaders().containsKey("A2A-Extensions"));
    }

    private static void assertExactNumbers(MessageSendParams_v0_3 request, long id) throws Exception {
        assertEquals(id, request.metadata().get("id"));
        assertEquals(Map.of("id", id), request.message().metadata());
        var part = assertInstanceOf(org.a2aproject.sdk.compat03.spec.DataPart_v0_3.class, request.message().parts().get(0));
        assertEquals(Map.of("id", id), part.metadata());
        assertEquals(Map.of("nested", List.of(Map.of("id", id))), part.data());
        assertTrue(JsonUtil_v0_3.toJson(request).contains("9007199254740993"));
    }

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

    @Test
    void providerRejectsTenantOnLegacyAgentInterface() {
        AgentCard card = cardWithInterfaceTenant();

        org.a2aproject.sdk.spec.A2AClientException exception = assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> new JSONRPCCompat03ClientTransportProvider().create(null, card, card.supportedInterfaces().get(0)));

        assertTrue(exception.getMessage().contains("tenant"));
    }

    @Test
    void providerRejectsWrongConfigurationType() {
        org.a2aproject.sdk.spec.A2AClientException exception = assertThrows(
                org.a2aproject.sdk.spec.A2AClientException.class,
                () -> new JSONRPCCompat03ClientTransportProvider().create(
                        new ClientTransportConfig<JSONRPCTransport>() { },
                        testCard(), testCard().supportedInterfaces().get(0)));

        assertTrue(exception.getMessage().contains("JSONRPCTransportConfig"));
    }

    @Test
    void providerRejectsNullConfiguration() {
        org.a2aproject.sdk.spec.A2AClientException exception = assertThrows(
                org.a2aproject.sdk.spec.A2AClientException.class,
                () -> new JSONRPCCompat03ClientTransportProvider().create(
                        null, testCard(), testCard().supportedInterfaces().get(0)));

        assertTrue(exception.getMessage().contains("JSONRPCTransportConfig"));
    }

    @Test
    void receivesInlineFilesWithoutMimeTypeForBlockingAndStreamingSends() throws Exception {
        RecordingDelegate delegate = new RecordingDelegate();
        delegate.response = JsonUtil_v0_3.fromJson("""
                {"kind":"message","role":"agent","messageId":"response",
                 "parts":[{"kind":"file","file":{"bytes":"aGVsbG8="}}]}
                """, Message_v0_3.class);
        JSONRPCCompat03ClientTransport transport = new JSONRPCCompat03ClientTransport(
                delegate, testCard(), List.of());
        MessageSendParams request = new MessageSendParams(
                new Message(Message.Role.ROLE_USER, List.of(new TextPart("hello")), "request",
                        null, null, null, null, null), null, null);

        Message blocking = assertInstanceOf(Message.class, transport.sendMessage(request, null));
        AtomicReference<org.a2aproject.sdk.spec.StreamingEventKind> received = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        transport.sendMessageStreaming(request, received::set, error::set, null);

        assertNull(error.get());
        Message streaming = assertInstanceOf(Message.class, received.get());
        for (Message response : List.of(blocking, streaming)) {
            assertEquals("response", response.messageId());
            FilePart part = assertInstanceOf(FilePart.class, response.parts().get(0));
            FileWithBytes file = assertInstanceOf(FileWithBytes.class, part.file());
            assertEquals("", file.mimeType());
            assertEquals("", file.name());
            assertEquals("aGVsbG8=", file.bytes());
        }
    }

    @Test
    void rejectsNonObjectDataIntroducedByAnInterceptorBeforeDelegation() {
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, java.util.Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                Message replacement = new Message(Message.Role.ROLE_USER, List.of(new DataPart(List.of("item"))),
                        "request", null, null, null, null, null);
                org.a2aproject.sdk.grpc.SendMessageRequest request =
                        (org.a2aproject.sdk.grpc.SendMessageRequest) payload;
                return new PayloadAndHeaders(request.toBuilder()
                        .setMessage(org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.message(replacement)).build(), headers);
            }
        };
        RecordingDelegate delegate = new RecordingDelegate();
        JSONRPCCompat03ClientTransport transport = new JSONRPCCompat03ClientTransport(
                delegate, testCard(), List.of(interceptor));
        MessageSendParams request = new MessageSendParams(
                new Message(Message.Role.ROLE_USER, List.of(new TextPart("hello")), "request",
                        null, null, null, null, null), null, null);

        var blocking = assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.sendMessage(request, null));
        var streaming = assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.sendMessageStreaming(request, event -> { }, error -> { }, null));

        assertInstanceOf(UnsupportedOperationError.class, blocking.getCause());
        assertInstanceOf(UnsupportedOperationError.class, streaming.getCause());
        assertFalse(delegate.called);
    }

    @Test
    void rejectsUnsupportedHistoryLengthIntroducedByAnInterceptor() {
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, java.util.Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                org.a2aproject.sdk.grpc.GetTaskRequest request =
                        (org.a2aproject.sdk.grpc.GetTaskRequest) payload;
                return new PayloadAndHeaders(request.toBuilder().setHistoryLength(0).build(), headers);
            }
        };
        RecordingDelegate delegate = new RecordingDelegate();
        JSONRPCCompat03ClientTransport transport = new JSONRPCCompat03ClientTransport(
                delegate, testCard(), List.of(interceptor));

        assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> transport.getTask(new TaskQueryParams("task", 1), null));
        assertFalse(delegate.called);
    }

    private static AgentCard testCard() {
        return AgentCard.builder().name("agent").description("description").version("1")
                .capabilities(new AgentCapabilities(false, false, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of()).supportedInterfaces(List.of(new AgentInterface("JSONRPC", "https://example.test", null, "0.3")))
                .build();
    }

    private static AgentCard cardWithInterfaceTenant() {
        return AgentCard.builder().name("agent").description("description").version("1")
                .capabilities(new AgentCapabilities(false, false, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of()).supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "https://example.test", "tenant", "0.3")))
                .build();
    }

    private static final class RecordingDelegate implements ClientTransport_v0_3 {
        boolean called;
        Message_v0_3 response;
        MessageSendParams_v0_3 sent;
        TaskIdParams_v0_3 cancelled;
        ClientCallContext_v0_3 context;

        @Override public EventKind_v0_3 sendMessage(MessageSendParams_v0_3 request, ClientCallContext_v0_3 context) {
            called = true;
            sent = request;
            this.context = context;
            assertNotNull(response);
            return response;
        }
        @Override public void sendMessageStreaming(MessageSendParams_v0_3 request, java.util.function.Consumer<StreamingEventKind_v0_3> events,
                java.util.function.Consumer<Throwable> errors, ClientCallContext_v0_3 context) {
            called = true;
            sent = request;
            this.context = context;
            assertNotNull(response);
            events.accept(response);
        }
        @Override public org.a2aproject.sdk.compat03.spec.Task_v0_3 getTask(TaskQueryParams_v0_3 request, ClientCallContext_v0_3 context) { called = true; return null; }
        @Override public org.a2aproject.sdk.compat03.spec.Task_v0_3 cancelTask(TaskIdParams_v0_3 request, ClientCallContext_v0_3 context) {
            cancelled = request;
            this.context = context;
            return new org.a2aproject.sdk.compat03.spec.Task_v0_3(request.id(), "context",
                    new org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3(org.a2aproject.sdk.compat03.spec.TaskState_v0_3.CANCELED),
                    List.of(), List.of(), null);
        }
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
