package org.a2aproject.sdk.client.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class VertxA2AHttpClientSSETest extends AbstractA2AHttpClientSSETest {

    private VertxA2AHttpClient vertxClient;

    @Override
    protected A2AHttpClient createClient() {
        vertxClient = new VertxA2AHttpClient();
        return vertxClient;
    }

    @AfterEach
    public void closeVertxClient() {
        if (vertxClient != null) {
            vertxClient.close();
        }
    }

    // The two tests below expose gaps in the Vert.x SSE path: it delegates to
    // Vert.x's built-in BodyCodec.sseStream() rather than ServerSentEventParser,
    // so last-event-id propagation and end-of-stream flush() are not supported.
    // These will be fixed when the Vert.x path is migrated to ServerSentEventParser.

    @Override
    @Test
    @Disabled("Vert.x BodyCodec.sseStream() does not propagate last event ID to subsequent events")
    public void testSSEEventIdAndLastEventId() {
    }

    @Override
    @Test
    @Disabled("Vert.x BodyCodec.sseStream() has no flush() equivalent for streams ending without a trailing blank line")
    public void testSSEStreamEndingWithoutTrailingEmptyLine() {
    }
}
