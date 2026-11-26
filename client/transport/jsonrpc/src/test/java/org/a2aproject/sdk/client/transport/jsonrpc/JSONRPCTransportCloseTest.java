package org.a2aproject.sdk.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
import java.util.function.Consumer;

import com.google.gson.JsonParser;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link JSONRPCTransport#close()} contract: closing the transport ends every SSE stream
 * it still owns, reports that as normal completion rather than as the cancellation error, and is
 * safe to call repeatedly or concurrently with stream setup.
 */
class JSONRPCTransportCloseTest {

    private static final AgentInterface INTERFACE = new AgentInterface("JSONRPC", "http://localhost:4001");

    @Test
    void closeEndsEveryActiveStreamOnceAndIsIdempotent() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport transport = new JSONRPCTransport(http, null, INTERFACE, null);
        List<Throwable> streamTerminals = terminals();
        List<Throwable> subscriptionTerminals = terminals();
        sendStreaming(transport, streamTerminals::add);
        transport.subscribeToTask(new TaskIdParams("task"), event -> {}, subscriptionTerminals::add, null);

        transport.close();
        transport.close();

        assertEquals(2, http.requests.size());
        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled(), "close() must cancel each active stream");
        }
        assertEquals(Collections.singletonList(null), streamTerminals,
                "close() must report normal completion once, not the cancellation error");
        assertEquals(Collections.singletonList(null), subscriptionTerminals);
    }

    @Test
    void closeDoesNotTouchCompletedStreams() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport transport = new JSONRPCTransport(http, null, INTERFACE, null);
        List<Throwable> terminals = terminals();
        sendStreaming(transport, terminals::add);
        CountingFuture request = http.requests.get(0);
        request.completion.run();
        request.complete(null);

        transport.close();

        assertEquals(0, request.cancellations, "Completed streams must be removed from the active streams");
        assertEquals(Collections.singletonList(null), terminals, "close() must not signal completion again");
    }

    @Test
    void closeDuringStreamSetupEndsTheLateStream() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport transport = new JSONRPCTransport(http, null, INTERFACE, null);
        http.duringSetup = () -> CompletableFuture.runAsync(transport::close).orTimeout(5, TimeUnit.SECONDS).join();
        List<Throwable> terminals = terminals();

        sendStreaming(transport, terminals::add);

        assertTrue(http.requests.get(0).isCancelled(), "close() must not miss a stream returned after it began");
        assertEquals(Collections.singletonList(null), terminals);
    }

    @Test
    void closeReleasesOpenJdkStreamAndReportsNormalCompletion() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0)) {
            Future<Boolean> connectionClosed = pool.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    int contentLength = 0;
                    for (String line; (line = reader.readLine()) != null && !line.isEmpty();) {
                        if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
                        }
                    }
                    StringBuilder body = new StringBuilder();
                    for (int i = 0; i < contentLength; i++) {
                        body.append((char) reader.read());
                    }
                    String id = JsonParser.parseString(body.toString()).getAsJsonObject().get("id").getAsString();
                    byte[] event = ("data: {\"jsonrpc\":\"2.0\",\"id\":\"" + id + "\",\"result\":{\"task\":{"
                            + "\"id\":\"task\",\"contextId\":\"ctx\",\"status\":{\"state\":\"TASK_STATE_WORKING\"}}}}\n\n")
                            .getBytes(StandardCharsets.UTF_8);
                    OutputStream output = socket.getOutputStream();
                    output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\n"
                            + "Transfer-Encoding: chunked\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    output.write((Integer.toHexString(event.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    output.write(event);
                    output.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                    output.flush();
                    // Keep the response open, as an agent that is still working would. EOF means the client let go.
                    try {
                        return reader.read() == -1;
                    } catch (SocketException e) {
                        return true;
                    } catch (SocketTimeoutException e) {
                        return false;
                    }
                }
            });
            JSONRPCTransport transport = new JSONRPCTransport("http://localhost:" + server.getLocalPort());
            CountDownLatch eventReceived = new CountDownLatch(1);
            List<Throwable> terminals = terminals();
            transport.subscribeToTask(new TaskIdParams("task"), event -> eventReceived.countDown(), terminals::add, null);
            assertTrue(eventReceived.await(10, TimeUnit.SECONDS), "stream should deliver the first event");

            transport.close();

            assertEquals(Collections.singletonList(null), terminals,
                    "close() should report normal completion once, not the cancellation error");
            assertTrue(connectionClosed.get(10, TimeUnit.SECONDS), "close() should release the HTTP connection");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void closeEndsEveryStreamEvenWhenHandlersThrow() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport transport = new JSONRPCTransport(http, null, INTERFACE, null);
        IllegalStateException first = new IllegalStateException("first handler");
        IllegalStateException second = new IllegalStateException("second handler");
        sendStreaming(transport, error -> {
            throw first;
        });
        sendStreaming(transport, error -> {
            throw second;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, transport::close);

        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled(), "A throwing handler must not leave a stream open");
        }
        assertEquals(1, thrown.getSuppressed().length, "Every handler failure must be reported");
    }

    @Test
    void closeEndsEveryStreamWhenHandlersThrowTheSameException() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport transport = new JSONRPCTransport(http, null, INTERFACE, null);
        IllegalStateException shared = new IllegalStateException("shared");
        for (int i = 0; i < 3; i++) {
            sendStreaming(transport, error -> {
                throw shared;
            });
        }

        assertSame(shared, assertThrows(IllegalStateException.class, transport::close));

        for (CountingFuture request : http.requests) {
            assertTrue(request.isCancelled(), "Rethrowing one exception instance must not leave a stream open");
        }
    }

    private static List<Throwable> terminals() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    private static void sendStreaming(JSONRPCTransport transport, Consumer<Throwable> terminalConsumer)
            throws Exception {
        Message message = Message.builder().role(Message.Role.ROLE_USER).messageId("message")
                .parts(List.of(new TextPart("hello"))).build();
        transport.sendMessageStreaming(MessageSendParams.builder().message(message).build(), event -> {},
                terminalConsumer, null);
    }

    /**
     * Reports the first cancellation through the error consumer, as the JDK HTTP client does with
     * {@code IOException("Request cancelled")}.
     */
    private static class CountingFuture extends CompletableFuture<Void> {
        private int cancellations;
        private Consumer<Throwable> errors = error -> {};
        private Runnable completion = () -> {};

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellations++;
            boolean firstCancellation = !isDone();
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (firstCancellation) {
                errors.accept(new IOException("Request cancelled"));
            }
            return cancelled;
        }
    }

    private static class PendingHttpClient implements A2AHttpClient {
        private final List<CountingFuture> requests = new ArrayList<>();
        private volatile Runnable duringSetup = () -> {};

        @Override
        public GetBuilder createGet() {
            throw new UnsupportedOperationException("This test client only streams POST requests");
        }

        @Override
        public PostBuilder createPost() {
            return new PostBuilder() {
                @Override
                public PostBuilder body(String body) {
                    return this;
                }

                @Override
                public PostBuilder url(String s) {
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
                public A2AHttpResponse post() {
                    throw new UnsupportedOperationException("This test client only streams POST requests");
                }

                @Override
                public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> messageConsumer,
                                                            Consumer<Throwable> errorConsumer,
                                                            Runnable completeRunnable) {
                    CountingFuture request = new CountingFuture();
                    request.errors = errorConsumer;
                    request.completion = completeRunnable;
                    requests.add(request);
                    duringSetup.run();
                    return request;
                }
            };
        }

        @Override
        public DeleteBuilder createDelete() {
            throw new UnsupportedOperationException("This test client only streams POST requests");
        }
    }
}
