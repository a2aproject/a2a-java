package org.a2aproject.sdk.client.transport.jsonrpc;

import static org.a2aproject.sdk.client.transport.jsonrpc.JsonMessages.GET_TASK_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;

class ResponseIdIntegrationTest {
    @Test void concurrentRequestsUseTheirOwnIds() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String id = JsonParser.parseString(request).getAsJsonObject().get("id").getAsString();
            byte[] body = GET_TASK_TEST_RESPONSE.replace("cd4c76de-d54c-436c-8b9f-4c2703648d64", id).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport("http://localhost:" + server.getAddress().getPort());
            var a = executor.submit(() -> client.getTask(new TaskQueryParams("a"), null));
            var b = executor.submit(() -> client.getTask(new TaskQueryParams("b"), null));
            assertNotNull(a.get(10, TimeUnit.SECONDS));
            assertNotNull(b.get(10, TimeUnit.SECONDS));
        } finally { server.stop(0); executor.shutdownNow(); }
    }

    @Test void explicitNullIdErrorIsPropagated() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"invalid request\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport("http://localhost:" + server.getAddress().getPort());
            var error = assertThrows(A2AClientException.class, () -> client.getTask(new TaskQueryParams("a"), null));
            assertTrue(error.getMessage().contains("invalid request"), error.getMessage());
        } finally { server.stop(0); }
    }
}
