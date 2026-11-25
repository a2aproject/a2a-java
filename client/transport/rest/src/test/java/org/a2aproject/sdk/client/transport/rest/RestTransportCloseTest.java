package org.a2aproject.sdk.client.transport.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * Covers the {@link RestTransport#close()} contract: closing the transport cancels every SSE
 * stream it still owns, keeps normal stream completion working (as merged upstream) and is safe
 * to call repeatedly or concurrently with stream setup.
 */
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
        assertEquals(List.of(false, false), http.followRedirectCalls,
                "Streaming POST requests must keep redirect following disabled");
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
    void normalStreamCompletionIsForwardedToTheListenerOnce() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        AtomicInteger completions = new AtomicInteger();
        AtomicReference<Throwable> terminal = new AtomicReference<>();
        sendStreaming(transport, event -> {}, error -> {
            completions.incrementAndGet();
            terminal.set(error);
        });

        CountingFuture request = http.requests.get(0);
        request.completion.run();

        assertEquals(1, completions.get(), "Normal stream completion must reach the listener exactly once");
        assertNull(terminal.get(), "Normal stream completion must not report an error");

        request.complete(null);
        transport.close();

        assertEquals(0, request.cancellations, "A stream completed before close must not be cancelled");
        assertFalse(request.isCancelled(), "The completed future must stay completed");
        assertEquals(1, completions.get(), "Close must not emit another completion callback");
    }

    @Test
    void normalSubscriptionCompletionIsForwardedToTheListenerOnce() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        AtomicInteger completions = new AtomicInteger();
        AtomicReference<Throwable> terminal = new AtomicReference<>();
        transport.subscribeToTask(new TaskIdParams("task-1234"), event -> {}, error -> {
            completions.incrementAndGet();
            terminal.set(error);
        }, null);

        CountingFuture request = http.requests.get(0);
        request.completion.run();

        assertEquals(1, completions.get(), "Normal subscription completion must reach the listener exactly once");
        assertNull(terminal.get(), "Normal subscription completion must not report an error");

        request.complete(null);
        transport.close();

        assertEquals(0, request.cancellations, "A subscription completed before close must not be cancelled");
        assertFalse(request.isCancelled(), "The completed future must stay completed");
        assertEquals(1, completions.get(), "Close must not emit another completion callback");
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

    @Test
    void normalCompletionPrecedesCancellationError() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.reportCancellationErrors = true;
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        List<Throwable> sendTerminal = Collections.synchronizedList(new ArrayList<>());
        sendStreaming(transport, event -> {}, sendTerminal::add);
        sendStreaming(transport, event -> {}, sendTerminal::add);

        transport.close();
        transport.close();

        assertEquals(Collections.nCopies(2, null), sendTerminal);
        assertEquals(2, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled());
        }
    }

    @Test
    void normalSubscriptionCompletionPrecedesCancellationError() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.reportCancellationErrors = true;
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        List<Throwable> subscribeTerminal = Collections.synchronizedList(new ArrayList<>());
        transport.subscribeToTask(new TaskIdParams("task-1234"), event -> {}, subscribeTerminal::add, null);
        transport.subscribeToTask(new TaskIdParams("task-5678"), event -> {}, subscribeTerminal::add, null);

        transport.close();
        transport.close();

        assertEquals(Collections.nCopies(2, null), subscribeTerminal);
        assertEquals(2, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled());
        }
    }

    @Test
    void throwingHandlersCloseEveryStream() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.reportCancellationErrors = true;
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        sendStreaming(transport, event -> {}, error -> { throw first; });
        sendStreaming(transport, event -> {}, error -> { throw second; });

        RuntimeException thrown = null;
        try {
            transport.close();
        } catch (RuntimeException e) {
            thrown = e;
        }

        assertEquals(2, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled());
        }
        assertTrue(thrown == first || thrown == second);
        Throwable[] suppressed = thrown.getSuppressed();
        assertEquals(1, suppressed.length);
        Throwable other = thrown == first ? second : first;
        assertEquals(other, suppressed[0]);
    }

    @Test
    void sameExceptionInstanceIsNotSelfSuppressed() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.reportCancellationErrors = true;
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        RuntimeException shared = new RuntimeException("shared");
        sendStreaming(transport, event -> {}, error -> { throw shared; });
        sendStreaming(transport, event -> {}, error -> { throw shared; });
        sendStreaming(transport, event -> {}, error -> { throw shared; });

        RuntimeException thrown = null;
        try {
            transport.close();
        } catch (RuntimeException e) {
            thrown = e;
        }

        assertSame(shared, thrown);
        assertEquals(0, thrown.getSuppressed().length);
        assertEquals(3, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled());
        }
    }

    @Test
    void closeDuringSetupReportsNormalCompletion() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.reportCancellationErrors = true;
        RestTransport transport = new RestTransport(http, CARD, INTERFACE, null);
        List<Throwable> terminal = Collections.synchronizedList(new ArrayList<>());
        http.duringSetup = () -> CompletableFuture.runAsync(transport::close).orTimeout(5, TimeUnit.SECONDS).join();

        sendStreaming(transport, event -> {}, terminal::add);

        assertEquals(1, terminal.size());
        assertNull(terminal.get(0));
        assertEquals(1, http.requests.size());
        assertTrue(http.requests.get(0).isCancelled());
    }

    private static void sendStreaming(RestTransport transport, Consumer<StreamingEventKind> consumer)
            throws Exception {
        sendStreaming(transport, consumer, error -> {});
    }

    private static void sendStreaming(RestTransport transport, Consumer<StreamingEventKind> consumer,
            Consumer<Throwable> terminalConsumer) throws Exception {
        Message message = Message.builder().role(Message.Role.ROLE_USER).messageId("message-1234")
                .parts(List.of(new TextPart("hello"))).build();
        transport.sendMessageStreaming(MessageSendParams.builder().message(message).build(), consumer,
                terminalConsumer, null);
    }

    private static class CountingFuture extends CompletableFuture<Void> {
        private int cancellations;
        private Runnable completion = () -> {};
        private Consumer<Throwable> errors = error -> {};
        private boolean reportCancellationErrors;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellations++;
            boolean firstCancellation = !isDone();
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (firstCancellation && reportCancellationErrors) {
                errors.accept(new IOException("Request cancelled"));
            }
            return cancelled;
        }
    }

    private static class PendingHttpClient extends JdkA2AHttpClient {
        private final List<CountingFuture> requests = new ArrayList<>();
        private final List<Boolean> followRedirectCalls = new ArrayList<>();
        private volatile Runnable duringSetup = () -> {};
        private boolean reportCancellationErrors;

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
                public PostBuilder followRedirects(boolean follow) {
                    followRedirectCalls.add(follow);
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
                    request.completion = complete;
                    request.errors = errors;
                    request.reportCancellationErrors = reportCancellationErrors;
                    requests.add(request);
                    duringSetup.run();
                    return request;
                }
            };
        }
    }
}
