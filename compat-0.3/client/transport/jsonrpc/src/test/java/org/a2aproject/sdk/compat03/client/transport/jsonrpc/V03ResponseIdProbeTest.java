package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonMessages_v0_3.GET_TASK_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.junit.jupiter.api.Test;

class V03ResponseIdProbeTest {
    private static final String TASK_ID = "de38c76d-d54c-436c-8b9f-4c2703648d64";

    @Test
    void correlatesActualHttpResponses() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        var wireId = new AtomicReference<String>();
        var bodyFn = new AtomicReference<Function<String, String>>();
        server.createContext("/", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String id = JsonParser.parseString(request).getAsJsonObject().get("id").getAsString();
            wireId.set(id);
            byte[] bytes = bodyFn.get().apply(id).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport_v0_3("http://localhost:" + server.getAddress().getPort());
            var params = new TaskQueryParams_v0_3(TASK_ID, 10);
            bodyFn.set(id -> GET_TASK_TEST_RESPONSE.replace("\"jsonrpc\": \"2.0\",", "\"jsonrpc\": \"2.0\", \"id\":\"" + id + "\","));
            assertEquals(TASK_ID, client.getTask(params, null).id());
            bodyFn.set(id -> GET_TASK_TEST_RESPONSE.replace("\"jsonrpc\": \"2.0\",", "\"jsonrpc\": \"2.0\", \"id\":\"unrelated\","));
            assertThrows(A2AClientException_v0_3.class, () -> client.getTask(params, null));
            assertNotEquals("unrelated", wireId.get());
            bodyFn.set(id -> "{\"jsonrpc\":\"2.0\",\"id\":\"unrelated\",\"error\":{\"code\":-32600,\"message\":\"wrong request\"}}");
            var mismatch = assertThrows(A2AClientException_v0_3.class, () -> client.getTask(params, null));
            assertFalse(mismatch.getMessage().contains("wrong request"));
            bodyFn.set(id -> "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"server rejection\"}}");
            var error = assertThrows(A2AClientException_v0_3.class, () -> client.getTask(params, null));
            assertTrue(error.getMessage().contains("server rejection"));

            var interceptor = new org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3() {
                @Override
                public org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3 intercept(
                        String method, Object payload, java.util.Map<String, String> headers,
                        org.a2aproject.sdk.compat03.spec.AgentCard_v0_3 card,
                        org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3 context) {
                    var request = (org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3) payload;
                    var replacement = new org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3("intercepted-id", request.getParams());
                    return new org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3(replacement, headers);
                }
            };
            var interceptedClient = new JSONRPCTransport_v0_3(null, null,
                    "http://localhost:" + server.getAddress().getPort(), java.util.List.of(interceptor));
            bodyFn.set(id -> GET_TASK_TEST_RESPONSE.replace("\"jsonrpc\": \"2.0\",", "\"jsonrpc\": \"2.0\", \"id\":\"intercepted-id\","));
            assertEquals(TASK_ID, interceptedClient.getTask(params, null).id());
            assertEquals("intercepted-id", wireId.get());
        } finally { server.stop(0); }
    }

    @Test
    void validatesPreStreamJsonErrors() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        var id = new AtomicReference<String>("wrong");
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String body = "{\"jsonrpc\":\"2.0\",\"id\":" + id.get()
                    + ",\"error\":{\"code\":-32600,\"message\":\"preflight rejection\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport_v0_3("http://localhost:" + server.getAddress().getPort());
            for (String responseId : new String[] { "\"unrelated\"", "null" }) {
                id.set(responseId);
                var error = new AtomicReference<Throwable>();
                var latch = new CountDownLatch(1);
                client.resubscribe(new TaskIdParams_v0_3("t"), event -> fail("unexpected event"),
                        ex -> { error.set(ex); latch.countDown(); }, null);
                assertTrue(latch.await(5, TimeUnit.SECONDS));
                if (responseId.equals("null")) {
                    assertNotNull(error.get());
                    assertTrue(error.get().getMessage().contains("preflight rejection"));
                } else {
                    assertInstanceOf(IllegalArgumentException.class, error.get());
                }
            }
        } finally { server.stop(0); }
    }

    @Test
    void rejectsWrongIdInRealSse() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = ("data: {\"jsonrpc\":\"2.0\",\"id\":\"wrong\",\"result\":{\"kind\":\"task\",\"id\":\"t\",\"status\":{\"state\":\"working\"}}}\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        try {
            var client = new JSONRPCTransport_v0_3("http://localhost:" + server.getAddress().getPort());
            var events = new AtomicInteger();
            var error = new AtomicReference<Throwable>();
            var latch = new CountDownLatch(1);
            client.resubscribe(new TaskIdParams_v0_3("t"), event -> events.incrementAndGet(), ex -> { error.set(ex); latch.countDown(); }, null);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class, error.get());
            assertEquals(0, events.get());
        } finally { server.stop(0); }
    }
}
