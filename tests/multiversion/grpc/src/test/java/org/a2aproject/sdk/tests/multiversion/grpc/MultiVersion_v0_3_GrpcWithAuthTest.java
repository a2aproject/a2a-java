package org.a2aproject.sdk.tests.multiversion.grpc;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerWithAuthTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class MultiVersion_v0_3_GrpcWithAuthTest extends AbstractA2AServerWithAuthTest_v0_3 {

    private static ManagedChannel authenticatedChannel;
    private static ManagedChannel unauthenticatedChannel;

    public MultiVersion_v0_3_GrpcWithAuthTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "localhost:8081";
    }

    @Override
    protected void configureTransportWithAuth(ClientBuilder_v0_3 builder) {
        AuthInterceptor_v0_3 authInterceptor = new AuthInterceptor_v0_3(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName) ? getEncodedCredentials() : null
        );

        builder.withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3()
                .channelFactory(target -> {
                    authenticatedChannel = ManagedChannelBuilder
                            .forTarget(target)
                            .usePlaintext()
                            .build();
                    return authenticatedChannel;
                })
                .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3().channelFactory(target -> {
            unauthenticatedChannel = ManagedChannelBuilder
                    .forTarget(target)
                    .usePlaintext()
                    .build();
            return unauthenticatedChannel;
        }));
    }

    @AfterAll
    public static void closeChannels() {
        if (authenticatedChannel != null) {
            authenticatedChannel.shutdownNow();
            try {
                authenticatedChannel.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (unauthenticatedChannel != null) {
            unauthenticatedChannel.shutdownNow();
            try {
                unauthenticatedChannel.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @Override
    @Disabled
    public void testGetAgentCardIsPublic() {
        // Skip - gRPC doesn't have /.well-known/agent-card.json endpoint
    }

    @Test
    @Override
    @Disabled
    public void testBasicAuthWorksViaHttp() {
        // Skip - HTTP-specific test
    }
}
