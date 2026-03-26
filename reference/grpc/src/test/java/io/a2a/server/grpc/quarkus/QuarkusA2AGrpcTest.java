package io.a2a.server.grpc.quarkus;

import java.util.concurrent.TimeUnit;

import io.a2a.client.ClientBuilder;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
public class QuarkusA2AGrpcTest extends AbstractA2AServerTest {

    private static ManagedChannel channel;

    public QuarkusA2AGrpcTest() {
        super(8081); // HTTP server port for utility endpoints
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        // gRPC server runs on port 8081, which is the same port as the main web server.
        // This is from the setting on the
        return "localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
            channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            return channel;
        }));
    }

    @AfterAll
    public static void closeChannel() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void testAgentCardHeaders() {
        // Skip - gRPC doesn't use HTTP caching headers for Agent Card
        // The A2A spec section 8.6 caching requirements apply only to HTTP endpoints
    }
}