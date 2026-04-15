package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.compat03.client.ClientBuilder;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.compat03.conversion.AbstractCompat03ServerTest;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.AfterAll;

@QuarkusTest
public class QuarkusA2AGrpcTest extends AbstractCompat03ServerTest {

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
        if (channel != null) {
            channel.shutdownNow();
            try {
                channel.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
