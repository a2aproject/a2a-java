package org.a2aproject.sdk.client.http;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.util.Assert;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vert.x WebClient-based implementation of {@link A2AHttpClient}.
 *
 * <p>
 * This implementation uses Vert.x's reactive HTTP client to execute requests.
 * For synchronous methods ({@link GetBuilder#get()}, {@link PostBuilder#post()}, {@link DeleteBuilder#delete()}),
 * the implementation blocks the calling thread until the asynchronous operation completes.
 * For SSE streaming methods, the implementation returns immediately with a
 * {@link CompletableFuture} and streams events asynchronously via callbacks.
 *
 * <h2>Lifecycle Management</h2>
 * <p>
 * This client implements {@link AutoCloseable} and should be closed when no longer needed:
 * <pre>{@code
 * try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
 *     A2AHttpResponse response = client.createGet()
 *         .url("https://example.com/api")
 *         .get();
 *     // Use response
 * }
 * }</pre>
 *
 * <p>
 * If constructed with the no-args constructor, the client creates and owns a
 * {@link Vertx} instance which will be closed when {@link #close()} is called.
 * If constructed with an external {@link Vertx} instance, only the WebClient is
 * closed, leaving the Vertx instance management to the caller.
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This client is thread-safe. Multiple threads can create and execute requests
 * concurrently. However, individual builder instances are NOT thread-safe and should
 * not be shared across threads.
 *
 * <h2>HTTP/2 Support</h2>
 * <p>
 * Vert.x WebClient automatically negotiates HTTP/2 when supported by the server
 * via ALPN. No explicit configuration is required.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple GET Request</h3>
 * <pre>{@code
 * try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
 *     A2AHttpResponse response = client.createGet()
 *         .url("https://api.example.com/data")
 *         .addHeader("Authorization", "Bearer token")
 *         .get();
 *
 *     if (response.success()) {
 *         System.out.println(response.body());
 *     }
 * }
 * }</pre>
 *
 * <h3>POST Request with JSON Body</h3>
 * <pre>{@code
 * try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
 *     A2AHttpResponse response = client.createPost()
 *         .url("https://api.example.com/submit")
 *         .addHeader("Content-Type", "application/json")
 *         .body("{\"key\":\"value\"}")
 *         .post();
 *
 *     System.out.println("Status: " + response.status());
 * }
 * }</pre>
 *
 * <h3>Async SSE Streaming</h3>
 * <pre>{@code
 * try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
 *     CompletableFuture<Void> future = client.createGet()
 *         .url("https://api.example.com/stream")
 *         .getAsyncSSE(
 *             event -> System.out.println("Received: " + event.data()),
 *             error -> error.printStackTrace(),
 *             () -> System.out.println("Stream complete")
 *         );
 *
 *     // Do other work while streaming...
 *     future.join(); // Wait for completion if needed
 * }
 * }</pre>
 */
public class VertxA2AHttpClient implements A2AHttpClient, AutoCloseable {

    private final Vertx vertx;
    private final WebClient webClient;
    private final HttpClient httpClient;
    private boolean ownsVertx;
    private static final Logger log = Logger.getLogger(VertxA2AHttpClient.class.getName());

