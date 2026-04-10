package org.a2aproject.sdk.client;

import java.util.Collections;
import java.util.List;

import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientBuilderTest {

    private static AgentCard buildCard(List<AgentInterface> interfaces) {
        return AgentCard.builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(Collections.singletonList("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .build()))
                .supportedInterfaces(interfaces)
                .build();
    }

    private final AgentCard card = buildCard(List.of(
            new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")));

    private final AgentCard cardWithTenant = buildCard(List.of(
            new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999", "/default-tenant")));

    private final AgentCard cardWithMultipleInterfaces = buildCard(List.of(
            new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:9998", "/grpc-tenant", "1.0"),
            new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999", "/jsonrpc-tenant", "1.0")));

    @Test
    public void shouldNotFindCompatibleTransport() throws A2AClientException {
        A2AClientException exception = Assertions.assertThrows(A2AClientException.class,
                () -> Client
                        .builder(card)
                        .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                        .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                                .channelFactory(s -> null))
                        .build());

        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().contains("No compatible transport found"));
    }

    @Test
    public void shouldNotFindConfigurationTransport() throws A2AClientException {
        A2AClientException exception = Assertions.assertThrows(A2AClientException.class,
                () -> Client
                        .builder(card)
                        .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                        .build());

        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().startsWith("Missing required TransportConfig for"));
    }

    @Test
    public void shouldCreateJSONRPCClient() throws A2AClientException {
        Client client = Client
                .builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                        .addInterceptor(null)
                        .httpClient(null))
                .build();

        Assertions.assertNotNull(client);
    }

    @Test
    public void shouldCreateClient_differentConfigurations() throws A2AClientException {
        Client client = Client
                .builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(A2AHttpClientFactory.create()))
                .build();

        Assertions.assertNotNull(client);
    }

    @Test
    public void shouldPreserveTenantFromAgentInterface() throws A2AClientException {
        ClientBuilder builder = Client
                .builder(cardWithTenant)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        AgentInterface selectedInterface = builder.findBestClientTransport();

        Assertions.assertEquals("/default-tenant", selectedInterface.tenant());
        Assertions.assertEquals("http://localhost:9999", selectedInterface.url());
        Assertions.assertEquals(TransportProtocol.JSONRPC.asString(), selectedInterface.protocolBinding());
    }

    @Test
    public void shouldPreserveProtocolVersionFromAgentInterface() throws A2AClientException {
        ClientBuilder builder = Client
                .builder(cardWithMultipleInterfaces)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        AgentInterface selectedInterface = builder.findBestClientTransport();

        Assertions.assertEquals("/jsonrpc-tenant", selectedInterface.tenant());
        Assertions.assertEquals("1.0", selectedInterface.protocolVersion());
    }

    @Test
    public void shouldSelectCorrectInterfaceWithServerPreference() throws A2AClientException {
        // Server preference (default): iterates server interfaces in order, picks first that client supports
        // cardWithMultipleInterfaces has [GRPC, JSONRPC] - GRPC is first
        // Client supports both GRPC and JSONRPC, so GRPC should be selected (server's first choice)
        ClientBuilder builder = Client
                .builder(cardWithMultipleInterfaces)
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(s -> null))
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        AgentInterface selectedInterface = builder.findBestClientTransport();

        Assertions.assertEquals(TransportProtocol.GRPC.asString(), selectedInterface.protocolBinding());
        Assertions.assertEquals("http://localhost:9998", selectedInterface.url());
        Assertions.assertEquals("/grpc-tenant", selectedInterface.tenant());
    }

    @Test
    public void shouldSelectCorrectInterfaceWithClientPreference() throws A2AClientException {
        // Client preference: iterates client transports in registration order, picks first that server supports
        // Client registers [JSONRPC, GRPC] - JSONRPC is first
        // Server supports both, so JSONRPC should be selected (client's first choice)
        ClientBuilder builder = Client
                .builder(cardWithMultipleInterfaces)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(s -> null));

        AgentInterface selectedInterface = builder.findBestClientTransport();

        Assertions.assertEquals(TransportProtocol.JSONRPC.asString(), selectedInterface.protocolBinding());
        Assertions.assertEquals("http://localhost:9999", selectedInterface.url());
        Assertions.assertEquals("/jsonrpc-tenant", selectedInterface.tenant());
    }

    @Test
    public void shouldPreserveEmptyTenant() throws A2AClientException {
        ClientBuilder builder = Client
                .builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());

        AgentInterface selectedInterface = builder.findBestClientTransport();

        Assertions.assertEquals("", selectedInterface.tenant());
    }
}
