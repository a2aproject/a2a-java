package org.a2aproject.sdk.client.transport.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

class RestTransportCloseTest {

    private static final AgentInterface INTERFACE = new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:4001");
    private static final AgentCard CARD = AgentCard.builder().name("Test Agent").description("Test Agent")
            .version("1.0").supportedInterfaces(List.of(INTERFACE))
            .capabilities(AgentCapabilities.builder().streaming(true).build())
            .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();

    @Test
    void closeCancelsAllActiveStreamsAndIsIdempotent() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        sendStreaming(transport, event -> {});
        transport.subscribeToTask(new TaskIdParams("task-1234"), event -> {}, error -> {}, null);

        transport.close();
        transport.close();

        assertEquals(2, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled(), "Closing REST must cancel each active HTTP stream");
            assertEquals(1, request.cancellations, "Repeated close must not cancel a request again");
        }
    }

    @Test
    void closeDoesNotCancelCompletedStreams() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        sendStreaming(transport, event -> {});
        CountingFuture request = http.requests.get(0);
        request.complete(null);

        transport.close();

        assertEquals(0, request.cancellations, "Completed streams must be removed from the active requests");
    }

    @Test
    void closeDoesNotRetainStreamsCompletedDuringSetup() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        http.duringSetup = () -> http.requests.get(0).complete(null);

        sendStreaming(transport, event -> {});
        transport.close();

        assertEquals(0, http.requests.get(0).cancellations, "Synchronous completion must not leave a tracked stream");
    }

    @Test
    void closeDuringRequestSetupCancelsTheLateRegisteredStream() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        http.duringSetup = () -> CompletableFuture.runAsync(transport::close).orTimeout(5, TimeUnit.SECONDS).join();

        sendStreaming(transport, event -> {});

        assertTrue(http.requests.get(0).isCancelled(), "Close must not lose a future returned after close began");
    }

    @Test
    void closeReleasesTheRealJdkHttpConnection() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Socket> accepted = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(10000);
            Future<Integer> connectionEnd = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    accepted.set(socket);
                    socket.setSoTimeout(10000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    int contentLength = 0;
                    for (String line; (line = input.readLine()) != null && !line.isEmpty();) {
                        if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                        }
                    }
                    for (int i = 0; i < contentLength; i++) {
                        if (input.read() == -1) {
                            throw new IOException("Request ended before its body");
                        }
                    }
                    String event = "data: {\"task\":{\"id\":\"task-1234\",\"contextId\":\"context-1234\","
                            + "\"status\":{\"state\":\"TASK_STATE_WORKING\"}}}\n\n";
                    byte[] data = event.getBytes(StandardCharsets.UTF_8);
                    String headers = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nTransfer-Encoding: chunked\r\n\r\n";
                    socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write((Integer.toHexString(data.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(data);
                    socket.getOutputStream().write("\r\n".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    // Keep the response open. EOF here proves the client released the upstream socket.
                    return input.read();
                }
            });
            AgentInterface endpoint = new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://127.0.0.1:" + server.getLocalPort());
            RestTransport transport = new RestTransport(new JdkA2AHttpClient(), CARD, endpoint, null);
            CountDownLatch received = new CountDownLatch(1);
            try {
                sendStreaming(transport, event -> received.countDown());
                assertTrue(received.await(5, TimeUnit.SECONDS), "The stream must be active before close");

                transport.close();

                assertEquals(-1, connectionEnd.get(5, TimeUnit.SECONDS), "Close must terminate the real HTTP connection");
            } finally {
                transport.close();
                Socket socket = accepted.get();
                if (socket != null) {
                    socket.close();
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void sendStreaming(RestTransport transport, Consumer<StreamingEventKind> consumer)
            throws Exception {
        Message message = Message.builder().role(Message.Role.ROLE_USER).messageId("message-1234")
                .parts(List.of(new TextPart("hello"))).build();
        transport.sendMessageStreaming(MessageSendParams.builder().message(message).build(), consumer, error -> {}, null);
    }

    private static class CountingFuture extends CompletableFuture<Void> {
        private int cancellations;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellations++;
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class PendingHttpClient extends JdkA2AHttpClient {
        private final List<CountingFuture> requests = new ArrayList<>();
        private Runnable duringSetup = () -> {};

        @Override
        public PostBuilder createPost() {
            return new A2AHttpClient.PostBuilder() {
                @Override
                public PostBuilder url(String url) {
                    return this;
                }
                @Override
                public PostBuilder addHeaders(Map<String, String> headers) {
                    return this;
                }
                @Override
                public PostBuilder addHeader(String name, String value) {
                    return this;
                }
                @Override
                public PostBuilder body(String body) {
                    return this;
                }
                @Override
                public A2AHttpResponse post() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> messages,
                        Consumer<Throwable> errors, Runnable complete) {
                    CountingFuture request = new CountingFuture();
                    requests.add(request);
                    duringSetup.run();
                    return request;
                }
            };
        }
    }
}
