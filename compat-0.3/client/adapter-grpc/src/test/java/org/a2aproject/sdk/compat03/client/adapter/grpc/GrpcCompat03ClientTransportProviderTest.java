package org.a2aproject.sdk.compat03.client.adapter.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.compat03.grpc.Message;
import org.a2aproject.sdk.compat03.grpc.Part;
import org.a2aproject.sdk.compat03.grpc.Role;
import org.a2aproject.sdk.compat03.grpc.SendMessageResponse;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.Test;

class GrpcCompat03ClientTransportProviderTest {
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
}
