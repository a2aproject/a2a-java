package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.compat03.server.grpc.quarkus.CompatibilityAuthTestProfile_v0_3;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityWithAuthTest_v0_3;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
@TestProfile(CompatibilityAuthTestProfile_v0_3.class)
public class QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest
        extends AbstractA2AServerCompatibilityWithAuthTest_v0_3 {
    private static ManagedChannel authenticatedChannel;
    private static ManagedChannel unauthenticatedChannel;

    public QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest() { super(8081); }
    @Override protected String getTransportProtocol() { return TransportProtocol.GRPC.asString(); }
    @Override protected String getTransportUrl() { return "localhost:8081"; }
    @Override protected AgentCard getAgentCard() {
        return QuarkusA2AGrpc_v0_3_CompatibilityTest.card(true);
    }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
            unauthenticatedChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            return unauthenticatedChannel;
        }));
    }
    @Override protected void configureTransportWithAuth(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                .channelFactory(target -> {
                    authenticatedChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    return authenticatedChannel;
                }).addInterceptor(new AuthInterceptor(
                        (scheme, context) -> BASIC_AUTH_SCHEME_NAME.equals(scheme) ? getEncodedCredentials() : null)));
    }

    @AfterAll
    public static void closeChannels() {
        close(authenticatedChannel);
        close(unauthenticatedChannel);
    }

    private static void close(ManagedChannel channel) {
        if (channel != null) {
            channel.shutdownNow();
            try { channel.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
