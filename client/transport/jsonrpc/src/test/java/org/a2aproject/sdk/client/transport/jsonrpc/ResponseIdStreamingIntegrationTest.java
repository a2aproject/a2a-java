package org.a2aproject.sdk.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.junit.jupiter.api.Test;

class ResponseIdStreamingIntegrationTest {
    @Test void mismatchedSseEventIsRejected() throws Exception {
        check("text/event-stream", "data: {\"jsonrpc\":\"2.0\",\"id\":\"wrong\",\"result\":{\"task\":{\"id\":\"task\",\"status\":{\"state\":\"TASK_STATE_WORKING\"}}}}\n\n", A2AClientException.class);
    }
    @Test void mismatchedPreflightErrorIsRejected() throws Exception {
        check("application/json", "{\"jsonrpc\":\"2.0\",\"id\":\"wrong\",\"error\":{\"code\":-32600,\"message\":\"invalid\"}}", A2AClientException.class);
    }
    @Test void nullIdPreflightErrorIsPropagated() throws Exception {
        check("application/json", "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"invalid\"}}", A2AError.class);
    }
    private void check(String contentType, String text, Class<? extends Throwable> expected) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport("http://localhost:" + server.getAddress().getPort());
            var terminal = new AtomicReference<Throwable>();
            var received = new AtomicInteger();
            var latch = new CountDownLatch(1);
            client.subscribeToTask(new TaskIdParams("task"), event -> received.incrementAndGet(),
                    error -> { terminal.set(error); latch.countDown(); }, null);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertInstanceOf(expected, terminal.get());
            assertEquals(0, received.get());
        } finally { server.stop(0); }
    }
}
