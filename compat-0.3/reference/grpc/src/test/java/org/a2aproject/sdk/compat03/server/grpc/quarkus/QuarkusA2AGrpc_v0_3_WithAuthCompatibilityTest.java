package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityWithAuthTest_v0_3;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
@TestProfile(CompatibilityAuthTestProfile_v0_3.class)
public class QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest
        extends AbstractA2AServerCompatibilityWithAuthTest_v0_3 {
    private static final List<ManagedChannel> channels = new CopyOnWriteArrayList<>();

    public QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest() { super(8081); }
    @Override protected String getTransportProtocol() { return TransportProtocol.GRPC.asString(); }
    @Override protected String getTransportUrl() { return "localhost:8081"; }
    @Override protected AgentCard getAgentCard() {
        return QuarkusA2AGrpc_v0_3_CompatibilityTest.card(true);
    }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
            ManagedChannel created = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            channels.add(created);
            return created;
        }));
    }
    @Override protected void configureTransportWithAuth(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                .channelFactory(target -> {
                    ManagedChannel created = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    channels.add(created);
                    return created;
                }).addInterceptor(new AuthInterceptor(
                        (scheme, context) -> BASIC_AUTH_SCHEME_NAME.equals(scheme) ? getEncodedCredentials() : null)));
    }

    @AfterAll
    public static void closeChannels() {
        // Shut down every caller-owned channel before waiting for any one of them.
        channels.forEach(ManagedChannel::shutdownNow);
        try {
            for (ManagedChannel channel : channels) {
                channel.awaitTermination(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            channels.clear();
        }
    }
}
