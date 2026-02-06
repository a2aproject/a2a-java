package io.a2a.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;
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

/**
 * Tests for handling HTTP 401 (Unauthorized) and 403 (Forbidden) responses
 * when the client sends streaming and non-streaming messages.
 * 
 * These tests verify that the client properly fails when the server returns
 * authentication or authorization errors.
 */
public class AuthenticationAuthorizationTest {

    private static final String AGENT_URL = "http://localhost:4001";
    private static final String AUTHENTICATION_FAILED_MESSAGE = "Authentication failed";
    private static final String AUTHORIZATION_FAILED_MESSAGE = "Authorization failed";

    private ClientAndServer server;
    private Message MESSAGE;
    private AgentCard agentCard;
    private Server grpcServer;
    private ManagedChannel grpcChannel;
    private String grpcServerName;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
        MESSAGE = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("test message")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        
        grpcServerName = InProcessServerBuilder.generateName();

        agentCard = AgentCard.builder()
                .name("Test Agent")
                .description("Test agent for auth tests")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)  // Support streaming for all tests
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                        .id("test_skill")
                        .name("Test skill")
                        .description("Test skill")
                        .tags(Collections.singletonList("test"))
                        .build()))
                .supportedInterfaces(java.util.Arrays.asList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), AGENT_URL),
                        new AgentInterface(TransportProtocol.GRPC.asString(), grpcServerName)))
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
    public void testJsonRpcNonStreamingUnauthenticated() throws A2AClientException {
        // Mock server to return 401 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        Client client = getJSONRPCClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testJsonRpcNonStreamingUnauthorized() throws A2AClientException {
        // Mock server to return 403 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        Client client = getJSONRPCClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
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
    public void testRestNonStreamingUnauthenticated() throws A2AClientException {
        // Mock server to return 401 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/message:send")
        ).respond(
                response()
                        .withStatusCode(401)
        );

        Client client = getRestClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testRestNonStreamingUnauthorized() throws A2AClientException {
        // Mock server to return 403 for non-streaming message
        server.when(
                request()
                        .withMethod("POST")
                        .withPath("/message:send")
        ).respond(
                response()
                        .withStatusCode(403)
        );

        Client client = getRestClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
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
                        .withPath("/message:stream")
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
                        .withPath("/message:stream")
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

        Client client = getGrpcClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
            client.sendMessage(MESSAGE);
        });

        assertTrue(exception.getMessage().contains(AUTHENTICATION_FAILED_MESSAGE));
    }

    @Test
    public void testGrpcNonStreamingUnauthorized() throws Exception {
        setupGrpcServer(Status.PERMISSION_DENIED);

        Client client = getGrpcClientBuilder(false).build();

        A2AClientException exception = assertThrows(A2AClientException.class, () -> {
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

    private ClientBuilder getJSONRPCClientBuilder(boolean streaming) {
        return Client.builder(agentCard)
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());
    }

    private ClientBuilder getRestClientBuilder(boolean streaming) {
        return Client.builder(agentCard)
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).build())
                .withTransport(RestTransport.class, new RestTransportConfigBuilder());
    }

    private ClientBuilder getGrpcClientBuilder(boolean streaming) {
        return Client.builder(agentCard)
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).build())
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
                        .channelFactory(target -> grpcChannel));
    }

    private void assertStreamingError(ClientBuilder clientBuilder, String expectedErrorMessage) throws Exception {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch errorLatch = new CountDownLatch(1);

        Consumer<Throwable> errorHandler = error -> {
            errorRef.set(error);
            errorLatch.countDown();
        };

        Client client = clientBuilder.streamingErrorHandler(errorHandler).build();

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