package org.a2aproject.sdk.tests.multitenancy.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import jakarta.inject.Inject;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for multi-tenant executor routing on a gRPC server.
 * Verifies that gRPC SendMessage requests with a {@code tenant} field are routed
 * to the correct {@code AgentExecutor} bean qualified with {@code @Tenant}.
 */
@QuarkusTest
public class MultiTenantGrpcTest {

    @Inject
    AuthorizationController authorizationController;

    private static ManagedChannel channel;
    private static String grpcTarget;

    @BeforeAll
    public static void setupChannel() {
        int port = ConfigProvider.getConfig().getValue("quarkus.http.port", Integer.class);
        grpcTarget = "localhost:" + port;
        channel = ManagedChannelBuilder.forTarget(grpcTarget).usePlaintext().build();
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

    @Test
    public void authorizationControllerIsTestAlternative() {
        assertInstanceOf(TestAuthorizationController.class, authorizationController,
                "TestAuthorizationController CDI alternative must be active");
        assertFalse(authorizationController.isAuthorizationEnabled(),
                "Authorization must be disabled for multitenancy tests");
    }

    @Test
    public void knownTenantRoutesToAcmeExecutor() throws Exception {
        assertEquals("acme", sendMessageAndGetArtifactText("acme"));
    }

    @Test
    public void secondTenantRoutesToBetaExecutor() throws Exception {
        assertEquals("beta", sendMessageAndGetArtifactText("beta"));
    }

    @Test
    public void unknownTenantFallsBackToDefault() throws Exception {
        assertEquals("default", sendMessageAndGetArtifactText("unknown-corp"));
    }

    @Test
    public void nullTenantUsesDefault() throws Exception {
        assertEquals("default", sendMessageAndGetArtifactText(null));
    }

    private String sendMessageAndGetArtifactText(@Nullable String tenant) throws Exception {
        AgentCard card = buildGrpcAgentCard();
        MessageSendParams params = buildParams(tenant);

        List<Task> tasks = new ArrayList<>();
        List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, agentCard) -> {
            if (event instanceof TaskEvent te) {
                tasks.add(te.getTask());
            }
        });
        try (Client client = Client.builder(card)
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                        .channelFactory(target -> channel))
                .build()) {
            client.sendMessage(params, consumers, null, null);
        }

        assertEquals(1, tasks.size(), "Expected exactly one Task in the response");
        Task task = tasks.get(0);
        assertNotNull(task.artifacts(), "Expected artifacts in task");
        assertFalse(task.artifacts().isEmpty(), "Expected at least one artifact");
        return ((TextPart) task.artifacts().get(0).parts().get(0)).text();
    }

    private static MessageSendParams buildParams(@Nullable String tenant) {
        Message message = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("hello")))
                .build();
        MessageSendParams.Builder builder = MessageSendParams.builder().message(message);
        if (tenant != null) {
            builder.tenant(tenant);
        }
        return builder.build();
    }

    private static AgentCard buildGrpcAgentCard() {
        return AgentCard.builder()
                .name("test-agent")
                .description("test")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().streaming(false).build())
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.GRPC.asString(), grpcTarget)))
                .build();
    }
}
