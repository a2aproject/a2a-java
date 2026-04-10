package org.a2aproject.sdk.client.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class JdkA2AHttpClientTest {

    private ClientAndServer server;

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testDefaultConstructorCreatesUsableClient() throws Exception {
        server = ClientAndServer.startClientAndServer(0);
        server.when(request().withMethod("GET").withPath("/default"))
                .respond(response().withStatusCode(200).withBody("ok"));

        JdkA2AHttpClient client = new JdkA2AHttpClient();

        A2AHttpResponse response = client.createGet()
                .url("http://localhost:" + server.getLocalPort() + "/default")
                .get();

        assertEquals(200, response.status());
        assertEquals("ok", response.body());
    }

    @Test
    public void testConstructorUsesProvidedHttpClient() throws Exception {
        server = ClientAndServer.startClientAndServer(0);
        server.when(request().withMethod("GET").withPath("/custom"))
                .respond(response().withStatusCode(200).withBody("ok"));

        TrackingProxySelector proxySelector = new TrackingProxySelector();
        HttpClient providedClient = HttpClient.newBuilder()
                .proxy(proxySelector)
                .build();

        JdkA2AHttpClient client = new JdkA2AHttpClient(providedClient);

        A2AHttpResponse response = client.createGet()
                .url("http://localhost:" + server.getLocalPort() + "/custom")
                .get();

        assertEquals(200, response.status());
        assertEquals("ok", response.body());
        assertEquals(1, proxySelector.selectCount.get(),
                "Provided HttpClient should be used for request execution");
    }

    @Test
    public void testConstructorRejectsNullHttpClient() {
        assertThrows(IllegalArgumentException.class, () -> new JdkA2AHttpClient(null), "foo");
    }

    private static final class TrackingProxySelector extends ProxySelector {
        private final AtomicInteger selectCount = new AtomicInteger();

        @Override
        public List<Proxy> select(URI uri) {
            selectCount.incrementAndGet();
            return List.of(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            throw new AssertionError("Proxy connection should not fail in this test", ioe);
        }
    }
}
