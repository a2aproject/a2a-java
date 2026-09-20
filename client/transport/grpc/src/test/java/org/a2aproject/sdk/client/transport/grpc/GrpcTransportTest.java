package org.a2aproject.sdk.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.a2aproject.sdk.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class GrpcTransportTest {

    private static final String TASK_ID = "task-1";
    private static final String CALLBACK_URL = "https://example.com/callback";
    private static final AgentCard CARD = AgentCard.builder()
            .name("Test Agent")
            .description("Agent for gRPC transport tests")
            .version("1.0.0")
            .supportedInterfaces(List.of(new AgentInterface("GRPC", "http://localhost")))
            .capabilities(AgentCapabilities.builder().pushNotifications(true).build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .build();

    private final AtomicReference<GetTaskPushNotificationConfigRequest> receivedRequest = new AtomicReference<>();
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new A2AServiceGrpc.A2AServiceImplBase() {
                    @Override
                    public void getTaskPushNotificationConfig(GetTaskPushNotificationConfigRequest request,
                            StreamObserver<org.a2aproject.sdk.grpc.TaskPushNotificationConfig> responseObserver) {
                        receivedRequest.set(request);
                        responseObserver.onNext(org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder()
                                .setTaskId(request.getTaskId())
                                .setId(request.getId().isEmpty() ? request.getTaskId() : request.getId())
                                .setUrl(CALLBACK_URL)
                                .build());
                        responseObserver.onCompleted();
                    }
                })
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testGetPushNotificationConfigWithOmittedId() throws Exception {
        assertGetPushNotificationConfig(new GetTaskPushNotificationConfigParams(TASK_ID), null, "", "");
    }

    @Test
    void testGetPushNotificationConfigWithOmittedIdViaBuilder() throws Exception {
        assertGetPushNotificationConfig(GetTaskPushNotificationConfigParams.builder().taskId(TASK_ID).build(),
                null, "", "");
    }

    @Test
    void testGetPushNotificationConfigWithEmptyId() throws Exception {
        assertGetPushNotificationConfig(new GetTaskPushNotificationConfigParams(TASK_ID, ""), null, "", "");
    }

    @Test
    void testGetPushNotificationConfigWithExplicitIdAndDefaultTenant() throws Exception {
        assertGetPushNotificationConfig(new GetTaskPushNotificationConfigParams(TASK_ID, "config-1"),
                "default-tenant", "config-1", "default-tenant");
    }

    @Test
    void testGetPushNotificationConfigWithOmittedIdAndRequestTenant() throws Exception {
        assertGetPushNotificationConfig(new GetTaskPushNotificationConfigParams(TASK_ID, null, "request-tenant"),
                "default-tenant", "", "request-tenant");
    }

    private void assertGetPushNotificationConfig(GetTaskPushNotificationConfigParams params,
            @Nullable String defaultTenant, String expectedId, String expectedTenant) throws Exception {
        GrpcTransport transport = new GrpcTransport(channel, CARD, defaultTenant, null);
        TaskPushNotificationConfig result = transport.getTaskPushNotificationConfiguration(params, null);

        GetTaskPushNotificationConfigRequest request = receivedRequest.get();
        assertNotNull(request);
        assertEquals(TASK_ID, request.getTaskId());
        assertEquals(expectedId, request.getId());
        assertEquals(expectedTenant, request.getTenant());
        assertEquals(expectedId.isEmpty() ? TASK_ID : expectedId, result.id());
        assertEquals(TASK_ID, result.taskId());
        assertEquals(CALLBACK_URL, result.url());
    }
}
