package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.TrustOptions;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientIntegrationTest;
import org.a2aproject.sdk.client.http.HttpsDefaultPortFixture;
import org.a2aproject.sdk.client.http.RedirectHeaderPolicy;
import org.junit.jupiter.api.Test;

public class VertxA2AHttpClientTest {

    @Test
    public void defaultPortPreservesHostHeader() throws Exception {
        var vertx = Vertx.vertx();
        try {
            AbstractA2AHttpClientIntegrationTest.assertDefaultPortHostOnWire(address -> proxiedClient(vertx, address));
        } finally {
            vertx.close();
        }
    }

    @Test
    void normalizedHttpsDefaultPortPreservesHostHeader() throws Exception {
        var vertx = Vertx.vertx();
        try {
            HttpsDefaultPortFixture.assertHostPreserved(connection -> {
                var proxy = new ProxyOptions().setHost(connection.proxy().getHostString())
                        .setPort(connection.proxy().getPort());
                var options = new HttpClientOptions().setProxyOptions(proxy)
                        .setTrustOptions(TrustOptions.wrap(connection.trustManagers()));
                return new VertxA2AHttpClient(vertx, false, RedirectHeaderPolicy.DEFAULT, options);
            });
        } finally {
            vertx.close();
        }
    }

    @Test
    void streamsThroughConfiguredHttpProxy() throws Exception {
        var proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/", exchange -> {
            byte[] body = "data: via proxy\n\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        proxy.start();
        var vertx = Vertx.vertx();
        try (var client = proxiedClient(vertx, proxy.getAddress())) {
            var data = new AtomicReference<String>();
            var failure = new AtomicReference<Throwable>();
            client.createGet().url("http://example.invalid/stream")
                    .getAsyncSSE(event -> data.set(event.data()), failure::set, () -> {})
                    .get(5, TimeUnit.SECONDS);
            assertNull(failure.get());
            assertEquals("via proxy", data.get());
        } finally {
            vertx.close();
            proxy.stop(0);
        }
    }

    private static VertxA2AHttpClient proxiedClient(Vertx vertx, InetSocketAddress address) {
        var proxy = new ProxyOptions().setHost(address.getHostString()).setPort(address.getPort());
        var options = new HttpClientOptions().setKeepAlive(true).setProxyOptions(proxy);
        return new VertxA2AHttpClient(vertx, false, RedirectHeaderPolicy.DEFAULT, options);
    }

    @Test
    public void encodedPathIsPreservedOnWire() throws Exception {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            AbstractA2AHttpClientIntegrationTest.assertEncodedPathOnWire(client);
        }
    }

    @Test
    public void testNoArgsConstructor() {
        VertxA2AHttpClient client = new VertxA2AHttpClient();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testVertxParameterConstructor() {
        Vertx vertx = Vertx.vertx();
        VertxA2AHttpClient client = new VertxA2AHttpClient(vertx);
        assertNotNull(client);
        client.close();
        vertx.close();
    }

    @Test
    public void testVertxParameterConstructorNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VertxA2AHttpClient(null);
        });
    }

    @Test
    public void testCreateGet() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            assertNotNull(builder);
        }
    }

    @Test
    public void testCreatePost() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.PostBuilder builder = client.createPost();
            assertNotNull(builder);
        }
    }

    @Test
    public void testCreateDelete() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.DeleteBuilder builder = client.createDelete();
            assertNotNull(builder);
        }
    }

    @Test
    public void testBuilderUrlSetting() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            A2AHttpClient.GetBuilder result = builder.url("https://example.com");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }

    @Test
    public void testBuilderHeaderSetting() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            A2AHttpClient.GetBuilder result = builder.addHeader("Accept", "application/json");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }

    @Test
    public void testBuilderMethodChaining() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet()
                    .url("https://example.com")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer token");
            assertNotNull(builder);
        }
    }

    @Test
    public void testPostBuilderBody() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.PostBuilder builder = client.createPost();
            A2AHttpClient.PostBuilder result = builder.body("{\"key\":\"value\"}");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }
}
