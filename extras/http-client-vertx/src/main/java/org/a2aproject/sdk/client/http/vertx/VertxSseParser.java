package org.a2aproject.sdk.client.http.vertx;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import java.util.function.Consumer;
import org.a2aproject.sdk.client.http.BoundedLineAccumulator;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.http.ServerSentEventParser;

/** Splits response buffers into bounded UTF-8 lines without pausing an incomplete SSE event. */
final class VertxSseParser implements Handler<Buffer> {

    private final BoundedLineAccumulator line = new BoundedLineAccumulator(SSEParserConfig.DEFAULT.maxLineLength());
    private final ServerSentEventParser parser;
    private boolean pendingCR;

    VertxSseParser(Consumer<ServerSentEvent> messageConsumer, Consumer<Throwable> errorConsumer) {
        parser = new ServerSentEventParser(messageConsumer, errorConsumer, SSEParserConfig.DEFAULT);
    }

    @Override
    public void handle(Buffer buffer) {
        for (int i = 0; i < buffer.length(); i++) {
            byte b = buffer.getByte(i);
            if (pendingCR) {
                pendingCR = false;
                if (b == '\n') {
                    continue;
                }
            }
            if (b == '\r' || b == '\n') {
                processLine();
                pendingCR = b == '\r';
            } else {
                line.addByte(b);
            }
        }
    }

    void end() {
        if (line.isTooLong() || line.size() > 0) {
            processLine();
        }
        parser.flush();
    }

    private void processLine() {
        if (line.isTooLong()) {
            parser.processLineTooLong();
        } else {
            parser.processLine(line.toLine());
        }
        line.reset();
    }
}
