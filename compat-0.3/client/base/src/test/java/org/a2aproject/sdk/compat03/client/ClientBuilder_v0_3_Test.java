package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfig_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class ClientBuilder_v0_3_Test {

    private AgentCard_v0_3 card = new AgentCard_v0_3.Builder()
            .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
            .defaultInputModes(Collections.singletonList("text"))
            .defaultOutputModes(Collections.singletonList("text"))
            .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
            .examples(List.of("hi", "hello world"))
            .build()))
            .protocolVersion("0.3.0")
                .additionalInterfaces(List.of(
            new AgentInterface_v0_3(TransportProtocol_v0_3.JSONRPC.asString(), "http://localhost:9999")))
            .build();

    @Test
    public void shouldNotFindCompatibleTransport() throws A2AClientException_v0_3 {
        A2AClientException_v0_3 exception = Assertions.assertThrows(A2AClientException_v0_3.class,
                () -> Client_v0_3
                        .builder(card)
                        .clientConfig(new ClientConfig_v0_3.Builder().setUseClientPreference(true).build())
                        .withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3()
                                .channelFactory(s -> null))
                        .build());

        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().contains("No compatible transport found"));
    }

    @Test
    public void shouldNotFindConfigurationTransport() throws A2AClientException_v0_3 {
        A2AClientException_v0_3 exception = Assertions.assertThrows(A2AClientException_v0_3.class,
                () -> Client_v0_3
                        .builder(card)
                        .clientConfig(new ClientConfig_v0_3.Builder().setUseClientPreference(true).build())
                        .build());

        Assertions.assertTrue(exception.getMessage() != null && exception.getMessage().startsWith("Missing required TransportConfig for"));
    }

    @Test
    public void shouldCreateJSONRPCClient() throws A2AClientException_v0_3 {
        Client_v0_3 client = Client_v0_3
                .builder(card)
                .clientConfig(new ClientConfig_v0_3.Builder().setUseClientPreference(true).build())
                .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3()
                        .addInterceptor(null)
                        .httpClient(null))
                .build();

        Assertions.assertNotNull(client);
    }

    @Test
    public void shouldCreateClient_differentConfigurations() throws A2AClientException_v0_3 {
        Client_v0_3 client = Client_v0_3
                .builder(card)
                .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3())
                .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfig_v0_3(new JdkA2AHttpClient_v0_3()))
                .build();

        Assertions.assertNotNull(client);
    }
}
