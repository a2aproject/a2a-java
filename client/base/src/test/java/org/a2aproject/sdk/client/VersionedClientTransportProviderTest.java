package org.a2aproject.sdk.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;

import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
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
