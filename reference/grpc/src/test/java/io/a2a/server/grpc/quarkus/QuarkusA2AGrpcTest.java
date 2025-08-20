package io.a2a.server.grpc.quarkus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.a2a.client.config.ClientTransportConfig;
import io.a2a.client.transport.grpc.GrpcTransportConfig;
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
        return "localhost:9001"; // gRPC server runs on port 9001
    }

    @Override
    protected List<ClientTransportConfig> getClientTransportConfigs() {
        List<ClientTransportConfig> transportConfigs = new ArrayList<>();
        transportConfigs.add(new GrpcTransportConfig(
                target -> {
                    channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    return channel;
                }));
        return transportConfigs;
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
}