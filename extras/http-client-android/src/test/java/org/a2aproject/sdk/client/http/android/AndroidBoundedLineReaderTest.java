package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.a2aproject.sdk.client.http.BoundedLineAccumulator;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the bounded line reader inside {@link AndroidA2AHttpClient}.
 * Since {@code readBoundedLine} is inside a private inner class, we access it via reflection.
 */
public class AndroidBoundedLineReaderTest {

    private static final Method READ_BOUNDED_LINE;

    static {
        try {
            Class<?> builderClass = Class.forName(
                    "org.a2aproject.sdk.client.http.android.AndroidA2AHttpClient$AndroidBuilder");
            READ_BOUNDED_LINE = builderClass.getDeclaredMethod(
                    "readBoundedLine", InputStream.class, BoundedLineAccumulator.class);
            READ_BOUNDED_LINE.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static boolean readBoundedLineRaw(InputStream is, BoundedLineAccumulator accumulator) throws Exception {
        try {
            return (Boolean) READ_BOUNDED_LINE.invoke(null, is, accumulator);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }

    private static InputStream stream(String data) {
        return new BufferedInputStream(
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<String> readAllLines(InputStream is, int maxLineBytes) throws Exception {
        BoundedLineAccumulator acc = new BoundedLineAccumulator(maxLineBytes);
        List<String> lines = new ArrayList<>();
        while (readBoundedLineRaw(is, acc)) {
            if (!acc.isTooLong()) {
                lines.add(acc.toLine());
            }
            acc.reset();
        }
        return lines;
    }

    @Test
    public void testBasicLFLines() throws Exception {
        List<String> lines = readAllLines(stream("hello\nworld\n"), 0);
        assertEquals(List.of("hello", "world"), lines);
    }

    @Test
    public void testCRLFLines() throws Exception {
        List<String> lines = readAllLines(stream("hello\r\nworld\r\n"), 0);
        assertEquals(List.of("hello", "world"), lines);
    }

    @Test
    public void testBareCRLines() throws Exception {
        List<String> lines = readAllLines(stream("hello\rworld\r"), 0);
        assertEquals(List.of("hello", "world"), lines);
    }

    @Test
    public void testMixedTerminators() throws Exception {
        List<String> lines = readAllLines(stream("LF\nCRLF\r\nCR\rend"), 0);
        assertEquals(List.of("LF", "CRLF", "CR", "end"), lines);
    }

    @Test
    public void testStreamEndWithoutTerminator() throws Exception {
        List<String> lines = readAllLines(stream("no-newline"), 0);
        assertEquals(List.of("no-newline"), lines);
    }

    @Test
    public void testEmptyStream() throws Exception {
        List<String> lines = readAllLines(stream(""), 0);
        assertEquals(List.of(), lines);
    }

    @Test
    public void testEmptyLines() throws Exception {
        List<String> lines = readAllLines(stream("\n\n\n"), 0);
        assertEquals(List.of("", "", ""), lines);
    }

    @Test
    public void testReturnsFalseAtEndOfStream() throws Exception {
        InputStream is = stream("one\n");
        BoundedLineAccumulator acc = new BoundedLineAccumulator(0);
        assertTrue(readBoundedLineRaw(is, acc));
        assertEquals("one", acc.toLine());
        acc.reset();
        assertFalse(readBoundedLineRaw(is, acc));
    }

    @Test
    public void testLineTooLongIsFlagged() throws Exception {
        InputStream is = stream("short\n" + "x".repeat(20) + "\nok\n");
        BoundedLineAccumulator acc = new BoundedLineAccumulator(10);

        assertTrue(readBoundedLineRaw(is, acc));
        assertFalse(acc.isTooLong());
        assertEquals("short", acc.toLine());
        acc.reset();

        assertTrue(readBoundedLineRaw(is, acc));
        assertTrue(acc.isTooLong(), "Too-long line should be flagged");
        acc.reset();

        assertTrue(readBoundedLineRaw(is, acc));
        assertFalse(acc.isTooLong());
        assertEquals("ok", acc.toLine());
    }

    @Test
    public void testLineTooLongAtStreamEnd() throws Exception {
        InputStream is = stream("ok\n" + "x".repeat(20));
        BoundedLineAccumulator acc = new BoundedLineAccumulator(10);

        assertTrue(readBoundedLineRaw(is, acc));
        assertFalse(acc.isTooLong());
        assertEquals("ok", acc.toLine());
        acc.reset();

        assertTrue(readBoundedLineRaw(is, acc));
        assertTrue(acc.isTooLong(), "Too-long line at stream end should be flagged");
    }

    @Test
    public void testLineExactlyAtLimit() throws Exception {
        List<String> lines = readAllLines(stream("1234567890\n"), 10);
        assertEquals(1, lines.size());
        assertEquals("1234567890", lines.get(0));
    }

    @Test
    public void testLineOneOverLimit() throws Exception {
        InputStream is = stream("12345678901\n");
        BoundedLineAccumulator acc = new BoundedLineAccumulator(10);
        assertTrue(readBoundedLineRaw(is, acc));
        assertTrue(acc.isTooLong(), "Line one byte over limit should be flagged");
    }

    @Test
    public void testMaxLineBytesZeroDisablesLimit() throws Exception {
        String longLine = "x".repeat(10_000);
        List<String> lines = readAllLines(stream(longLine + "\n"), 0);
        assertEquals(1, lines.size());
        assertEquals(longLine, lines.get(0));
    }

    @Test
    public void testLineTooLongWithCRLF() throws Exception {
        InputStream is = stream("x".repeat(20) + "\r\nok\n");
        BoundedLineAccumulator acc = new BoundedLineAccumulator(10);

        assertTrue(readBoundedLineRaw(is, acc));
        assertTrue(acc.isTooLong());
        acc.reset();

        assertTrue(readBoundedLineRaw(is, acc));
        assertFalse(acc.isTooLong());
        assertEquals("ok", acc.toLine());
    }

    @Test
    public void testLineTooLongWithBareCR() throws Exception {
        InputStream is = stream("x".repeat(20) + "\rok\n");
        BoundedLineAccumulator acc = new BoundedLineAccumulator(10);

        assertTrue(readBoundedLineRaw(is, acc));
        assertTrue(acc.isTooLong());
        acc.reset();

        assertTrue(readBoundedLineRaw(is, acc));
        assertFalse(acc.isTooLong());
        assertEquals("ok", acc.toLine());
    }

    @Test
    public void testUtf8MultiByteCharacters() throws Exception {
        List<String> lines = readAllLines(stream("héllo wörld café\n"), 0);
        assertEquals(1, lines.size());
        assertEquals("héllo wörld café", lines.get(0));
    }

    @Test
    public void testCRFollowedByEOFDoesNotHang() throws Exception {
        List<String> lines = readAllLines(stream("hello\r"), 0);
        assertEquals(List.of("hello"), lines);
    }
}
