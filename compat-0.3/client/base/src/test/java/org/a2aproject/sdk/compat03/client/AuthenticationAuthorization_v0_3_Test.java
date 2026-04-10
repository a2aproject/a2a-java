package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc;
import org.a2aproject.sdk.compat03.grpc.SendMessageRequest;
import org.a2aproject.sdk.compat03.grpc.SendMessageResponse;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Tests for handling HTTP 401 (Unauthorized) and 403 (Forbidden) responses
 * when the client sends streaming and non-streaming messages.
 * 
 * These tests verify that the client properly fails when the server returns
 * authentication or authorization errors.
 */
public class AuthenticationAuthorization_v0_3_Test {

    private static final String AGENT_URL = "http://localhost:4001";
    private static final String AUTHENTICATION_FAILED_MESSAGE = "Authentication failed";
    private static final String AUTHORIZATION_FAILED_MESSAGE = "Authorization failed";

    private ClientAndServer server;
    private Message_v0_3 MESSAGE;
    private AgentCard_v0_3 agentCard;
    private Server grpcServer;
    private ManagedChannel grpcChannel;
    private String grpcServerName;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
        MESSAGE = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("test message")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        
        grpcServerName = InProcessServerBuilder.generateName();

        agentCard = new AgentCard_v0_3.Builder()
                .name("Test Agent")
                .description("Test agent for auth tests")
                .url(AGENT_URL)
                .version("1.0.0")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)  // Support streaming for all tests
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                        .id("test_skill")
                        .name("Test skill")
                        .description("Test skill")
                        .tags(Collections.singletonList("test"))
                        .build()))
                .protocolVersion("0.3.0")
                .additionalInterfaces(java.util.Arrays.asList(
                        new AgentInterface_v0_3(TransportProtocol_v0_3.JSONRPC.asString(), AGENT_URL),
                        new AgentInterface_v0_3(TransportProtocol_v0_3.HTTP_JSON.asString(), AGENT_URL),
                        new AgentInterface_v0_3(TransportProtocol_v0_3.GRPC.asString(), grpcServerName)))
                .build();
    }

    @AfterEach
    public void tearDown() {
        server.stop();
        if (grpcChannel != null) {
            grpcChannel.shutdownNow();
        }
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
    }

    // ========== JSON-RPC Transport Tests ==========

    @Test
    public void testJsonRpcNonStreamingUnauthenticated() throws A2AClientException_v0_3 {
        // Mock server to return 401 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        Client_v0_3 client = getJSONRPCClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testJsonRpcNonStreamingUnauthorized() throws A2AClientException_v0_3 {
        // Mock server to return 403 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        Client_v0_3 client = getJSONRPCClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHORIZATION_FAILED_MESSAGE));
    }

    @Test
    public void testJsonRpcStreamingUnauthenticated() throws Exception {
        // Mock server to return 401 for streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        assertStreamingError(
                getJSONRPCClientBuilder(true),
                AUTHENTICATION_FAILED_MESSAGE);
    }

    @Test
    public void testJsonRpcStreamingUnauthorized() throws Exception {
        // Mock server to return 403 for streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        assertStreamingError(
                getJSONRPCClientBuilder(true),
                AUTHORIZATION_FAILED_MESSAGE);
    }

    // ========== REST Transport Tests ==========

    @Test
    public void testRestNonStreamingUnauthenticated() throws A2AClientException_v0_3 {
        // Mock server to return 401 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:send")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        Client_v0_3 client = getRestClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testRestNonStreamingUnauthorized() throws A2AClientException_v0_3 {
        // Mock server to return 403 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:send")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        Client_v0_3 client = getRestClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHORIZATION_FAILED_MESSAGE));
    }

    @Test
    public void testRestStreamingUnauthenticated() throws Exception {
        // Mock server to return 401 for streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:stream")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        assertStreamingError(
                getRestClientBuilder(true),
                AUTHENTICATION_FAILED_MESSAGE);
    }

    @Test
    public void testRestStreamingUnauthorized() throws Exception {
        // Mock server to return 403 for streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:stream")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        assertStreamingError(
                getRestClientBuilder(true),
                AUTHORIZATION_FAILED_MESSAGE);
    }

    // ========== gRPC Transport Tests ==========

    @Test
    public void testGrpcNonStreamingUnauthenticated() throws Exception {
        setupGrpcServer(Status.UNAUTHENTICATED);

        Client_v0_3 client = getGrpcClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testGrpcNonStreamingUnauthorized() throws Exception {
        setupGrpcServer(Status.PERMISSION_DENIED);

        Client_v0_3 client = getGrpcClientBuilder(false).build();

        A2AClientException_v0_3 exception = assertThrows(A2AClientException_v0_3.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHORIZATION_FAILED_MESSAGE));
    }

    @Test
    public void testGrpcStreamingUnauthenticated() throws Exception {
        setupGrpcServer(Status.UNAUTHENTICATED);

        assertStreamingError(
                getGrpcClientBuilder(true),
                AUTHENTICATION_FAILED_MESSAGE);
    }

    @Test
    public void testGrpcStreamingUnauthorized() throws Exception {
        setupGrpcServer(Status.PERMISSION_DENIED);

        assertStreamingError(
                getGrpcClientBuilder(true),
                AUTHORIZATION_FAILED_MESSAGE);
    }

    private ClientBuilder_v0_3 getJSONRPCClientBuilder(boolean streaming) {
        return Client_v0_3.builder(agentCard)
                .clientConfig(new ClientConfig_v0_3.Builder().setStreaming(streaming).build())
                .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3());
    }

    private ClientBuilder_v0_3 getRestClientBuilder(boolean streaming) {
        return Client_v0_3.builder(agentCard)
                .clientConfig(new ClientConfig_v0_3.Builder().setStreaming(streaming).build())
                .withTransport(RestTransport_v0_3.class, new RestTransportConfigBuilder_v0_3());
    }

    private ClientBuilder_v0_3 getGrpcClientBuilder(boolean streaming) {
        return Client_v0_3.builder(agentCard)
                .clientConfig(new ClientConfig_v0_3.Builder().setStreaming(streaming).build())
                .withTransport(GrpcTransport_v0_3.class, new GrpcTransportConfigBuilder_v0_3()
                        .channelFactory(target -> grpcChannel));
    }

    private void assertStreamingError(ClientBuilder_v0_3 clientBuilder, String expectedErrorMessage) throws Exception {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        Consumer<Throwable> errorHandler = error -> {
            errorRef.set(error);
            errorLatch.countDown();
        };

        Client_v0_3 client = clientBuilder.streamingErrorHandler(errorHandler).build();

        try {
            client.sendMessage(MESSAGE);
            // If no immediate exception, wait for async error
            assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Expected error handler to be called");
            Throwable error = errorRef.get();
            assertTrue(error.getMessage().contains(expectedErrorMessage),
                      "Expected error message to contain '" + expectedErrorMessage + "' but got: " + error.getMessage());
        } catch (Exception e) {
            // Immediate exception is also acceptable
            assertTrue(e.getMessage().contains(expectedErrorMessage),
                      "Expected error message to contain '" + expectedErrorMessage + "' but got: " + e.getMessage());
        }
    }

    private void setupGrpcServer(Status status) throws IOException {
        grpcServerName = InProcessServerBuilder.generateName();
        grpcServer = InProcessServerBuilder.forName(grpcServerName)
                .directExecutor()
                .addService(new A2AServiceGrpc.A2AServiceImplBase() {
                    @Override
                    public void sendMessage(SendMessageRequest request, StreamObserver<SendMessageResponse> responseObserver) {
                        responseObserver.onError(status.asRuntimeException());
                    }

                    @Override
                    public void sendStreamingMessage(SendMessageRequest request, StreamObserver<StreamResponse> responseObserver) {
                        responseObserver.onError(status.asRuntimeException());
                    }
                })
                .build()
                .start();

        grpcChannel = InProcessChannelBuilder.forName(grpcServerName)
                .directExecutor()
                .build();
    }
}