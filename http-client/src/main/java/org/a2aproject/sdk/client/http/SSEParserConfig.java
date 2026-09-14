package org.a2aproject.sdk.client.http;

/**
 * Configuration for {@link ServerSentEventParser} limits.
 *
 * <p>All limits have safe defaults suitable for most deployments. Use the {@link Builder}
 * to override individual values -- for example, to raise the per-event character budget
 * for agents that return large artifacts.
 *
 * <p><strong>Enforcement semantics:</strong> {@code maxLineLength} is enforced at the
 * transport layer as a <em>byte</em> limit on raw UTF-8 data, before lines are decoded
 * to characters. For ASCII-only content (the common case for SSE/JSON-RPC), bytes and
 * characters are equivalent. For multi-byte UTF-8 content, the byte-level check is
 * stricter — a line of {@code maxLineLength} multi-byte characters may exceed the limit.
 * This is intentional: the byte-level enforcement prevents memory exhaustion from a
 * malicious unterminated line before it is fully materialised.
 *
 * <p><strong>Security note:</strong> setting {@code maxLineLength} to {@code 0} disables
 * the per-line memory protection entirely. Without this limit, a malicious server sending
 * a single unterminated line (no newline) can consume unbounded memory — {@code maxBufferChars}
 * only limits accumulated {@code data:} field values <em>after</em> lines have been decoded,
 * not the raw line itself. Only disable the per-line check when you trust the remote agent
 * or have other safeguards (e.g. a reverse proxy that enforces its own line-length limit).
 *
 * @param maxLineLength max bytes per raw SSE line (0 = disabled)
 * @param maxBufferLines max {@code data:} lines per event block
 * @param maxBufferChars max total characters across all {@code data:} values per event
 */
public record SSEParserConfig(int maxLineLength, int maxBufferLines, int maxBufferChars) {

    /**
     * Default configuration: 1 MB per-line and per-event character limit, 1 000 data lines per event.
     */
    public static final SSEParserConfig DEFAULT = new SSEParserConfig(1024 * 1024, 1000, 1024 * 1024);

    public SSEParserConfig {
        if (maxLineLength < 0) {
            throw new IllegalArgumentException("maxLineLength must be >= 0, got " + maxLineLength);
        }
        if (maxBufferLines <= 0) {
            throw new IllegalArgumentException("maxBufferLines must be > 0, got " + maxBufferLines);
        }
        if (maxBufferChars <= 0) {
            throw new IllegalArgumentException("maxBufferChars must be > 0, got " + maxBufferChars);
        }
    }

    /**
     * Returns a new {@link Builder} initialized with the {@link #DEFAULT} values.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxLineLength = DEFAULT.maxLineLength;
        private int maxBufferLines = DEFAULT.maxBufferLines;
        private int maxBufferChars = DEFAULT.maxBufferChars;

        Builder() {
        }

        /**
         * Sets the maximum number of bytes allowed in a single raw SSE line.
         * Set to {@code 0} to disable the per-line check.
         *
         * <p><strong>Security note:</strong> disabling this check removes the first line of
         * defence against memory exhaustion from oversized SSE lines. Ensure
         * {@code maxBufferChars} is set to an acceptable upper bound when disabling.
         */
        public Builder maxLineLength(int maxLineLength) {
            this.maxLineLength = maxLineLength;
            return this;
        }

        /**
         * Sets the maximum number of {@code data:} lines allowed in a single event block.
         */
        public Builder maxBufferLines(int maxBufferLines) {
            this.maxBufferLines = maxBufferLines;
            return this;
        }

        /**
         * Sets the maximum total characters across all {@code data:} values in a single event.
         */
        public Builder maxBufferChars(int maxBufferChars) {
            this.maxBufferChars = maxBufferChars;
            return this;
        }

        public SSEParserConfig build() {
            return new SSEParserConfig(maxLineLength, maxBufferLines, maxBufferChars);
        }
    }
}
