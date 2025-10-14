package io.a2a.client.http.vertx;

import io.a2a.client.Client;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * The purpose of this one is to make sure that the Vertx http implementation can be integrated into
 * the Client builder when creating a new instance of the Client.
 */
public class ClientBuilderTest {

    private final AgentCard card = new AgentCard.Builder()
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
    public void shouldCreateJSONRPCClient() throws A2AClientException {
        Client client = Client
                .builder(card)
                .clientConfig(new ClientConfig.Builder().build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                        .httpClientBuilder(new VertxHttpClientBuilder().vertx(Vertx.vertx())))
                .build();

        Assertions.assertNotNull(client);
    }
}
