package io.a2a.client;


import java.util.Collections;
import java.util.List;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientBuilderTest {

    private AgentCard card = AgentCard.builder()
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
            .supportedInterfaces(List.of(
                    new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
            .build();

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
}
