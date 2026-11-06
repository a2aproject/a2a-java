package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VertxA2AHttpClientLargeSSETest {

    private Vertx vertx;
    private HttpServer server;
    private VertxA2AHttpClient client;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        client = new VertxA2AHttpClient(vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void deliversFragmentedLargeEvents(boolean post, boolean multiline) throws Exception {
        String data = multiline ? String.join("\n", Collections.nCopies(32, "x".repeat(2048)))
                : "é".repeat(32768);
        String body = "data: " + data.replace("\n", "\ndata: ") + "\n\ndata: tail\n\n";

        StreamResult result = consume(body, post);

        assertEquals(List.of(data, "tail"), result.events().stream().map(ServerSentEvent::data).toList());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.completed());
    }

    @Test
    void rejectsOversizedRawLineAndRecovers() throws Exception {
        // The decoded line fits the character limit, but its UTF-8 encoding exceeds the byte limit.
        String oversized = "é".repeat(SSEParserConfig.DEFAULT.maxLineLength() / 2);
        String body = "id: safe\ndata: first\n\nid: corrupt\ndata: " + oversized
                + "\ndata: discarded\n\ndata: recovered\n\n";

        StreamResult result = consume(body, false);

        assertEquals(List.of("first", "recovered"), result.events().stream().map(ServerSentEvent::data).toList());
        assertEquals(List.of("safe", "safe"), result.events().stream().map(ServerSentEvent::id).toList());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0) instanceof IllegalArgumentException);
        assertTrue(result.completed());
    }

    @Test
    void rejectsTooManyDataLinesAndRecovers() throws Exception {
        String body = "data: x\n".repeat(SSEParserConfig.DEFAULT.maxBufferLines() + 1)
                + "\ndata: recovered\n\n";

        StreamResult result = consume(body, false);

        assertEquals(List.of("recovered"), result.events().stream().map(ServerSentEvent::data).toList());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0) instanceof IllegalStateException);
        assertTrue(result.completed());
    }

    @Test
    void rejectsOversizedEventAndRecovers() throws Exception {
        String line = "data: " + "x".repeat(600000) + "\n";

        StreamResult result = consume(line + line + "\ndata: recovered\n\n", true);

        assertEquals(List.of("recovered"), result.events().stream().map(ServerSentEvent::data).toList());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0) instanceof IllegalStateException);
        assertTrue(result.completed());
    }

    @Test
    void handlesSplitUtf8AndFlushesFinalLine() throws Exception {
        // Split a UTF-8 character across separate HTTP writes and finish without a newline.
        String body = "data: " + "x".repeat(8185) + "é\r\n\r\ndata: final";

        StreamResult result = consume(body, false);

        assertEquals(List.of("x".repeat(8185) + "é", "final"),
                result.events().stream().map(ServerSentEvent::data).toList());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.completed());
    }

    @Test
    void reportsOriginalEventConsumerFailure() throws Exception {
        String url = serve("data: first\n\n");
        RuntimeException failure = new IllegalStateException("Event consumer failed");
        List<Throwable> errors = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        client.createGet().url(url).getAsyncSSE(event -> {
            throw failure;
        }, errors::add, () -> completed.set(true)).get(5, TimeUnit.SECONDS);

        assertEquals(1, errors.size());
        assertSame(failure, errors.get(0));
        assertFalse(completed.get());
    }

    @Test
    void reportsDisconnectedStreamWithoutFlushingPartialEvent() throws Exception {
        server.requestHandler(request -> request.response().setChunked(true)
                .putHeader("Content-Type", "text/event-stream")
                .write("data: incomplete\n")
                .onSuccess(ignored -> request.connection().close()));
        server.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        client.createGet().url("http://127.0.0.1:" + server.actualPort() + "/stream")
                .getAsyncSSE(events::add, errors::add, () -> completed.set(true)).get(5, TimeUnit.SECONDS);

        assertTrue(events.isEmpty());
        assertEquals(1, errors.size());
        assertFalse(completed.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void completesFutureWhenTerminalCallbackThrows(boolean streamFailure) throws Exception {
        RuntimeException failure = new IllegalStateException("Terminal callback failed");
        CompletableFuture<Throwable> uncaught = new CompletableFuture<>();
        vertx.exceptionHandler(uncaught::complete);
        server.requestHandler(request -> {
            var response = request.response().setChunked(true).putHeader("Content-Type", "text/event-stream");
            if (streamFailure) {
                response.write("data: incomplete\n").onSuccess(ignored -> request.connection().close());
            } else {
                response.end("data: complete\n\n");
            }
        });
        server.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        List<Throwable> errors = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        client.createGet().url("http://127.0.0.1:" + server.actualPort() + "/stream")
                .getAsyncSSE(event -> {}, error -> {
                    errors.add(error);
                    throw failure;
                }, () -> {
                    completed.set(true);
                    throw failure;
                }).get(5, TimeUnit.SECONDS);

        assertSame(failure, uncaught.get(5, TimeUnit.SECONDS));
        assertEquals(streamFailure ? 1 : 0, errors.size());
        assertEquals(!streamFailure, completed.get());
    }

    private String serve(String body) throws Exception {
        Buffer bytes = Buffer.buffer(body);
        server.requestHandler(request -> {
            request.bodyHandler(ignored -> {
                var response = request.response().setChunked(true).putHeader("Content-Type", "text/event-stream");
                // An incomplete event exceeding the old codec's 4 KB watermark must not stop reads.
                int split = Math.min(8192, bytes.length());
                response.write(bytes.getBuffer(0, split));
                vertx.setTimer(25, ignoredTimer -> response.end(bytes.getBuffer(split, bytes.length())));
            });
        });
        server.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
        return "http://127.0.0.1:" + server.actualPort() + "/stream";
    }

    private StreamResult consume(String body, boolean post) throws Exception {
        String url = serve(body);
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();
        CompletableFuture<Void> future = post
                ? client.createPost().url(url).body("{}").postAsyncSSE(events::add, errors::add, () -> completed.set(true))
                : client.createGet().url(url).getAsyncSSE(events::add, errors::add, () -> completed.set(true));
        future.get(5, TimeUnit.SECONDS);
        return new StreamResult(events, errors, completed.get());
    }

    private record StreamResult(List<ServerSentEvent> events, List<Throwable> errors, boolean completed) {}
}
