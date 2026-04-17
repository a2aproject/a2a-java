package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.client.config.ClientConfig;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.AgentInterface;
import org.a2aproject.sdk.compat03.spec.AgentSkill;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class ClientBuilderTest {

    private AgentCard card = new AgentCard.Builder()
            .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .skills(Collections.singletonList(new AgentSkill.Builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
            .examples(List.of("hi", "hello world"))
            .build()))
            .protocolVersion("0.3.0")
                .additionalInterfaces(List.of(
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
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(new JdkA2AHttpClient()))
                .build();

        Assertions.assertNotNull(client);
    }
}
