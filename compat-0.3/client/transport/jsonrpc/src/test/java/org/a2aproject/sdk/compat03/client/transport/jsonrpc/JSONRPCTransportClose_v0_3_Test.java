package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Covers the {@link JSONRPCTransport_v0_3#close()} contract: closing the transport ends every SSE
 * stream it still owns, reports that as normal completion rather than as the cancellation error, and
 * is safe to call repeatedly or concurrently with stream setup.
 */
public class JSONRPCTransportClose_v0_3_Test {

    @Test
    public void nullStreamFutureIsIgnoredBeforeAndAfterClose() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        http.returnNull = true;
        JSONRPCTransport_v0_3 transport = transport(http);
        List<Throwable> terminals = terminals();
        sendStreaming(transport, terminals::add);
        transport.close();
        transport.resubscribe(new TaskIdParams_v0_3("task"), event -> {}, terminals::add, null);
        assertTrue(terminals.isEmpty());
    }

    @Test
    public void closeEndsEveryActiveStreamOnceAndIsIdempotent() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport_v0_3 transport = transport(http);
        List<Throwable> streamTerminals = terminals();
        List<Throwable> subscriptionTerminals = terminals();
        sendStreaming(transport, streamTerminals::add);
        transport.resubscribe(new TaskIdParams_v0_3("task"), event -> {}, subscriptionTerminals::add, null);

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
    public void closeDoesNotTouchCompletedStreams() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport_v0_3 transport = transport(http);
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
    public void closeDuringStreamSetupEndsTheLateStream() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport_v0_3 transport = transport(http);
        http.duringSetup = () -> CompletableFuture.runAsync(transport::close).orTimeout(5, TimeUnit.SECONDS).join();
        List<Throwable> terminals = terminals();

        sendStreaming(transport, terminals::add);

        assertTrue(http.requests.get(0).isCancelled(), "close() must not miss a stream returned after it began");
        assertEquals(Collections.singletonList(null), terminals);
    }

    @Test
    public void closeEndsEveryStreamEvenWhenHandlersThrow() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport_v0_3 transport = transport(http);
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
    public void closeEndsEveryStreamWhenHandlersThrowTheSameException() throws Exception {
        PendingHttpClient http = new PendingHttpClient();
        JSONRPCTransport_v0_3 transport = transport(http);
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

    private static JSONRPCTransport_v0_3 transport(A2AHttpClient httpClient) {
        return new JSONRPCTransport_v0_3(httpClient, null, "http://localhost:4001", List.of());
    }

    private static List<Throwable> terminals() {
        return Collections.synchronizedList(new ArrayList<>());
    }

    private static void sendStreaming(JSONRPCTransport_v0_3 transport, Consumer<Throwable> terminalConsumer)
            throws Exception {
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("hello")))
                .messageId("message")
                .build();
        transport.sendMessageStreaming(new MessageSendParams_v0_3.Builder().message(message).build(), event -> {},
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
        private boolean returnNull;
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
                    if (returnNull) {
                        return null;
                    }
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
