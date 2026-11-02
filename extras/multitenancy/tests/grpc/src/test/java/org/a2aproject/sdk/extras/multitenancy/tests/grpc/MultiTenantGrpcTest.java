package org.a2aproject.sdk.extras.multitenancy.tests.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.extras.multitenancy.tests.AbstractMultiTenantServerTest;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
public class MultiTenantGrpcTest extends AbstractMultiTenantServerTest {

    // Two clients (streaming + non-streaming) are built per test instance, each creating its own
    // channel; accumulate them all here so @AfterAll can close every one, not just the last.
    private static final List<ManagedChannel> channels = new CopyOnWriteArrayList<>();

    public MultiTenantGrpcTest() {
        super(TEST_PORT);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "localhost:" + TEST_PORT;
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(
                GrpcTransport.class,
                new GrpcTransportConfigBuilder()
                        .channelFactory(
                                target -> {
                                    ManagedChannel channel =
                                            ManagedChannelBuilder.forTarget(target)
                                                    .usePlaintext()
                                                    .build();
                                    channels.add(channel);
                                    return channel;
                                }));
    }

    @AfterAll
    public static void closeChannels() {
        for (ManagedChannel channel : channels) {
            channel.shutdownNow();
            try {
                channel.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        channels.clear();
    }

    // gRPC-only deployments do not serve the well-known public-card endpoints.
    @Override
    public void publicCardWithoutTenantReturnsDefault() {
        // no-op: not served by gRPC
    }

    @Override
    public void publicCardWithAcmeTenant() {
        // no-op: not served by gRPC
    }

    @Override
    public void publicCardWithBetaTenant() {
        // no-op: not served by gRPC
    }

    @Override
    public void publicCardUnknownTenantReturns404() {
        // no-op: not served by gRPC
    }
}
