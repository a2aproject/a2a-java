package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.buffer.Buffer;
import java.util.ArrayList;
import java.util.List;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VertxSseParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r", "\r\n"})
    void parsesEventsAcrossEveryBufferBoundary(String newline) {
        Buffer body = Buffer.buffer(String.join(newline, ": comment", "id: first", "event: update", "retry: 0",
                "data: hé🙂", "data: world", "", "data: tail", "", ""));
        List<ServerSentEvent> expected = List.of(
                new ServerSentEvent("hé🙂\nworld", "update", "first", 0L),
                new ServerSentEvent("tail", "message", "first", 0L));

        for (int split = 0; split <= body.length(); split++) {
            List<ServerSentEvent> events = new ArrayList<>();
            List<Throwable> errors = new ArrayList<>();
            VertxSseParser parser = new VertxSseParser(events::add, errors::add);
            parser.handle(body.getBuffer(0, split));
            parser.handle(Buffer.buffer());
            parser.handle(body.getBuffer(split, body.length()));
            parser.end();

            assertEquals(expected, events, "Split at byte " + split);
            assertTrue(errors.isEmpty(), "Split at byte " + split);
        }
    }

    @Test
    void acceptsLineAtDefaultByteLimit() {
        String data = "x".repeat(SSEParserConfig.DEFAULT.maxLineLength() - 6);
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        VertxSseParser parser = new VertxSseParser(events::add, errors::add);

        parser.handle(Buffer.buffer("data: " + data + "\n\n"));
        parser.end();

        assertEquals(List.of(new ServerSentEvent(data)), events);
        assertTrue(errors.isEmpty());
    }

    @Test
    void rejectsUnterminatedOversizedLineAtEndOfStream() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        VertxSseParser parser = new VertxSseParser(events::add, errors::add);
        parser.handle(Buffer.buffer("data: previously buffered\n"));
        Buffer chunk = Buffer.buffer("x".repeat(8192));
        for (int i = 0; i <= SSEParserConfig.DEFAULT.maxLineLength() / chunk.length(); i++) {
            parser.handle(chunk);
        }
        parser.end();

        assertTrue(events.isEmpty(), "The corrupt event must not be flushed");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof IllegalArgumentException);
    }
}
