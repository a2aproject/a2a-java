package org.a2aproject.sdk.tests.multiversion.grpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithTaskAuthorizationTest;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
@TestProfile(TaskAuthorizationTestProfile.class)
public class MultiVersionGrpcWithTaskAuthorizationTest extends AbstractA2AServerWithTaskAuthorizationTest {

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "localhost:8081";
    }

    @Override
    protected AgentCard fetchAgentCardFromServer() {
        return agentCard;
    }

    @Override
    protected void configureTransportWithCredentials(ClientBuilder builder, String username, String password) {
        AuthInterceptor authInterceptor = new AuthInterceptor(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName)
                        ? getEncodedCredentials(username, password) : null);
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
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
