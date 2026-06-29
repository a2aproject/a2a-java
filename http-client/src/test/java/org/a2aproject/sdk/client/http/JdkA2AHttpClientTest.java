package org.a2aproject.sdk.client.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void testCancellationExceptionViaSubscriberOnErrorIsNotPropagated() throws Exception {
        AtomicReference<Throwable> capturedError = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        CountDownLatch errorPathReached = new CountDownLatch(1);

        HttpClient fakeClient = new StubHttpClient() {
            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> handler) {
                HttpResponse.BodySubscriber<T> bodySubscriber =
                        handler.apply(new FakeResponseInfo(200, "text/plain"));
                bodySubscriber.onSubscribe(new NoOpSubscription());
                bodySubscriber.onError(new CancellationException());
                errorPathReached.countDown();
                return new CompletableFuture<>(); // never completes
            }
        };

        new JdkA2AHttpClient(fakeClient)
                .createGet()
                .url("http://example.com/sse")
                .getAsyncSSE(event -> {}, e -> capturedError.set(e), () -> completed.set(true));

        assertTrue(errorPathReached.await(5, TimeUnit.SECONDS));
        assertNull(capturedError.get(), "CancellationException should not reach the error consumer");
        assertFalse(completed.get(), "Complete handler must not be called after cancellation");
    }

    @Test
    public void testCancellationExceptionViaFutureFailureIsNotPropagated() throws Exception {
        AtomicReference<Throwable> capturedError = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        HttpClient fakeClient = new StubHttpClient() {
            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> handler) {
                CompletableFuture<HttpResponse<T>> future = new CompletableFuture<>();
                future.completeExceptionally(new CancellationException());
                return future;
            }
        };

        CompletableFuture<Void> result = new JdkA2AHttpClient(fakeClient)
                .createGet()
                .url("http://example.com/sse")
                .getAsyncSSE(event -> {}, e -> capturedError.set(e), () -> completed.set(true));

        result.get(5, TimeUnit.SECONDS);
        assertNull(capturedError.get(), "CancellationException should not reach the error consumer");
        assertFalse(completed.get(), "Complete handler must not be called after cancellation");
    }

    @Test
    public void testRealErrorsAreStillPropagatedToErrorConsumer() throws Exception {
        AtomicReference<Throwable> capturedError = new AtomicReference<>();
        CountDownLatch errorLatch = new CountDownLatch(1);
        IOException expectedError = new IOException("connection refused");

        HttpClient fakeClient = new StubHttpClient() {
            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request, HttpResponse.BodyHandler<T> handler) {
                CompletableFuture<HttpResponse<T>> future = new CompletableFuture<>();
                future.completeExceptionally(expectedError);
                return future;
            }
        };

        new JdkA2AHttpClient(fakeClient)
                .createGet()
                .url("http://example.com/sse")
                .getAsyncSSE(event -> {}, e -> { capturedError.set(e); errorLatch.countDown(); }, () -> {});

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(capturedError.get());
        assertEquals(expectedError, capturedError.get());
    }

    private abstract static class StubHttpClient extends HttpClient {
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public HttpClient.Redirect followRedirects() { return HttpClient.Redirect.NORMAL; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { throw new UnsupportedOperationException(); }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
        @Override public <T> HttpResponse<T> send(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { return sendAsync(r, h); }
        @Override public WebSocket.Builder newWebSocketBuilder() { throw new UnsupportedOperationException(); }
    }

    private static final class FakeResponseInfo implements HttpResponse.ResponseInfo {
        private final int statusCode;
        private final HttpHeaders headers;

        FakeResponseInfo(int statusCode, String contentType) {
            this.statusCode = statusCode;
            this.headers = HttpHeaders.of(Map.of("Content-Type", List.of(contentType)), (k, v) -> true);
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpHeaders headers() { return headers; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    private static final class NoOpSubscription implements Flow.Subscription {
        @Override public void request(long n) {}
        @Override public void cancel() {}
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
