package org.a2aproject.sdk.examples.streamlifecycle.server;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.examples.streamlifecycle.client.StreamLifecycleClient;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
class StreamLifecycleTest {

    private static final String HTTP_URL = "http://localhost:8081";
    private static final String GRPC_URL = "localhost:8081";

    @AfterEach
    void cleanupGrpcChannels() {
        StreamLifecycleClient.shutdownGrpcChannels();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testStreamLifecycleWithJsonRpc() throws Exception {
        StreamLifecycleClient.runAndVerify(buildCard(TransportProtocol.JSONRPC, HTTP_URL), "JSONRPC");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testStreamLifecycleWithRest() throws Exception {
        StreamLifecycleClient.runAndVerify(buildCard(TransportProtocol.HTTP_JSON, HTTP_URL), "HTTP+JSON");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testStreamLifecycleWithGrpc() throws Exception {
        StreamLifecycleClient.runAndVerify(buildCard(TransportProtocol.GRPC, GRPC_URL), "GRPC");
    }

    private AgentCard buildCard(TransportProtocol protocol, String url) {
        return AgentCard.builder()
                .name("test")
                .description("test")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(protocol.asString(), url)))
                .capabilities(AgentCapabilities.builder().streaming(true).build())
                .build();
    }
}
