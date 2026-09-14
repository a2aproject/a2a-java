package org.a2aproject.sdk.client.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Accumulates raw bytes for a single line while enforcing a configurable byte limit.
 *
 * <p>Used by both the blocking ({@code readBoundedLine}) and reactive
 * ({@code BoundedLineBodySubscriber}) bounded-line readers to centralise the
 * accumulation logic and limit enforcement.
 *
 * <p>When {@code maxLineBytes <= 0} the per-line byte cap is disabled.
 */
public final class BoundedLineAccumulator {

    static final int INITIAL_BUFFER_CAPACITY = 256;

    private final int maxLineBytes;
    private final ByteArrayOutputStream buffer;
    private boolean tooLong;

    /**
     * Creates a new accumulator with the given per-line byte limit.
     *
     * @param maxLineBytes the maximum number of bytes per line ({@code 0} = disabled)
     */
    public BoundedLineAccumulator(int maxLineBytes) {
        this.maxLineBytes = maxLineBytes;
        this.buffer = new ByteArrayOutputStream(INITIAL_BUFFER_CAPACITY);
    }

    /**
     * Adds a single byte to the accumulator.
     *
     * <p>If the limit has already been exceeded, the byte is silently discarded.
     * If adding this byte would exceed the limit, the accumulator transitions to
     * the "too long" state and discards all previously accumulated bytes.
     *
     * @param b the byte to add (only the low 8 bits are used)
     */
    public void addByte(int b) {
        if (tooLong) {
            return;
        }
        if (maxLineBytes > 0 && buffer.size() >= maxLineBytes) {
            tooLong = true;
            buffer.reset();
            return;
        }
        buffer.write(b);
    }

    /**
     * Adds a chunk of bytes to the accumulator.
     *
     * <p>If the accumulated byte count plus the chunk length would exceed the limit,
     * the accumulator transitions to the "too long" state and discards all
     * previously accumulated bytes. The chunk is not written.
     *
     * @param chunk the byte array to add from
     * @param off the start offset within the array
     * @param len the number of bytes to add
     */
    public void addChunk(byte[] chunk, int off, int len) {
        if (tooLong) {
            return;
        }
        if (maxLineBytes > 0 && buffer.size() + len > maxLineBytes) {
            tooLong = true;
            buffer.reset();
            return;
        }
        buffer.write(chunk, off, len);
    }

    /**
     * Returns {@code true} if the accumulated bytes have exceeded the configured limit.
     */
    public boolean isTooLong() {
        return tooLong;
    }

    /**
     * Returns the number of bytes currently accumulated.
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Assembles the accumulated bytes into a UTF-8 string.
     *
     * @return the decoded line
     */
    public String toLine() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Resets the accumulator for the next line, clearing all accumulated bytes
     * and the "too long" flag.
     */
    public void reset() {
        buffer.reset();
        tooLong = false;
    }
}
