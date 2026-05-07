package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        ServerSentEventParser parser = new ServerSentEventParser(events::add, error::set);

        // Oversized line mid-event: the whole event block is discarded
        parser.processLine("data: before overflow");
        String longLine = "data: " + "x".repeat(65537);
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

        // Value is 65530 chars so the full line ("data: " + value = 65536) stays within the per-line
        // limit; 17 such lines (17 * 65530 = 1,114,010 bytes) exceed the 1MB buffer byte limit.
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
}
