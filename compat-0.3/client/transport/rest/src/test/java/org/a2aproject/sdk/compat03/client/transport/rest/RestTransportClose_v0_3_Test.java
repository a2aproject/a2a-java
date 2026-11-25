package org.a2aproject.sdk.compat03.client.transport.rest;

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
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link RestTransport_v0_3#close()} contract: closing the transport cancels every SSE
 * stream it still owns, keeps the normal send/resubscribe completion callbacks working and is safe
 * to call repeatedly or concurrently with stream setup.
 */
class RestTransportClose_v0_3_Test {

    private static final String INTERFACE = "http://localhost:4001";
    private static final AgentCard_v0_3 CARD = new AgentCard_v0_3.Builder().name("Test Agent").description("Test Agent")
            .version("1.0").url(INTERFACE)
            .capabilities(new AgentCapabilities_v0_3.Builder().streaming(true).build())
            .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();

    @Test
    void closeCancelsAllActiveStreamsAndIsIdempotent() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        sendStreaming(transport, event -> {});
        transport.resubscribe(new TaskIdParams_v0_3("task-1234"), event -> {}, error -> {}, null);

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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        sendStreaming(transport, event -> {});
        CountingFuture request = http.requests.get(0);
        request.complete(null);

        transport.close();

        assertEquals(0, request.cancellations, "Completed streams must be removed from the active requests");
    }

    @Test
    void normalStreamCompletionIsForwardedToTheListenerOnce() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
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
    void normalResubscriptionCompletionIsForwardedToTheListenerOnce() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        AtomicInteger completions = new AtomicInteger();
        AtomicReference<Throwable> terminal = new AtomicReference<>();
        transport.resubscribe(new TaskIdParams_v0_3("task-1234"), event -> {}, error -> {
            completions.incrementAndGet();
            terminal.set(error);
        }, null);

        CountingFuture request = http.requests.get(0);
        request.completion.run();

        assertEquals(1, completions.get(), "Normal resubscription completion must reach the listener exactly once");
        assertNull(terminal.get(), "Normal resubscription completion must not report an error");

        request.complete(null);
        transport.close();

        assertEquals(0, request.cancellations, "A resubscription completed before close must not be cancelled");
        assertFalse(request.isCancelled(), "The completed future must stay completed");
        assertEquals(1, completions.get(), "Close must not emit another completion callback");
    }

    @Test
    void closeDoesNotRetainStreamsCompletedDuringSetup() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        http.duringSetup = () -> http.requests.get(0).complete(null);

        sendStreaming(transport, event -> {});
        transport.close();

        assertEquals(0, http.requests.get(0).cancellations, "Synchronous completion must not leave a tracked stream");
    }

    @Test
    void closeDuringRequestSetupCancelsTheLateRegisteredStream() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
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
            String endpoint = "http://127.0.0.1:" + server.getLocalPort();
            RestTransport_v0_3 transport = new RestTransport_v0_3(new JdkA2AHttpClient(), CARD, endpoint, null);
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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        List<Throwable> subscribeTerminal = Collections.synchronizedList(new ArrayList<>());
        transport.resubscribe(new TaskIdParams_v0_3("task-1234"), event -> {}, subscribeTerminal::add, null);
        transport.resubscribe(new TaskIdParams_v0_3("task-5678"), event -> {}, subscribeTerminal::add, null);

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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
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
        RestTransport_v0_3 transport = new RestTransport_v0_3(http, CARD, INTERFACE, null);
        List<Throwable> terminal = Collections.synchronizedList(new ArrayList<>());
        http.duringSetup = () -> CompletableFuture.runAsync(transport::close).orTimeout(5, TimeUnit.SECONDS).join();

        sendStreaming(transport, event -> {}, terminal::add);

        assertEquals(1, terminal.size());
        assertNull(terminal.get(0));
        assertEquals(1, http.requests.size());
        assertTrue(http.requests.get(0).isCancelled());
    }

    private static void sendStreaming(RestTransport_v0_3 transport, Consumer<StreamingEventKind_v0_3> consumer)
            throws Exception {
        sendStreaming(transport, consumer, error -> {});
    }

    private static void sendStreaming(RestTransport_v0_3 transport, Consumer<StreamingEventKind_v0_3> consumer,
            Consumer<Throwable> terminalConsumer) throws Exception {
        Message_v0_3 message = new Message_v0_3.Builder().role(Message_v0_3.Role.USER).messageId("message-1234")
                .parts(List.of(new TextPart_v0_3("hello"))).build();
        transport.sendMessageStreaming(new MessageSendParams_v0_3.Builder().message(message).build(), consumer,
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
