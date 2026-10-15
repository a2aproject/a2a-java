package org.a2aproject.sdk.compat03.client.adapter.grpc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportBase;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class GrpcCompat03UnsupportedFieldsTest {
    @Test
    void rejectsOriginalFieldsAndInterceptorMutationsBeforeDelegation() {
        AgentCard card = AgentCard.builder().name("legacy").description("legacy").version("1")
                .url("https://example.test").capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface("GRPC", "https://example.test", null, "0.3"))).build();
        ManagedChannel channel = InProcessChannelBuilder.forName(InProcessServerBuilder.generateName())
                .directExecutor().build();
        var delegate = new GrpcTransport_v0_3(channel, Compat03ClientTransportBase.legacyCard(card)) {
            @Override
            public EventKind_v0_3 sendMessage(MessageSendParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) {
                throw new AssertionError("Unsupported request reached the delegate");
            }

            @Override
            public void sendMessageStreaming(MessageSendParams_v0_3 request, Consumer<StreamingEventKind_v0_3> events,
                    Consumer<Throwable> errors, @Nullable ClientCallContext_v0_3 context) {
                throw new AssertionError("Unsupported streaming request reached the delegate");
            }

            @Override
            public Task_v0_3 cancelTask(TaskIdParams_v0_3 request, @Nullable ClientCallContext_v0_3 context) {
                throw new AssertionError("Unsupported cancellation reached the delegate");
            }
        };
        try {
            for (boolean intercepted : List.of(false, true)) {
                var transport = new GrpcCompat03ClientTransport(delegate, card,
                        intercepted ? List.of(unsupportedFieldsInterceptor()) : List.of());
                var request = new MessageSendParams(new Message(Message.Role.ROLE_USER, List.of(new TextPart("hello")),
                        "request", null, null, intercepted ? null : List.of("referenced-task"), null, null), null, null);
                assertUnsupported("referenceTaskIds", () -> transport.sendMessage(request, null));
                assertUnsupported("referenceTaskIds", () -> transport.sendMessageStreaming(request,
                        event -> { }, error -> { throw new AssertionError(error); }, null));
                var cancel = new CancelTaskParams("task", null, intercepted ? Map.of() : Map.of("reason", "stop"));
                assertUnsupported("metadata", () -> transport.cancelTask(cancel, null));
            }
        } finally {
            channel.shutdownNow();
        }
    }

    private static void assertUnsupported(String field, Runnable call) {
        A2AClientException error = assertThrows(A2AClientException.class, call::run);
        assertInstanceOf(UnsupportedOperationError.class, error.getCause());
        assertTrue(error.getMessage().contains(field));
    }

    private static ClientCallInterceptor unsupportedFieldsInterceptor() {
        return new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, @Nullable Object payload, Map<String, String> headers,
                    @Nullable AgentCard card, @Nullable ClientCallContext context) {
                if (payload instanceof org.a2aproject.sdk.grpc.SendMessageRequest) {
                    var request = ((org.a2aproject.sdk.grpc.SendMessageRequest) payload).toBuilder();
                    request.getMessageBuilder().addReferenceTaskIds("referenced-task");
                    return new PayloadAndHeaders(request.build(), headers);
                }
                Struct metadata = Struct.newBuilder().putFields("reason", Value.newBuilder().setStringValue("stop").build()).build();
                var request = ((org.a2aproject.sdk.grpc.CancelTaskRequest) payload).toBuilder();
                request.setMetadata(metadata);
                return new PayloadAndHeaders(request.build(), headers);
            }
        };
    }
}
