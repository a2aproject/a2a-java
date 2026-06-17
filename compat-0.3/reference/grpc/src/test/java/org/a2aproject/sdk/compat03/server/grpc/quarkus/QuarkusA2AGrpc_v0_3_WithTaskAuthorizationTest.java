package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerWithTaskAuthorizationTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.TaskAuthorizationTestProfile_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
@TestProfile(TaskAuthorizationTestProfile_v0_3.class)
public class QuarkusA2AGrpc_v0_3_WithTaskAuthorizationTest extends AbstractA2AServerWithTaskAuthorizationTest_v0_3 {

    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public QuarkusA2AGrpc_v0_3_WithTaskAuthorizationTest() {
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
    protected void configureTransportWithCredentials(ClientBuilder_v0_3 builder, String username, String password) {
        AuthInterceptor_v0_3 authInterceptor = new AuthInterceptor_v0_3(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName)
                        ? getEncodedCredentials(username, password) : null);
        builder.withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3()
                .channelFactory(target -> {
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    channels.put(username, channel);
                    return channel;
                })
                .addInterceptor(authInterceptor));
    }

    @AfterAll
    static void closeChannels() {
        channels.values().forEach(ch -> {
            ch.shutdownNow();
            try {
                ch.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