    /**
     * Creates a new VertxA2AHttpClient with an internally managed Vert.x instance.
     *
     * <p>
     * The client creates a new {@link Vertx} instance and {@link WebClient} configured
     * with HTTP keep-alive and automatic redirect following. When {@link #close()} is called,
     * both the WebClient and Vertx instance are closed.
     *
     * <p>
     * <strong>Important:</strong> Always call {@link #close()} when done with this client
     * to prevent resource leaks.
     *
     * @see #VertxA2AHttpClient(Vertx) for using an externally managed Vertx instance
     */
    public VertxA2AHttpClient() {
        this.vertx = createVertx();
        WebClientOptions options = new WebClientOptions()
                .setFollowRedirects(true)
                .setKeepAlive(true);
        this.webClient = WebClient.create(vertx, options);
        this.httpClient = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(true));
       log.fine("Vert.x client is ready.");
    }

    private Vertx createVertx() {
        try {
            BeanManager beanManager = CDI.current().getBeanManager();
            Set<Bean<?>> beans = beanManager.getBeans(Vertx.class);
            if (beans != null && !beans.isEmpty()) {
                this.ownsVertx = false;
                Bean<?> bean = beans.iterator().next();
                CreationalContext<?> context = beanManager.createCreationalContext(bean);
                return (Vertx) beanManager.getReference(bean, Vertx.class, context);
            }
        } catch (Exception ex) {
            log.log(Level.FINE, "Error loading vertx from CDI error details", ex);
        }
        this.ownsVertx = true;
        return Vertx.vertx();
    }

    /**
     * Creates a new VertxA2AHttpClient using an externally managed Vert.x instance.
     *
     * <p>
     * The client creates a {@link WebClient} using the provided {@link Vertx} instance.
     * When {@link #close()} is called, only the WebClient is closed; the Vertx instance
     * remains open and must be managed by the caller.
     *
     * <p>
     * This constructor is useful in environments where Vert.x is already managed,
     * such as Quarkus applications.
     *
     * @param vertx the Vert.x instance to use; must not be null
     * @throws IllegalArgumentException if vertx is null
     */
    public VertxA2AHttpClient(Vertx vertx) {
        this.vertx = Assert.checkNotNullParam("vertx", vertx);
        this.ownsVertx = false;
        WebClientOptions options = new WebClientOptions()
                .setFollowRedirects(true)
                .setKeepAlive(true);
        this.webClient = WebClient.create(vertx, options);
        this.httpClient = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(true));
        log.fine("Vert.x client is ready.");
    }

    /**
     * Closes this HTTP client and releases associated resources.
     *
     * <p>
     * This method always closes the WebClient. If the client was created with the
     * no-args constructor (and thus owns the Vert.x instance), the Vertx instance is
     * also closed. Otherwise, the Vertx instance is left open for the caller to manage.
     */
    @Override
    public void close() {
        webClient.close();
        httpClient.close();
        if (ownsVertx) {
            vertx.close();
        }
    }

    @Override
    public GetBuilder createGet() {
        return new VertxGetBuilder();
    }

    @Override
    public PostBuilder createPost() {
        return new VertxPostBuilder();
    }

    @Override
    public DeleteBuilder createDelete() {
        return new VertxDeleteBuilder();
    }

    private abstract class VertxBuilder<T extends Builder<T>> implements Builder<T> {

        protected String url = "";
        protected Map<String, String> headers = new HashMap<>();

        @Override
        public T url(String url) {
            this.url = url;
            return self();
        }

        @Override
        public T addHeader(String name, String value) {
            headers.put(name, value);
            return self();
        }

        @Override
        public T addHeaders(Map<String, String> headers) {
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    addHeader(entry.getKey(), entry.getValue());
                }
            }
            return self();
        }

        @SuppressWarnings("unchecked")
        T self() {
            return (T) this;
        }
    }

    /**
     * Common method to execute synchronous HTTP requests (GET, POST, DELETE).
     *
     * @param request the HTTP request configured with method and URL
     * @param headers custom headers to add to the request
     * @param bodyBuffer optional body buffer for POST requests (null for GET/DELETE)
     * @return the HTTP response
     * @throws IOException if the request fails; the cause may be an
     *         {@link org.a2aproject.sdk.spec.A2AClientHTTPError} for HTTP 401/403 responses
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private A2AHttpResponse executeSyncRequest(
            HttpRequest<Buffer> request,
            Map<String, String> headers,
            @Nullable Buffer bodyBuffer) throws IOException, InterruptedException {

        // Add headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<A2AHttpResponse> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Send with or without body
        if (bodyBuffer != null) {
            request.sendBuffer(bodyBuffer, ar -> handleResponse(ar, responseRef, errorRef, latch));
        } else {
            request.send(ar -> handleResponse(ar, responseRef, errorRef, latch));
        }

        latch.await();

        if (errorRef.get() != null) {
            Throwable error = errorRef.get();
            if (error instanceof IOException) {
                throw (IOException) error;
            } 
            if (error instanceof InterruptedException) {
                throw (InterruptedException) error;
            }
            throw new IOException("Request failed", error);
        }
        A2AHttpResponse finalResponse = responseRef.get();
        if(finalResponse == null) {
            throw new IllegalStateException("No response from http request");
        }
        return finalResponse;
    }

    /**
     * Handles the HTTP response callback, checking for auth errors and populating response/error refs.
     */
    private void handleResponse(
            io.vertx.core.AsyncResult<HttpResponse<Buffer>> ar,
            AtomicReference<A2AHttpResponse> responseRef,
            AtomicReference<Throwable> errorRef,
            CountDownLatch latch) {

        if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            int status = response.statusCode();

            switch (status) {
                case HTTP_UNAUTHORIZED -> {
                    A2AHttpHeaders headers = fromVertxHeaders(response.headers());
                    errorRef.set(new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
                            new org.a2aproject.sdk.spec.A2AClientHTTPError(
                                    HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED, null, headers.toMap())));
                }
                case HTTP_FORBIDDEN -> {
                    A2AHttpHeaders headers = fromVertxHeaders(response.headers());
                    errorRef.set(new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
                            new org.a2aproject.sdk.spec.A2AClientHTTPError(
                                    HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED, null, headers.toMap())));
                }
                default -> {
                    String body = response.bodyAsString();
                    A2AHttpHeaders headers = fromVertxHeaders(response.headers());
                    responseRef.set(new VertxHttpResponse(status, body != null ? body : "", headers));
                }
            }
        } else {
            errorRef.set(ar.cause());
        }
        latch.countDown();
    }

    /**
     * Common method to execute async SSE requests (GET or POST).
     *
     * <p>Uses the lower-level {@link HttpClient} so that the response status and
     * {@code Content-Type} header are available before any body bytes flow.  The
     * response is paused immediately on arrival; the appropriate body handler is then
     * wired up and the response is resumed via {@code pipe().to(...)}.
     *
     * @param httpMethod the HTTP method (GET or POST)
     * @param url the absolute request URL
     * @param headers custom headers to add to the request
     * @param bodyBuffer optional body buffer for POST requests (null for GET)
     * @param messageConsumer callback for each SSE message received
     * @param errorConsumer callback for errors
     * @param completeRunnable callback when stream completes successfully
     * @return CompletableFuture that completes when the stream ends
     */
    private CompletableFuture<Void> executeAsyncSSE(
            HttpMethod httpMethod,
            String url,
            Map<String, String> headers,
            @Nullable Buffer bodyBuffer,
            Consumer<ServerSentEvent> messageConsumer,
            Consumer<Throwable> errorConsumer,
            Runnable completeRunnable) {

        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicBoolean futureCompleted = new AtomicBoolean(false);

        RequestOptions options = new RequestOptions()
                .setAbsoluteURI(url)
                .setMethod(httpMethod)
                .addHeader(ACCEPT, EVENT_STREAM);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            options.addHeader(entry.getKey(), entry.getValue());
        }

        httpClient.request(options)
                .compose(req -> bodyBuffer != null ? req.send(bodyBuffer) : req.send())
                .onSuccess(response -> {
                    // Pause before inspecting headers so no body bytes are lost while we
                    // set up the appropriate handler. pipe().to(...) will resume the response.
                    response.pause();
                    int statusCode = response.statusCode();
                    if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
                        if (futureCompleted.compareAndSet(false, true)) {
                            A2AHttpHeaders respHeaders = fromVertxHeaders(response.headers());
                            String msg = statusCode == HTTP_UNAUTHORIZED
                                    ? A2AErrorMessages.AUTHENTICATION_FAILED
                                    : A2AErrorMessages.AUTHORIZATION_FAILED;
                            errorConsumer.accept(new IOException(msg,
                                    new org.a2aproject.sdk.spec.A2AClientHTTPError(
                                            statusCode, msg, null, respHeaders.toMap())));
                            future.complete(null);
                        }
                        return;
                    }
                    String contentType = response.getHeader("Content-Type");
                    boolean isSse = statusCode >= HTTP_OK && statusCode < HTTP_MULT_CHOICE
                            && contentType != null && contentType.contains(EVENT_STREAM);
                    if (isSse) {
                        BodyCodec.sseStream(readStream ->
                            readStream.handler(sseEvent -> {
                                String data = sseEvent.data();
                                if (data != null && !data.isEmpty()) {
                                    String eventType = sseEvent.event() != null ? sseEvent.event() : ServerSentEvent.DEFAULT_EVENT_TYPE;
                                    // Vert.x SseEvent.retry() defaults to 0 when no retry field is present, so
                                    // retry:0 (a valid SSE directive meaning "reconnect immediately") is
                                    // indistinguishable from "not set" and is silently treated as absent.
                                    Long retry = sseEvent.retry() != 0 ? (long) sseEvent.retry() : null;
                                    messageConsumer.accept(new ServerSentEvent(data, eventType, sseEvent.id(), retry));
                                }
                            })
                        ).create(ar -> {
                            if (ar.failed()) {
                                if (futureCompleted.compareAndSet(false, true)) {
                                    errorConsumer.accept(ar.cause());
                                    future.complete(null);
                                }
                                return;
                            }
                            response.pipe().to(ar.result())
                                    .onSuccess(v -> {
                                        if (futureCompleted.compareAndSet(false, true)) {
                                            completeRunnable.run();
                                            future.complete(null);
                                        }
                                    })
                                    .onFailure(cause -> {
                                        if (futureCompleted.compareAndSet(false, true)) {
                                            errorConsumer.accept(cause);
                                            future.complete(null);
                                        }
                                    });
                        });
                    } else {
                        // Non-SSE response (error body): deliver lines to messageConsumer so
                        // the SSEEventListener up the call stack can parse the JSON-RPC error.
                        response.pipe().to(new PlainBodyWriteStream(messageConsumer))
                                .onSuccess(v -> {
                                    if (futureCompleted.compareAndSet(false, true)) {
                                        completeRunnable.run();
                                        future.complete(null);
                                    }
                                })
                                .onFailure(cause -> {
                                    if (futureCompleted.compareAndSet(false, true)) {
                                        errorConsumer.accept(cause);
                                        future.complete(null);
                                    }
                                });
                    }
                })
                .onFailure(cause -> {
                    if (futureCompleted.compareAndSet(false, true)) {
                        errorConsumer.accept(cause);
                        future.complete(null);
                    }
                });

        return future;
    }

    private class VertxGetBuilder extends VertxBuilder<GetBuilder> implements A2AHttpClient.GetBuilder {

        /**
         * {@inheritDoc}
         *
         * <p>
         * <strong>Implementation Note:</strong> This method blocks the calling thread until
         * the asynchronous HTTP request completes. The underlying Vert.x operation executes
         * asynchronously on the Vert.x event loop.
         *
         * @throws IOException if the request fails, including network errors, HTTP 401, and HTTP 403;
         *         the cause may be an {@link org.a2aproject.sdk.spec.A2AClientHTTPError} carrying the response headers
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        @Override
        public A2AHttpResponse get() throws IOException, InterruptedException {
            return executeSyncRequest(webClient.getAbs(url), headers, null);
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {

            return executeAsyncSSE(HttpMethod.GET, url, headers, null, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private class VertxPostBuilder extends VertxBuilder<PostBuilder> implements A2AHttpClient.PostBuilder {

        private String body = "";

        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return self();
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * <strong>Implementation Note:</strong> This method blocks the calling thread until
         * the asynchronous HTTP request completes. The underlying Vert.x operation executes
         * asynchronously on the Vert.x event loop.
         *
         * @throws IOException if the request fails, including network errors, HTTP 401, and HTTP 403;
         *         the cause may be an {@link org.a2aproject.sdk.spec.A2AClientHTTPError} carrying the response headers
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            Buffer bodyBuffer = Buffer.buffer(body, StandardCharsets.UTF_8.name());
            return executeSyncRequest(webClient.postAbs(url), headers, bodyBuffer);
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {

            Buffer bodyBuffer = Buffer.buffer(body, StandardCharsets.UTF_8.name());
            return executeAsyncSSE(HttpMethod.POST, url, headers, bodyBuffer, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private class VertxDeleteBuilder extends VertxBuilder<DeleteBuilder> implements A2AHttpClient.DeleteBuilder {

        /**
         * {@inheritDoc}
         *
         * <p>
         * <strong>Implementation Note:</strong> This method blocks the calling thread until
         * the asynchronous HTTP request completes. The underlying Vert.x operation executes
         * asynchronously on the Vert.x event loop.
         *
         * @throws IOException if the request fails, including network errors, HTTP 401, and HTTP 403;
         *         the cause may be an {@link org.a2aproject.sdk.spec.A2AClientHTTPError} carrying the response headers
         * @throws InterruptedException if the thread is interrupted while waiting
         */
        @Override
        public A2AHttpResponse delete() throws IOException, InterruptedException {
            return executeSyncRequest(webClient.deleteAbs(url), headers, null);
        }
    }

    /**
     * A {@link WriteStream} that handles plain (non-SSE) response bodies, e.g. JSON error
     * responses returned when the stream never opens. Accumulates all bytes and emits the
     * entire body as a single {@link ServerSentEvent} on {@link #end} so that multi-line or
     * pretty-printed JSON is delivered as one parseable unit to the SSEEventListener.
     *
     * <p>A hard cap of {@value #MAX_BUFFER_BYTES} bytes is enforced on the internal buffer
     * to prevent Denial-of-Service via {@link OutOfMemoryError} for arbitrarily large inputs.
     */
    private static class PlainBodyWriteStream implements WriteStream<Buffer> {
        /** Maximum number of raw bytes that may be buffered before further writes are rejected. */
        private static final int MAX_BUFFER_BYTES = 1024 * 1024; // 1 MB

        private final Consumer<ServerSentEvent> messageConsumer;
        private Buffer rawBuffer = Buffer.buffer();
        private @Nullable Handler<Throwable> exceptionHandler;

        PlainBodyWriteStream(Consumer<ServerSentEvent> messageConsumer) {
            this.messageConsumer = messageConsumer;
        }

        @Override
        public Future<Void> write(Buffer data) {
            if (rawBuffer.length() + data.length() > MAX_BUFFER_BYTES) {
                IllegalStateException ex = new IllegalStateException(
                        "Response body exceeded maximum allowed size of " + MAX_BUFFER_BYTES + " bytes");
                Handler<Throwable> eh = exceptionHandler;
                if (eh != null) {
                    eh.handle(ex);
                }
                return Future.failedFuture(ex);
            }
            rawBuffer.appendBuffer(data);
            return Future.succeededFuture();
        }

        @Override
        public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
            Future<Void> result = write(data);
            if (handler != null) {
                handler.handle(result);
            }
        }

        @Override
        public void end(Handler<AsyncResult<Void>> handler) {
            if (rawBuffer.length() > 0) {
                String body = rawBuffer.toString(StandardCharsets.UTF_8).trim();
                rawBuffer = Buffer.buffer();
                if (!body.isEmpty()) {
                    messageConsumer.accept(new ServerSentEvent(body));
                }
            }
            if (handler != null) {
                handler.handle(Future.succeededFuture());
            }
        }

        @Override
        public WriteStream<Buffer> exceptionHandler(@Nullable Handler<Throwable> handler) {
            this.exceptionHandler = handler;
            return this;
        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
            return this;
        }

        @Override
        public boolean writeQueueFull() {
            return false;
        }

        @Override
        public WriteStream<Buffer> drainHandler(@Nullable Handler<Void> handler) {
            return this;
        }
    }

    private static A2AHttpHeaders fromVertxHeaders(io.vertx.core.MultiMap headers) {
        Map<String, List<String>> snapshot = new HashMap<>();
        for (String name : headers.names()) {
            snapshot.put(name, headers.getAll(name));
        }
        return A2AHttpHeaders.of(snapshot);
    }

    private record VertxHttpResponse(int status, String body, A2AHttpHeaders headers) implements A2AHttpResponse {

        @Override
        public int status() {
            return status;
        }

        @Override
        public boolean success() {
            return status >= HTTP_OK && status < HTTP_MULT_CHOICE;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public A2AHttpHeaders headers() {
            return headers;
        }
    }
}
