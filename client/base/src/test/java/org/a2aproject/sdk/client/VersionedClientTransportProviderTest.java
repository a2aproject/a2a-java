package org.a2aproject.sdk.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;

import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.junit.jupiter.api.Test;

class VersionedClientTransportProviderTest {
    @Test
    void selectsVersionedProviderForPatchFormAndUsesOrdinaryConfig() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://example.test", null, "0.3.0")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        assertEquals("0.3.0", builder.findBestClientTransport().protocolVersion());
        assertNotNull(builder.build());
    }

    @Test
    void rejectsUnknownProtocolVersionBeforeNativeFallback() {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://example.test", null, "0.2.9")))
                .build();

        assertThrows(A2AClientException.class, () -> Client.builder(card).findBestClientTransport());
    }

    @Test
    void preservesAgentCardOrderAcrossProtocolVersionsWithServerPreference() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://legacy.example", null, "0.3"),
                        new AgentInterface("GRPC", "http://native.example", null, "1.0")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> null));

        assertEquals("JSONRPC", builder.findBestClientTransport().protocolBinding());
    }

    @Test
    void selectsNativeInterfaceBeforeLegacyInterfaceWithClientPreference() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://legacy.example", null, "0.3"),
                        new AgentInterface("GRPC", "http://native.example", null, "1.0")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> null));

        assertEquals("GRPC", builder.findBestClientTransport().protocolBinding());
    }

    @Test
    void fallsBackToLegacyInterfaceWhenNoNativeInterfaceUsesAConfiguredTransport() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(
                        new AgentInterface("GRPC", "http://native.example", null, "1.0"),
                        new AgentInterface("JSONRPC", "http://legacy.example", null, "0.3")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        assertEquals("JSONRPC", builder.findBestClientTransport().protocolBinding());
        assertEquals("0.3", builder.findBestClientTransport().protocolVersion());
    }

    @Test
    void skipsLegacyInterfaceWhoseBindingAdapterIsNotInstalled() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(
                        new AgentInterface("GRPC", "http://grpc.example", null, "0.3"),
                        new AgentInterface("JSONRPC", "http://jsonrpc.example", null, "0.3")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> null))
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        assertEquals("JSONRPC", builder.findBestClientTransport().protocolBinding());
    }

    @Test
    void ignoresUnknownInterfaceVersionWhenACompatibleInterfaceExists() throws Exception {
        AgentCard card = AgentCard.builder()
                .name("agent").description("agent").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder().id("skill").name("skill").description("skill")
                        .tags(List.of("tag")).build()))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://future.example", null, "2.0"),
                        new AgentInterface("GRPC", "http://grpc.example", null, "1.0")))
                .build();

        ClientBuilder builder = Client.builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> null));

        assertEquals("GRPC", builder.findBestClientTransport().protocolBinding());
    }

    public static final class FakeVersionedProvider implements VersionedClientTransportProvider {
        @Override public String protocolBinding() { return "JSONRPC"; }
        @Override public String protocolVersion() { return "0.3"; }
        @Override public Class<? extends ClientTransport> configuredTransportClass() { return JSONRPCTransport.class; }
        @Override public ClientTransport create(ClientTransportConfig<?> config, AgentCard card,
                AgentInterface agentInterface) throws A2AClientException {
            return (ClientTransport) Proxy.newProxyInstance(ClientTransport.class.getClassLoader(),
                    new Class<?>[] {ClientTransport.class}, (proxy, method, args) -> null);
        }
    }
}
