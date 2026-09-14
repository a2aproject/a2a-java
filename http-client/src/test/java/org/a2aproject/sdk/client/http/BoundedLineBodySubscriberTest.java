package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the bounded line-splitting body subscriber inside {@link JdkA2AHttpClient}.
 * Since the subscriber is a private inner class, we instantiate it via reflection to test
 * its byte-level line splitting and length enforcement in isolation.
 */
public class BoundedLineBodySubscriberTest {

    @SuppressWarnings("unchecked")
    private static HttpResponse.BodySubscriber<Void> createSubscriber(
            Flow.Subscriber<String> lineSubscriber, int maxLineBytes,
            ServerSentEventParser sseParser) throws Exception {
        Class<?> clazz = Class.forName(
                "org.a2aproject.sdk.client.http.JdkA2AHttpClient$BoundedLineBodySubscriber");
        Constructor<?> ctor = clazz.getDeclaredConstructor(
                Flow.Subscriber.class, int.class, ServerSentEventParser.class);
        ctor.setAccessible(true);
        return (HttpResponse.BodySubscriber<Void>) ctor.newInstance(lineSubscriber, maxLineBytes, sseParser);
    }

    private static List<ByteBuffer> toBuffers(String data) {
        return List.of(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static void subscribe(HttpResponse.BodySubscriber<Void> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) { /* no-op in tests */ }

            @Override
            public void cancel() { /* no-op in tests */ }
        });
    }

    private static final class RecordingSubscriber implements Flow.Subscriber<String> {
        final List<String> lines = new ArrayList<>();
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String item) {
            lines.add(item);
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
        }

        @Override
        public void onComplete() {
            completed.set(true);
        }
    }

    private static ServerSentEventParser createParser(List<Throwable> errors) {
        return new ServerSentEventParser(
                event -> { /* discard events */ },
                errors::add,
                SSEParserConfig.DEFAULT);
    }

    @Test
    public void testBasicLFLineSplitting() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hello\nworld\n"));
        subscriber.onComplete();

        assertEquals(List.of("hello", "world"), rec.lines);
        assertTrue(rec.completed.get());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testCRLFLineSplitting() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hello\r\nworld\r\n"));
        subscriber.onComplete();

        assertEquals(List.of("hello", "world"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testBareCRLineSplitting() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hello\rworld\r"));
        subscriber.onComplete();

        assertEquals(List.of("hello", "world"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testMixedLineTerminators() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("LF\nCRLF\r\nCR\rend"));
        subscriber.onComplete();

        assertEquals(List.of("LF", "CRLF", "CR", "end"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testCRLFSplitAcrossChunks() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hello\r"));
        subscriber.onNext(toBuffers("\nworld\n"));
        subscriber.onComplete();

        assertEquals(List.of("hello", "world"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testBareCRAtChunkBoundaryFollowedByNonLF() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hello\r"));
        subscriber.onNext(toBuffers("world\n"));
        subscriber.onComplete();

        assertEquals(List.of("hello", "world"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testLineSplitAcrossMultipleChunks() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("hel"));
        subscriber.onNext(toBuffers("lo wo"));
        subscriber.onNext(toBuffers("rld\n"));
        subscriber.onComplete();

        assertEquals(List.of("hello world"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testStreamEndWithoutTrailingTerminator() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("no-newline"));
        subscriber.onComplete();

        assertEquals(List.of("no-newline"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testEmptyLines() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("\n\n\n"));
        subscriber.onComplete();

        assertEquals(List.of("", "", ""), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testLineTooLongCallsParser() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 10, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("short\n" + "x".repeat(20) + "\nok\n"));
        subscriber.onComplete();

        assertEquals(List.of("short", "ok"), rec.lines,
                "Too-long line should not appear in line subscriber output");
        assertEquals(1, errors.size(), "One error should be reported for too-long line");
        assertTrue(errors.get(0).getMessage().contains("maximum length"));
    }

    @Test
    public void testLineTooLongAcrossChunks() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 10, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("12345"));
        subscriber.onNext(toBuffers("67890AB\n"));
        subscriber.onComplete();

        assertTrue(rec.lines.isEmpty(), "Too-long line should not appear");
        assertEquals(1, errors.size());
    }

    @Test
    public void testLineTooLongAtStreamEnd() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 10, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("data: ok\n" + "x".repeat(20)));
        subscriber.onComplete();

        assertEquals(List.of("data: ok"), rec.lines);
        assertEquals(1, errors.size(),
                "Too-long line at stream end must trigger processLineTooLong");
    }

    @Test
    public void testLineExactlyAtLimit() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 10, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("1234567890\n"));
        subscriber.onComplete();

        assertEquals(1, rec.lines.size());
        assertEquals("1234567890", rec.lines.get(0), "Line exactly at limit should be accepted");
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testLineOneOverLimit() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 10, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("12345678901\n"));
        subscriber.onComplete();

        assertTrue(rec.lines.isEmpty(), "Line over limit should not appear");
        assertEquals(1, errors.size());
    }

    @Test
    public void testMaxLineBytesZeroDisablesLimit() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        String longLine = "x".repeat(10_000);
        subscriber.onNext(toBuffers(longLine + "\n"));
        subscriber.onComplete();

        assertEquals(1, rec.lines.size());
        assertEquals(longLine, rec.lines.get(0));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testUtf8MultiByteCharacters() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        subscriber.onNext(toBuffers("héllo wörld café\n"));
        subscriber.onComplete();

        assertEquals(1, rec.lines.size());
        assertEquals("héllo wörld café", rec.lines.get(0));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testMultipleBuffersInSingleOnNext() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        List<ByteBuffer> buffers = List.of(
                ByteBuffer.wrap("first\n".getBytes(StandardCharsets.UTF_8)),
                ByteBuffer.wrap("second\n".getBytes(StandardCharsets.UTF_8)));
        subscriber.onNext(buffers);
        subscriber.onComplete();

        assertEquals(List.of("first", "second"), rec.lines);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testErrorPropagation() throws Exception {
        RecordingSubscriber rec = new RecordingSubscriber();
        List<Throwable> errors = new ArrayList<>();
        var subscriber = createSubscriber(rec, 0, createParser(errors));
        subscribe(subscriber);

        RuntimeException expected = new RuntimeException("test error");
        subscriber.onError(expected);

        assertEquals(expected, rec.error.get());
    }
}
