package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.AfterAll;

@QuarkusTest
public class QuarkusA2AGrpc_v0_3_Test extends AbstractA2AServerServerTest_v0_3 {

    private static ManagedChannel channel;

    public QuarkusA2AGrpc_v0_3_Test() {
        super(8081); // HTTP server port for utility endpoints
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        // gRPC server runs on port 8081, which is the same port as the main web server.
        return "localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3().channelFactory(target -> {
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
