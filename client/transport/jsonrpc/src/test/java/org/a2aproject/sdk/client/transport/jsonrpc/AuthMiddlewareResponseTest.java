package org.a2aproject.sdk.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.transport.jsonrpc.sse.SSEEventListener;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;

class AuthMiddlewareResponseTest {
    @Test
    void missingIdInSuccessfulHttpResponseIsRejected() {
        assertThrows(
                A2AClientException.class,
                () ->
                        JsonRpcCorrelation.validate(
                                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600}}", "expected"));
    }

    @Test
    void missingIdInStreamingErrorCancelsFuture() {
        var errors = new AtomicReference<Throwable>();
        var listener =
                new SSEEventListener(event -> fail("Unexpected event"), errors::set, "expected");
        var future = new CompletableFuture<Void>();
        listener.bindFuture(future);
        listener.onMessage(
                new ServerSentEvent("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600}}"), future);
        assertTrue(future.isCancelled());
        assertInstanceOf(A2AClientException.class, errors.get());
    }

    @Test
    void missingIdOnHttp429PreservesStatusAndRetryAfter() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    byte[] body =
                            "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Rate limited\"}}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.getResponseHeaders().add("Retry-After", "120");
                    exchange.sendResponseHeaders(429, body.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                });
        server.start();
        try {
            var client = new JSONRPCTransport("http://127.0.0.1:" + server.getAddress().getPort());
            var error =
                    assertThrows(
                            A2AClientException.class,
                            () -> client.getTask(new TaskQueryParams("task"), null));
            var httpError = assertInstanceOf(A2AClientHTTPError.class, error.getCause());
            assertEquals(429, httpError.getCode());
            assertTrue(httpError.getResponseBody().contains("Rate limited"));
            assertTrue(
                    httpError.getResponseHeaders().entrySet().stream()
                            .anyMatch(
                                    entry ->
                                            entry.getKey().equalsIgnoreCase("Retry-After")
                                                    && entry.getValue().contains("120")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void missingIdOnHttp401PreservesStatusAndHeaders() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    byte[] body =
                            "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32000,\"message\":\"Unauthorized\"}}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.getResponseHeaders().add("WWW-Authenticate", "Bearer realm=\"agent\"");
                    exchange.sendResponseHeaders(401, body.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                });
        server.start();
        try {
            var client = new JSONRPCTransport("http://127.0.0.1:" + server.getAddress().getPort());
            var error =
                    assertThrows(
                            A2AClientException.class,
                            () -> client.getTask(new TaskQueryParams("task"), null));
            var httpError = assertInstanceOf(A2AClientHTTPError.class, error.getCause().getCause());
            assertEquals(401, httpError.getCode());
            assertTrue(
                    httpError.getResponseHeaders().entrySet().stream()
                            .anyMatch(
                                    entry ->
                                            entry.getKey().equalsIgnoreCase("WWW-Authenticate")
                                                    && entry.getValue()
                                                            .contains("Bearer realm=\"agent\"")));
        } finally {
            server.stop(0);
        }
    }
}
