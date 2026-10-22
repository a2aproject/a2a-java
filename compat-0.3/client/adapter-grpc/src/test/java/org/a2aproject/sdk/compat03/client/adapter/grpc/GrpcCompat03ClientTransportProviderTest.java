package org.a2aproject.sdk.compat03.client.adapter.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.compat03.grpc.Message;
import org.a2aproject.sdk.compat03.grpc.Part;
import org.a2aproject.sdk.compat03.grpc.Role;
import org.a2aproject.sdk.compat03.grpc.SendMessageResponse;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

class GrpcCompat03ClientTransportProviderTest {
    @Test
    void sendsCompletePushConfigurationResourceNamesIncludingDefaultAndClearedIds() throws Exception {
        String name = InProcessServerBuilder.generateName();
        AtomicReference<String> receivedName = new AtomicReference<>();
        Server server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new A2AServiceGrpc.A2AServiceImplBase() {
                    @Override
                    public void getTaskPushNotificationConfig(
                            org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest request,
                            StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> observer) {
                        receivedName.set(request.getName());
                        // Validate the contract directly rather than using the permissive reference parser.
                        if (!request.getName().matches("tasks/task-123/pushNotificationConfigs/(task-123|specific)")) {
                            observer.onError(Status.INVALID_ARGUMENT.withDescription("Invalid config resource name")
                                    .asRuntimeException());
                            return;
                        }
                        String configId = request.getName().substring(request.getName().lastIndexOf('/') + 1);
                        observer.onNext(org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder()
                                .setName(request.getName())
                                .setPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.PushNotificationConfig.newBuilder()
                                        .setId(configId).setUrl("https://example.test/callback"))
                                .build());
                        observer.onCompleted();
                    }
                }).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        try {
            AgentCard card = card(name);
            var provider = new GrpcCompat03ClientTransportProvider();
            ClientTransport transport = provider.create(
                    new GrpcTransportConfigBuilder().channelFactory(ignored -> channel).build(), card,
                    card.supportedInterfaces().get(0));
            try {
                assertEquals("task-123", transport.getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams("task-123"), null).id());
                assertEquals("tasks/task-123/pushNotificationConfigs/task-123", receivedName.get());
                assertEquals("specific", transport.getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams("task-123", "specific"), null).id());
                assertEquals("tasks/task-123/pushNotificationConfigs/specific", receivedName.get());
            } finally {
                transport.close();
            }

            ClientCallInterceptor clearingInterceptor = new ClientCallInterceptor() {
                @Override
                public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                        AgentCard agentCard, ClientCallContext context) {
                    var request = (org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest) payload;
                    return new PayloadAndHeaders(request.toBuilder().clearId().build(), headers);
                }
            };
            ClientTransport intercepted = provider.create(new GrpcTransportConfigBuilder()
                    .channelFactory(ignored -> channel).addInterceptor(clearingInterceptor).build(), card,
                    card.supportedInterfaces().get(0));
            try {
                assertEquals("task-123", intercepted.getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams("task-123", "specific"), null).id());
                assertEquals("tasks/task-123/pushNotificationConfigs/task-123", receivedName.get());
            } finally {
                intercepted.close();
            }
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(server.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void providerTargetsLegacyGrpcBindingAndVersion() {
        GrpcCompat03ClientTransportProvider provider = new GrpcCompat03ClientTransportProvider();

        assertEquals(TransportProtocol.GRPC.asString(), provider.protocolBinding());
        assertEquals("0.3", provider.protocolVersion());
        assertEquals(GrpcTransport.class, provider.configuredTransportClass());
    }

    @Test
    void sendsThroughLegacyServiceAndDoesNotOwnCallerChannel() throws Exception {
        String name = InProcessServerBuilder.generateName();
        AtomicBoolean called = new AtomicBoolean();
        Server server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new A2AServiceGrpc.A2AServiceImplBase() {
                    @Override
                    public void sendMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                            StreamObserver<SendMessageResponse> responseObserver) {
                        called.set(true);
                        responseObserver.onNext(SendMessageResponse.newBuilder().setMsg(Message.newBuilder()
                                .setMessageId("response")
                                .setRole(Role.ROLE_AGENT)
                                .addContent(Part.newBuilder().setText("hello").build())
                                .build()).build());
                        responseObserver.onCompleted();
                    }
                }).build().start();
        ManagedChannel channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        AgentCard card = card(name);
        ClientTransport transport = new GrpcCompat03ClientTransportProvider().create(
                new GrpcTransportConfigBuilder().channelFactory(ignored -> channel).build(), card,
                card.supportedInterfaces().get(0));
        try {
            var result = transport.sendMessage(new MessageSendParams(
                    new org.a2aproject.sdk.spec.Message(org.a2aproject.sdk.spec.Message.Role.ROLE_USER,
                            List.of(new TextPart("hello")), "request", null, null, null, null, null),
                    null, null, null), null);
            assertEquals("response", ((org.a2aproject.sdk.spec.Message) result).messageId());
            assertFalse(channel.isShutdown());
            assertEquals(true, called.get());
        } finally {
            transport.close();
            channel.shutdownNow();
            server.shutdownNow();
        }
    }

    @Test
    void rejectsTenantOnLegacyAgentInterface() {
        AgentCard card = cardWithInterfaceTenant();

        org.a2aproject.sdk.spec.A2AClientException exception = assertThrows(org.a2aproject.sdk.spec.A2AClientException.class,
                () -> new GrpcCompat03ClientTransportProvider().create(null, card, card.supportedInterfaces().get(0)));

        assertTrue(exception.getMessage().contains("tenant"));
    }

    private static AgentCard card(String endpoint) {
        return AgentCard.builder()
                .name("legacy")
                .description("legacy")
                .version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill").tags(List.of()).build()))
                .url(endpoint)
                .preferredTransport(TransportProtocol.GRPC.asString())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.GRPC.asString(), endpoint, null, "0.3")))
                .build();
    }

    private static AgentCard cardWithInterfaceTenant() {
        return AgentCard.builder()
                .name("legacy")
                .description("legacy")
                .version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.GRPC.asString(), "in-process", "tenant", "0.3")))
                .build();
    }
}
