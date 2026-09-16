package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityTest_v0_3;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.SecurityRequirement;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;

@QuarkusTest
@TestProfile(CompatibilityTestProfile_v0_3.class)
public class QuarkusA2AGrpc_v0_3_CompatibilityTest extends AbstractA2AServerCompatibilityTest_v0_3 {
    private static ManagedChannel channel;

    public QuarkusA2AGrpc_v0_3_CompatibilityTest() { super(8081); }
    @Override protected String getTransportProtocol() { return TransportProtocol.GRPC.asString(); }
    @Override protected String getTransportUrl() { return "localhost:8081"; }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
            channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            return channel;
        }));
    }
    @Override protected AgentCard getAgentCard() { return card(false); }

    static AgentCard card(boolean auth) {
        AgentCard.Builder builder = AgentCard.builder().name("legacy").description("legacy")
                .url("localhost:8081").version("1.0.0").capabilities(AgentCapabilities.builder().streaming(true)
                        .pushNotifications(true).build()).defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text")).skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface("GRPC", "localhost:8081", null, "0.3")));
        if (auth) {
            builder.securitySchemes(Map.of("basicAuth",
                    new org.a2aproject.sdk.spec.HTTPAuthSecurityScheme("none", "basic", "HTTP Basic authentication")))
                    .securityRequirements(List.of(new SecurityRequirement(Map.of("basicAuth", List.of()))));
        }
        return builder.build();
    }

    @AfterAll
    public static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
            try { channel.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
