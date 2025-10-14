package io.a2a.client;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.http.jdk.JdkHttpClientBuilder;
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
    public void shouldNotCreateJSONRPCClient_nullHttpClientFactory() throws A2AClientException {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    Client
                            .builder(card)
                            .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                                    .addInterceptor(null)
                                    .httpClientBuilder(null))
                            .build();
                });
    }

    @Test
    public void shouldCreateJSONRPCClient_defaultHttpClientFactory() throws A2AClientException {
        Client client = Client
                .builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                        .addInterceptor(null)
                        .httpClientBuilder(HttpClientBuilder.DEFAULT_FACTORY))
                .build();

        Assertions.assertNotNull(client);
    }

    @Test
    public void shouldCreateJSONRPCClient_withHttpClientFactory() throws A2AClientException {
        Client client = Client
                .builder(card)
                .clientConfig(new ClientConfig.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                        .addInterceptor(null)
                        .httpClientBuilder(new JdkHttpClientBuilder()))
                .build();

        Assertions.assertNotNull(client);
    }

    @Test
    public void shouldCreateClient_differentConfigurations() throws A2AClientException {
        Client client = Client
                .builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .build();

        Assertions.assertNotNull(client);
    }
}
