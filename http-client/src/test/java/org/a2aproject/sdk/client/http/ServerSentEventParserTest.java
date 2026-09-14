package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class ServerSentEventParserTest {

    @Test
    public void testSimpleDataEvent() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: Hello World");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Hello World", events.get(0).data());
        assertEquals("message", events.get(0).eventType());
        assertNull(events.get(0).id());
        assertNull(events.get(0).retry());
    }

    @Test
    public void testMultiLineDataEvent() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: First line");
        parser.processLine("data: Second line");
        parser.processLine("data: Third line");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("First line\nSecond line\nThird line", events.get(0).data());
    }

    @Test
    public void testEventWithType() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("event: custom");
        parser.processLine("data: Custom event data");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Custom event data", events.get(0).data());
        assertEquals("custom", events.get(0).eventType());
    }

    @Test
    public void testEventWithId() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("id: 123");
        parser.processLine("data: Event with ID");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Event with ID", events.get(0).data());
        assertEquals("123", events.get(0).id());
        assertEquals("123", parser.getLastEventId());
    }

    @Test
    public void testEventWithRetry() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("retry: 5000");
        parser.processLine("data: Event with retry");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Event with retry", events.get(0).data());
        assertEquals(5000L, events.get(0).retry());
        assertEquals(5000L, parser.getRetry());
    }

    @Test
    public void testCompleteEvent() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("event: notification");
        parser.processLine("id: msg-001");
        parser.processLine("retry: 3000");
        parser.processLine("data: Complete event");
        parser.processLine("");

        assertEquals(1, events.size());
        ServerSentEvent event = events.get(0);
        assertEquals("Complete event", event.data());
        assertEquals("notification", event.eventType());
        assertEquals("msg-001", event.id());
        assertEquals(3000L, event.retry());
    }

    @Test
    public void testMultipleEvents() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        // First event
        parser.processLine("event: type1");
        parser.processLine("data: First");
        parser.processLine("");

        // Second event
        parser.processLine("event: type2");
        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("First", events.get(0).data());
        assertEquals("type1", events.get(0).eventType());
        assertEquals("Second", events.get(1).data());
        assertEquals("type2", events.get(1).eventType());
    }

    @Test
    public void testEmptyIdClearsLastEventId() {
        // Per WHATWG SSE spec: id: with an empty value sets lastEventId to "" (clears it).
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("id: initial");
        parser.processLine("data: First");
        parser.processLine("");

        parser.processLine("id:");
        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("initial", events.get(0).id());
        assertEquals("", events.get(1).id(), "Empty id: should set currentEventId to empty string");
        assertEquals("", parser.getLastEventId(), "Empty id: should clear lastEventId to empty string");
    }

    @Test
    public void testInvalidRetryIsIgnored() {
        // Per SSE spec: non-digit retry values are silently ignored; the event is still dispatched.
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        assertDoesNotThrow(() -> parser.processLine("retry: not-a-number"));
        assertDoesNotThrow(() -> parser.processLine("retry: +100"));
        assertDoesNotThrow(() -> parser.processLine("retry: -1"));
        assertDoesNotThrow(() -> parser.processLine("retry: 1.5"));
        parser.processLine("data: Test");
        parser.processLine("");

        assertEquals(1, events.size());
        assertNull(events.get(0).retry(), "Retry should remain null after invalid values");
    }

    @Test
    public void testCommentLinesIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine(": This is a comment");
        parser.processLine("data: Real data");
        parser.processLine(": Another comment");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Real data", events.get(0).data());
    }

    @Test
    public void testDataPrefixStripping() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: with space");
        parser.processLine("");
        parser.processLine("data:no space");
        parser.processLine("");
        parser.processLine("data:  extra spaces  ");
        parser.processLine("");

        assertEquals(3, events.size());
        assertEquals("with space", events.get(0).data());
        assertEquals("no space", events.get(1).data());
        // SSE spec: remove only the first space after colon, preserve the rest
        assertEquals(" extra spaces  ", events.get(2).data());
    }

    @Test
    public void testEmptyDataFieldIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data:");
        parser.processLine("");

        assertEquals(0, events.size(), "Empty data field should not dispatch event");
    }

    @Test
    public void testMultipleEmptyLinesIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: first");
        parser.processLine("");
        parser.processLine("");
        parser.processLine("");
        parser.processLine("data: second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("first", events.get(0).data());
        assertEquals("second", events.get(1).data());
    }

    @Test
    public void testFieldWithoutColon() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data");
        parser.processLine("");

        assertEquals(0, events.size(), "Field without value should result in empty data");
    }

    @Test
    public void testFlush() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: Unflushed");
        assertEquals(0, events.size(), "Event should not be dispatched yet");

        parser.flush();
        assertEquals(1, events.size(), "Flush should dispatch buffered event");
        assertEquals("Unflushed", events.get(0).data());
    }

    @Test
    public void testNullLineIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine(null);
        parser.processLine("data: Valid");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Valid", events.get(0).data());
    }

    @Test
    public void testEventTypeResetBetweenEvents() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("event: custom");
        parser.processLine("data: First");
        parser.processLine("");

        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("custom", events.get(0).eventType());
        assertEquals("message", events.get(1).eventType(), "Event type should reset to 'message' after dispatch");
    }

    @Test
    public void testIdPersistsAcrossEvents() {
        // Per SSE spec, the "last event ID buffer" is never reset between events;
        // it persists until explicitly changed by another id: field.
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("id: 100");
        parser.processLine("data: First");
        parser.processLine("");

        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("100", events.get(0).id());
        assertEquals("100", events.get(1).id(), "ID should carry over to subsequent events per SSE spec");
        assertEquals("100", parser.getLastEventId(), "lastEventId should persist");
    }

    @Test
    public void testIdWithNullCharacterIsIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        // id containing U+0000 must be ignored per SSE spec
        parser.processLine("id: before");
        parser.processLine("data: First");
        parser.processLine("");

        parser.processLine("id: invalid id");
        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("before", events.get(0).id());
        // The null-containing id is ignored; currentEventId stays "before" (it persists)
        assertEquals("before", events.get(1).id());
        // lastEventId should still be "before" since the null id was discarded
        assertEquals("before", parser.getLastEventId());
    }

    @Test
    public void testRetryPersistsAcrossEvents() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("retry: 2000");
        parser.processLine("data: First");
        parser.processLine("");

        parser.processLine("data: Second");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals(2000L, events.get(0).retry());
        assertEquals(2000L, events.get(1).retry());
        assertEquals(2000L, parser.getRetry(), "Retry should persist");
    }

    @Test
    public void testUnknownFieldIgnored() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("unknown: field");
        parser.processLine("data: Valid");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("Valid", events.get(0).data());
    }

    // --- errorConsumer tests ---

    @Test
    public void testErrorConsumerCalledForNullLine() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        parser.processLine(null);

        assertNotNull(error.get(), "errorConsumer should be called for null line");
        assertEquals(IllegalArgumentException.class, error.get().getClass());
        assertEquals(0, events.size(), "No events should be dispatched");
    }

    @Test
    public void testErrorConsumerCalledForLineTooLong() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        SSEParserConfig config = SSEParserConfig.builder().maxLineLength(1000).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set, config);

        // Oversized line mid-event: the whole event block is discarded
        parser.processLine("data: before overflow");
        String longLine = "data: " + "x".repeat(1001);
        parser.processLine(longLine);
        // Subsequent lines in the same block are skipped
        parser.processLine("data: should be skipped");
        parser.processLine(""); // end of corrupted block — nothing dispatched

        assertNotNull(error.get(), "errorConsumer should be called for oversized line");
        assertEquals(IllegalArgumentException.class, error.get().getClass());
        assertNotNull(error.get().getMessage());
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");

        // Parser recovers cleanly at the next event boundary
        parser.processLine("data: recovered");
        parser.processLine("");
        assertEquals(1, events.size(), "Parser should recover after oversized line");
        assertEquals("recovered", events.get(0).data());
    }

    @Test
    public void testErrorConsumerCalledForBufferOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        for (int i = 0; i < 1000; i++) {
            parser.processLine("data: line" + i);
        }
        assertNull(error.get(), "No error expected before limit");

        parser.processLine("data: overflow");
        assertNotNull(error.get(), "errorConsumer should be called when buffer limit exceeded");
        assertEquals(IllegalStateException.class, error.get().getClass());

        // Lines in the same event block after the overflow are skipped
        parser.processLine("data: skipped in same block");
        parser.processLine(""); // end of corrupted block — nothing dispatched
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");

        // Parser recovers cleanly at the next event boundary
        parser.processLine("data: recovered");
        parser.processLine("");
        assertEquals(1, events.size(), "Parser should recover after buffer overflow");
        assertEquals("recovered", events.get(0).data());
    }

    @Test
    public void testErrorConsumerCalledForBufferByteOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        // Each value is 65530 chars; 17 such lines (17 * 65530 = 1,114,010 chars) exceed the 1 MB buffer char limit.
        String bigValue = "x".repeat(65530);
        for (int i = 0; i < 17; i++) {
            parser.processLine("data: " + bigValue);
        }

        assertNotNull(error.get(), "errorConsumer should be called when byte limit exceeded");
        assertEquals(IllegalStateException.class, error.get().getClass());

        // Lines in the same event block after the overflow are skipped
        parser.processLine("data: skipped in same block");
        parser.processLine(""); // end of corrupted block — nothing dispatched
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");

        // Parser recovers cleanly at the next event boundary
        parser.processLine("data: recovered");
        parser.processLine("");
        assertEquals(1, events.size(), "Parser should recover after byte overflow");
        assertEquals("recovered", events.get(0).data());
    }

    @Test
    public void testInvalidRetryDoesNotCallErrorConsumer() {
        // Per SSE spec: non-digit retry values are silently ignored, not errors.
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        parser.processLine("retry: not-a-number");
        parser.processLine("retry: +100");
        parser.processLine("data: Test");
        parser.processLine("");

        assertNull(error.get(), "errorConsumer must not be called for non-digit retry values");
        assertEquals(1, events.size(), "Event should still be dispatched");
        assertNull(events.get(0).retry(), "Retry should remain null");
    }

    @Test
    public void testProcessingContinuesAfterErrorConsumerInvocation() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add);

        parser.processLine(null);
        parser.processLine("data: recovered");
        parser.processLine("");

        assertEquals(1, errors.size(), "Should have one error from null line");
        assertEquals(1, events.size(), "Should still dispatch the event after error");
        assertEquals("recovered", events.get(0).data());
    }

    @Test
    public void testNullLineWithoutErrorConsumerLogsAndContinues() {
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        // Without errorConsumer: null is logged, not thrown
        assertDoesNotThrow(() -> parser.processLine(null));

        parser.processLine("data: still works");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("still works", events.get(0).data());
    }

    @Test
    public void testCRLFLineTerminatorsPreservedInValue() {
        // SSEParser processes individual lines after line-splitting by the HTTP client.
        // Callers (e.g., BufferedReader.readLine()) strip the \r\n terminator before passing
        // the line here, so processLine never receives a bare \r from a CRLF stream.
        // If a caller passes a line with a trailing \r (e.g., a non-standard source), it is
        // preserved in the data value — stripping is the caller's responsibility.
        List<ServerSentEvent> events = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add);

        parser.processLine("data: value\r");
        parser.processLine("");

        assertEquals(1, events.size());
        assertEquals("value\r", events.get(0).data());
    }

    @Test
    public void testLargeJsonRpcResponseRejectedByOldLimit() {
        // Reproducer: a 70 KB payload exceeds the old 64 KB per-line limit but fits within the
        // new 1 MB default, proving the limit raise fixes real-world large JSON-RPC responses.
        String hugeJson = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"" + "x".repeat(70_000) + "\"}";

        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig oldLimit = SSEParserConfig.builder().maxLineLength(65536).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, oldLimit);

        parser.processLine("data: " + hugeJson);
        parser.processLine("");

        assertEquals(0, events.size(), "Event must be rejected under the old 64 KB limit");
        assertEquals(1, errors.size());
        assertInstanceOf(IllegalArgumentException.class, errors.get(0));

        // Same payload split across two data: lines parses fine under the old limit
        events.clear();
        errors.clear();
        String half1 = hugeJson.substring(0, hugeJson.length() / 2);
        String half2 = hugeJson.substring(hugeJson.length() / 2);
        parser.processLine("data: " + half1);
        parser.processLine("data: " + half2);
        parser.processLine("");

        assertEquals(0, errors.size(), "Split payload should not trigger any error");
        assertEquals(1, events.size());
        assertEquals(half1 + "\n" + half2, events.get(0).data());
    }

    @Test
    public void testLargeJsonRpcResponseAcceptedByDefault() {
        // With the raised 1 MB default the same payload parses on a single line
        String hugeJson = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"" + "x".repeat(70_000) + "\"}";

        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add);

        parser.processLine("data: " + hugeJson);
        parser.processLine("");

        assertEquals(0, errors.size(), "70 KB line should be accepted with default 1 MB limit");
        assertEquals(1, events.size());
        assertEquals(hugeJson, events.get(0).data());
    }

    @Test
    public void testLargeSingleLineEventAcceptedByDefault() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        // 200 KB single data: line -- previously rejected at 64 KB, now accepted with 1 MB default
        String largeJson = "{\"result\":\"" + "x".repeat(200_000) + "\"}";
        parser.processLine("data: " + largeJson);
        parser.processLine("");

        assertNull(error.get(), "200 KB line should be accepted with default 1 MB limit");
        assertEquals(1, events.size());
        assertEquals(largeJson, events.get(0).data());
    }

    @Test
    public void testDisabledLineLengthCheck() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        SSEParserConfig config = SSEParserConfig.builder()
                .maxLineLength(0)
                .maxBufferChars(2 * 1024 * 1024)
                .build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set, config);

        // With maxLineLength=0 (disabled), even very large lines are accepted
        String hugeLine = "data: " + "x".repeat(1_500_000);
        parser.processLine(hugeLine);
        parser.processLine("");

        assertNull(error.get(), "Line length check should be disabled when maxLineLength=0");
        assertEquals(1, events.size());
        assertEquals("x".repeat(1_500_000), events.get(0).data());
    }

    @Test
    public void testCustomBufferLineLimit() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        SSEParserConfig config = SSEParserConfig.builder()
                .maxBufferLines(5)
                .build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set, config);

        for (int i = 0; i < 5; i++) {
            parser.processLine("data: line" + i);
        }
        assertNull(error.get(), "No error expected at exactly the limit");

        parser.processLine("data: overflow");
        assertNotNull(error.get(), "errorConsumer should be called when custom buffer line limit exceeded");
        parser.processLine("");
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");
    }

    @Test
    public void testCustomBufferCharLimit() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        SSEParserConfig config = SSEParserConfig.builder()
                .maxBufferChars(100)
                .build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set, config);

        parser.processLine("data: " + "x".repeat(101));
        assertNotNull(error.get(), "errorConsumer should be called when custom buffer char limit exceeded");
        parser.processLine("");
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");
    }

    @Test
    public void testParserRecoveryAfterCustomLimitViolation() {
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        SSEParserConfig config = SSEParserConfig.builder()
                .maxBufferLines(2)
                .build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set, config);

        parser.processLine("data: line0");
        parser.processLine("data: line1");
        parser.processLine("data: overflow");
        assertNotNull(error.get(), "errorConsumer should be called when custom buffer line limit exceeded");
        parser.processLine("");
        assertEquals(0, events.size(), "Corrupted event block must not be dispatched");

        error.set(null);
        parser.processLine("data: ok");
        parser.processLine("");
        assertNull(error.get(), "No error expected after recovery");
        assertEquals(1, events.size(), "Parser should recover after custom limit violation");
    }

    @Test
    public void testSSEParserConfigDefaults() {
        SSEParserConfig config = SSEParserConfig.DEFAULT;
        assertEquals(1024 * 1024, config.maxLineLength());
        assertEquals(1000, config.maxBufferLines());
        assertEquals(1024 * 1024, config.maxBufferChars());
    }

    @Test
    public void testSSEParserConfigBuilder() {
        SSEParserConfig config = SSEParserConfig.builder()
                .maxLineLength(500_000)
                .maxBufferLines(2000)
                .maxBufferChars(4 * 1024 * 1024)
                .build();
        assertEquals(500_000, config.maxLineLength());
        assertEquals(2000, config.maxBufferLines());
        assertEquals(4 * 1024 * 1024, config.maxBufferChars());
    }

    @Test
    public void testSSEParserConfigValidation() {
        assertDoesNotThrow(() -> SSEParserConfig.builder().maxLineLength(0).build(),
                "maxLineLength=0 (disabled) should be allowed");

        assertThrows(IllegalArgumentException.class,
                () -> SSEParserConfig.builder().maxLineLength(-1).build());

        assertThrows(IllegalArgumentException.class,
                () -> SSEParserConfig.builder().maxBufferLines(0).build());

        assertThrows(IllegalArgumentException.class,
                () -> SSEParserConfig.builder().maxBufferChars(0).build());
    }

    // --- lastEventId / skipping interaction ---

    @Test
    public void testLastEventIdNotAdvancedWhenBlockSkippedByLineTooLong() {
        // Regression: id: in a corrupt block must not update the reconnect cursor.
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        // Limit long enough for "id: good-id" (11) and "data: ok" (8), but shorter than the oversized data line.
        SSEParserConfig config = SSEParserConfig.builder().maxLineLength(50).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        // Good event that sets lastEventId to "good-id"
        parser.processLine("id: good-id");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals(1, events.size(), "Good event should be dispatched");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId should be set by good event");

        // Corrupt block: id: comes before the oversized line
        parser.processLine("id: bad-id");
        parser.processLine("data: " + "x".repeat(51)); // triggers skip
        parser.processLine(""); // end of corrupt block
        assertEquals(1, events.size(), "Corrupt block must not be dispatched");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId must not advance for a skipped block");
    }

    @Test
    public void testLastEventIdNotAdvancedWhenBlockSkippedByBufferLineOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferLines(2).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        parser.processLine("id: good-id");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId should be set by good event");

        parser.processLine("id: bad-id");
        parser.processLine("data: line0");
        parser.processLine("data: line1");
        parser.processLine("data: overflow"); // triggers skip
        parser.processLine("");
        assertEquals(1, events.size(), "Only the first good event should be dispatched");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId must not advance for a skipped block");
    }

    @Test
    public void testSkippedBlockIdDoesNotPoisonNextEventByLineTooLong() {
        // Regression: after a skipped block, currentEventId must be rolled back so a subsequent
        // event without an id: field does not inherit the skipped block's id.
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig config = SSEParserConfig.builder().maxLineLength(50).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        // Good event
        parser.processLine("id: good");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals("good", parser.getLastEventId(), "lastEventId should be set by good event");

        // Corrupt block with a different id
        parser.processLine("id: bad");
        parser.processLine("data: " + "x".repeat(51));
        parser.processLine("");

        // Next valid event has no id: field
        parser.processLine("data: next-valid-event");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("good", events.get(1).id(), "Next event must carry the pre-skip id, not the skipped block's id");
        assertEquals("good", parser.getLastEventId(), "lastEventId must not be poisoned by the skipped block");
    }

    @Test
    public void testSkippedBlockIdDoesNotPoisonNextEventByBufferOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferLines(2).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        parser.processLine("id: good");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals("good", parser.getLastEventId(), "lastEventId should be set by good event");

        parser.processLine("id: bad");
        parser.processLine("data: line0");
        parser.processLine("data: line1");
        parser.processLine("data: overflow");
        parser.processLine("");

        parser.processLine("data: next-valid-event");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("good", events.get(1).id(), "Next event must carry the pre-skip id, not the skipped block's id");
        assertEquals("good", parser.getLastEventId(), "lastEventId must not be poisoned by the skipped block");
    }

    @Test
    public void testSkippedBlockIdDoesNotPoisonNextEventByCharOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferChars(20).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        parser.processLine("id: good");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals("good", parser.getLastEventId(), "lastEventId should be set by good event");

        parser.processLine("id: bad");
        parser.processLine("data: " + "x".repeat(21));
        parser.processLine("");

        parser.processLine("data: next-valid-event");
        parser.processLine("");

        assertEquals(2, events.size());
        assertEquals("good", events.get(1).id(), "Next event must carry the pre-skip id, not the skipped block's id");
        assertEquals("good", parser.getLastEventId(), "lastEventId must not be poisoned by the skipped block");
    }

    @Test
    public void testLastEventIdNotAdvancedWhenBlockSkippedByBufferCharOverflow() {
        List<ServerSentEvent> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferChars(20).build();
        ServerSentEventParser parser = new ServerSentEventParser(events::add, errors::add, config);

        parser.processLine("id: good-id");
        parser.processLine("data: ok");
        parser.processLine("");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId should be set by good event");

        parser.processLine("id: bad-id");
        parser.processLine("data: " + "x".repeat(21)); // triggers skip
        parser.processLine("");
        assertEquals(1, events.size(), "Only the first good event should be dispatched");
        assertEquals("good-id", parser.getLastEventId(), "lastEventId must not advance for a skipped block");
    }
}
