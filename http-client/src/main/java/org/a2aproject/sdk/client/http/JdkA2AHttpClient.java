package org.a2aproject.sdk.client.http;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.spec.A2AClientHTTPError;

/**
 * Default HTTP client implementation using JDK 11+ {@link HttpClient}.
 *
 * <p>This is the fallback implementation used when no higher-priority
 * {@link A2AHttpClientProvider} is available. It provides full support for:
 * <ul>
 *   <li>HTTP/2 with automatic fallback to HTTP/1.1</li>
 *   <li>Synchronous GET, POST, and DELETE requests</li>
 *   <li>Asynchronous Server-Sent Events (SSE) streaming</li>
 * </ul>
 *
 * <p><b>Security Note:</b> The default client does not follow HTTP redirects
 * automatically to prevent credential leakage to third-party origins. To enable
 * redirect following for a POST request, supply a redirect-capable {@link HttpClient}
 * via {@link #JdkA2AHttpClient(HttpClient)} <em>and</em> call
 * {@link org.a2aproject.sdk.client.http.A2AHttpClient.PostBuilder#followRedirects(boolean)
 * followRedirects(true)} on the builder. Both steps are required.
 *
 * <p><b>{@code Host} header limitation:</b> the underlying JDK {@link HttpClient}
 * rejects {@code addHeader("Host", ...)} with an {@code IllegalArgumentException}
 * ("restricted header name") unless the JVM is started with
 * {@code -Djdk.httpclient.allowRestrictedHeaders=host}. This is a JDK-wide
 * restriction that cannot be worked around per-client-instance. Applications that
 * need to send an explicit {@code Host} header (for example, connecting directly to
 * an instance while presenting the hostname a load balancer normally supplies) should
 * either set that system property or use an {@link A2AHttpClient} implementation that
 * does not build on {@code java.net.http.HttpClient}, such as
 * {@code org.a2aproject.sdk.client.http.vertx.VertxA2AHttpClient} (the
 * {@code a2a-java-sdk-http-client-vertx} extra), which has no such restriction.
 *
 * <p><b>Provider Priority:</b> 0 (lowest - used as fallback)
 *
 * <p>This implementation is registered via {@link JdkA2AHttpClientProvider}
 * in the ServiceLoader system and is automatically used by {@link A2AHttpClientFactory}
 * when no other provider is available.
 *
 * @see A2AHttpClient
 * @see A2AHttpClientFactory
 * @see JdkA2AHttpClientProvider
 */
public class JdkA2AHttpClient implements A2AHttpClient {

    private final HttpClient httpClient;
    private final SSEParserConfig sseParserConfig;
    private volatile @Nullable HttpClient noRedirectClient;

    /**
     * Creates a new JDK-based HTTP client with secure defaults.
     *
     * <p>Configures the client with:
     * <ul>
     *   <li>HTTP/2 preferred (with HTTP/1.1 fallback)</li>
     *   <li>No automatic redirect following (security hardening to prevent credential leakage)</li>
     * </ul>
     *
     * <p>To enable redirect following for POST requests, use
     * {@link #JdkA2AHttpClient(HttpClient)} with a redirect-capable {@link HttpClient}
     * <em>and</em> call {@link PostBuilder#followRedirects(boolean) followRedirects(true)}
     * on each {@link PostBuilder}. Both steps are required: the builder flag selects
     * which underlying client is used; supplying a redirect-capable client alone has
     * no effect if the flag is not set.
     */
    public JdkA2AHttpClient() {
        this(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), SSEParserConfig.DEFAULT);
    }

    /**
     * Creates a new JDK-based HTTP client with secure defaults and custom SSE parser limits.
     *
     * <p>Named factory method avoids overload ambiguity with {@link #JdkA2AHttpClient(HttpClient)}.
     *
     * @param sseParserConfig the SSE parser configuration to use for streaming responses
     * @return a new JdkA2AHttpClient with the given SSE parser configuration
     * @throws IllegalArgumentException if {@code sseParserConfig} is {@code null}
     */
    public static JdkA2AHttpClient withSseConfig(SSEParserConfig sseParserConfig) {
        return new JdkA2AHttpClient(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                sseParserConfig);
    }

    /**
     * Creates a new JDK-based HTTP client using a caller-provided JDK {@link HttpClient}.
     *
     * <p>This constructor allows full control over the {@link HttpClient} configuration.
     * The caller is responsible for ensuring the client is configured securely.
     *
     * <p><strong>POST redirect opt-in requires two steps:</strong> this client is only
     * used for a POST request when {@link PostBuilder#followRedirects(boolean)
     * followRedirects(true)} is also called on the builder. If the flag is not set the
     * builder uses an internal no-redirect client regardless of the policy configured
     * on the {@code httpClient} supplied here.
     *
     * @param httpClient the JDK HTTP client to delegate requests to
     * @throws IllegalArgumentException if {@code httpClient} is {@code null}
     */
    public JdkA2AHttpClient(HttpClient httpClient) {
        this(httpClient, SSEParserConfig.DEFAULT);
    }

    /**
     * Creates a new JDK-based HTTP client using a caller-provided JDK {@link HttpClient}
     * and custom SSE parser limits.
     *
     * @param httpClient the JDK HTTP client to delegate requests to
     * @param sseParserConfig the SSE parser configuration to use for streaming responses
     * @throws IllegalArgumentException if {@code httpClient} or {@code sseParserConfig} is {@code null}
     */
    public JdkA2AHttpClient(HttpClient httpClient, SSEParserConfig sseParserConfig) {
        this.httpClient = checkNotNullParam("httpClient", httpClient);
        this.sseParserConfig = checkNotNullParam("sseParserConfig", sseParserConfig);
    }

    @Override
    public GetBuilder createGet() {
        return new JdkGetBuilder();
    }

    @Override
    public PostBuilder createPost() {
        return new JdkPostBuilder();
    }

    @Override
    public DeleteBuilder createDelete() {
        return new JdkDeleteBuilder();
    }

    private static boolean isCancellation(Throwable t) {
        for (Throwable current = t; current != null; current = current.getCause()) {
            if (current instanceof java.util.concurrent.CancellationException) {
                return true;
            }
        }
        return false;
    }

    private abstract class JdkBuilder<T extends Builder<T>> implements Builder<T> {
        private String url = "";
        private Map<String, String> headers = new HashMap<>();

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
            if(headers != null && ! headers.isEmpty()) {
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

        protected HttpRequest.Builder createRequestBuilder() throws IOException {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url));
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
            return builder;
        }

        protected CompletableFuture<Void> asyncRequest(
                HttpRequest request,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) {
            return asyncRequest(httpClient, request, messageConsumer, errorConsumer, completeRunnable);
        }

        protected CompletableFuture<Void> asyncRequest(
                HttpClient client,
                HttpRequest request,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable
        ) {
            ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer, sseParserConfig);
            AtomicBoolean useSseParser = new AtomicBoolean(false);
            AtomicBoolean errorNotified = new AtomicBoolean(false);
            StringBuilder nonSseBodyBuffer = new StringBuilder();

            Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
                private Flow.@Nullable Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1);
                }

                @Override
                public void onNext(String item) {
                    if (item != null) {
                        if (useSseParser.get()) {
                            sseParser.processLine(item);
                        } else {
                            // Preserve blank lines so JSON string values containing literal newlines survive intact
                            if (nonSseBodyBuffer.length() > 0) {
                                nonSseBodyBuffer.append('\n');
                            }
                            nonSseBodyBuffer.append(item);
                        }
                    }
                    if (subscription != null) {
                        subscription.request(1);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (errorNotified.compareAndSet(false, true)) {
                        if (!isCancellation(throwable)) {
                            errorConsumer.accept(throwable);
                        }
                    }
                }

                @Override
                public void onComplete() {
                    if (errorNotified.compareAndSet(false, true)) {
                        if (useSseParser.get()) {
                            sseParser.flush();
                        } else {
                            String body = nonSseBodyBuffer.toString();
                            if (!body.isEmpty()) {
                                messageConsumer.accept(new ServerSentEvent(body));
                            }
                        }
                        completeRunnable.run();
                    }
                }
            };

            // Create a custom body handler that checks status and Content-Type before processing body
            BodyHandler<Void> bodyHandler = responseInfo -> {
                // Check for authentication/authorization errors only
                if (responseInfo.statusCode() == HTTP_UNAUTHORIZED || responseInfo.statusCode() == HTTP_FORBIDDEN) {
                    final int statusCode = responseInfo.statusCode();
                    final String errorMessage = statusCode == HTTP_UNAUTHORIZED
                            ? A2AErrorMessages.AUTHENTICATION_FAILED
                            : A2AErrorMessages.AUTHORIZATION_FAILED;
                    final A2AClientHTTPError httpError = new A2AClientHTTPError(
                            statusCode, errorMessage, null, responseInfo.headers().map());
                    // Return a body subscriber that immediately signals error
                    return BodySubscribers.fromSubscriber(new Flow.Subscriber<List<ByteBuffer>>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {
                            subscriber.onError(new IOException(errorMessage, httpError));
                        }

                        @Override
                        public void onNext(List<ByteBuffer> item) {
                            // Should not be called
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // Should not be called
                        }

                        @Override
                        public void onComplete() {
                            // Should not be called
                        }
                    });
                }
                // Use SSE parser only for actual SSE responses; plain body lines are forwarded directly
                String contentType = responseInfo.headers().firstValue("Content-Type").orElse("");
                boolean isSse = JdkHttpResponse.success(responseInfo.statusCode())
                        && contentType.contains(EVENT_STREAM);
                useSseParser.set(isSse);
                if (isSse) {
                    // Use a bounded byte-level subscriber so that the maxLineLength limit is
                    // enforced *before* a full line is materialised in memory, preventing memory
                    // exhaustion from a malicious server that never sends a newline.
                    return new BoundedLineBodySubscriber(subscriber, sseParserConfig.maxLineLength(), sseParser);
                }
                return BodyHandlers.fromLineSubscriber(subscriber).apply(responseInfo);
            };

            // Send the response async, and let the subscriber handle the lines.
            // handle() catches failures before the body handler is reached (e.g. connection
            // errors, DNS failures) and routes them to errorConsumer, then always completes
            // normally — consistent with the Vert.x and Android implementations.
            return client.sendAsync(request, bodyHandler)
                    .<Void>handle((response, throwable) -> {
                        if (throwable != null && errorNotified.compareAndSet(false, true)) {
                            if (!isCancellation(throwable)) {
                                Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                                errorConsumer.accept(cause);
                            }
                        }
                        return null;
                    });
        }
    }

    private class JdkGetBuilder extends JdkBuilder<GetBuilder> implements A2AHttpClient.GetBuilder {

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().GET();
            if (SSE) {
                builder.header(ACCEPT, EVENT_STREAM);
            }
            return builder;
        }

        @Override
        public A2AHttpResponse get() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == HTTP_UNAUTHORIZED) {
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
                        new A2AClientHTTPError(HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED,
                                null, response.headers().map()));
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
                        new A2AClientHTTPError(HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED,
                                null, response.headers().map()));
            }

            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(true)
                    .build();
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
        }

    }

    private class JdkDeleteBuilder extends JdkBuilder<DeleteBuilder> implements A2AHttpClient.DeleteBuilder {

        @Override
        public A2AHttpResponse delete() throws IOException, InterruptedException {
            HttpRequest request = super.createRequestBuilder().DELETE().build();
            HttpResponse<String> response =
                    httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == HTTP_UNAUTHORIZED) {
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
                        new A2AClientHTTPError(HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED,
                                null, response.headers().map()));
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
                        new A2AClientHTTPError(HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED,
                                null, response.headers().map()));
            }

            return new JdkHttpResponse(response);
        }

    }

    private HttpClient getNoRedirectClient() {
        HttpClient local = noRedirectClient;
        if (local == null) {
            synchronized (this) {
                local = noRedirectClient;
                if (local == null) {
                    local = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .build();
                    noRedirectClient = local;
                }
            }
        }
        return local;
    }

    private class JdkPostBuilder extends JdkBuilder<PostBuilder> implements A2AHttpClient.PostBuilder {
        String body = "";
        boolean followRedirects = false;

        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return self();
        }

        @Override
        public PostBuilder followRedirects(boolean follow) {
            this.followRedirects = follow;
            return self();
        }

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (SSE) {
                builder.header(ACCEPT, EVENT_STREAM);
            }
            return builder;
        }

        private HttpClient resolveClient() {
            return followRedirects ? httpClient : getNoRedirectClient();
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            HttpResponse<String> response =
                    resolveClient().send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == HTTP_UNAUTHORIZED) {
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
                        new A2AClientHTTPError(HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED,
                                null, response.headers().map()));
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
                        new A2AClientHTTPError(HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED,
                                null, response.headers().map()));
            }

            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(true)
                    .build();
            return super.asyncRequest(resolveClient(), request, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private record JdkHttpResponse(HttpResponse<String> response) implements A2AHttpResponse {

        @Override
        public int status() {
            return response.statusCode();
        }

        @Override
        public boolean success() {
            return success(response.statusCode());
        }

        static boolean success(int statusCode) {
            return statusCode >= HTTP_OK && statusCode < HTTP_MULT_CHOICE;
        }

        @Override
        public String body() {
            return response.body();
        }

        @Override
        public A2AHttpHeaders headers() {
            return A2AHttpHeaders.of(response.headers().map());
        }
    }

    /**
     * A {@link HttpResponse.BodySubscriber} that splits the raw byte stream into lines and
     * delivers each complete line to a {@link Flow.Subscriber}{@code <String>}.
     *
     * <p>Unlike {@link java.net.http.HttpResponse.BodyHandlers#fromLineSubscriber}, this
     * implementation enforces {@code maxLineBytes} <em>while accumulating bytes</em>, so a
     * malicious unterminated line cannot exhaust heap before the limit is applied.
     *
     * <p>When {@code maxLineBytes <= 0} the per-line byte cap is disabled, matching the
     * semantics of {@link SSEParserConfig#maxLineLength()} == 0.
     */
    private static final class BoundedLineBodySubscriber implements HttpResponse.BodySubscriber<Void> {

        private final Flow.Subscriber<String> lineSubscriber;
        private final ServerSentEventParser sseParser;
        private final CompletableFuture<Void> result = new CompletableFuture<>();
        private final BoundedLineAccumulator accumulator;
        /** True when a bare CR was the last byte of the previous chunk (may start a CRLF pair). */
        private boolean pendingCR = false;

        BoundedLineBodySubscriber(Flow.Subscriber<String> lineSubscriber, int maxLineBytes,
                ServerSentEventParser sseParser) {
            this.lineSubscriber = lineSubscriber;
            this.sseParser = sseParser;
            this.accumulator = new BoundedLineAccumulator(maxLineBytes);
        }

        @Override
        public CompletableFuture<Void> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            // Request all data from the HTTP body publisher up-front — the JDK HTTP
            // layer is not back-pressure sensitive for this use-case, and mirroring what
            // BodyHandlers.fromLineSubscriber does keeps behaviour consistent.
            subscription.request(Long.MAX_VALUE);
            // Give the line subscriber a no-op subscription: it cannot cancel the HTTP
            // connection from the line level, and back-pressure is already handled above.
            lineSubscriber.onSubscribe(new Flow.Subscription() {
                @Override public void request(long n) { /* no-op */ }
                @Override public void cancel() { /* no-op */ }
            });
        }

        private record TerminatorScan(int lineLen, int resumePos, boolean crAtBoundary) {}

        @Override
        public void onNext(List<ByteBuffer> items) {
            for (ByteBuffer buf : items) {
                processBuffer(buf);
            }
        }

        private void processBuffer(ByteBuffer buf) {
            while (buf.hasRemaining()) {
                if (consumePendingCR(buf)) {
                    break;
                }

                int start = buf.position();
                TerminatorScan scan = scanForTerminator(buf, start);

                if (scan == null) {
                    accumulateRemainingBytes(buf, start);
                } else {
                    pendingCR = scan.crAtBoundary();
                    buf.position(scan.resumePos());
                    deliverCompleteLine(buf, start, scan.lineLen());
                }
            }
        }

        private boolean consumePendingCR(ByteBuffer buf) {
            if (!pendingCR) {
                return false;
            }
            pendingCR = false;
            if (buf.get(buf.position()) == '\n') {
                buf.position(buf.position() + 1);
                return !buf.hasRemaining();
            }
            return false;
        }

        private static @Nullable TerminatorScan scanForTerminator(ByteBuffer buf, int start) {
            int end = buf.limit();
            for (int i = start; i < end; i++) {
                byte b = buf.get(i);
                if (b == '\n') {
                    return new TerminatorScan(i - start, i + 1, false);
                }
                if (b == '\r') {
                    int lineLen = i - start;
                    if (i + 1 < end) {
                        int resumePos = buf.get(i + 1) == '\n' ? i + 2 : i + 1;
                        return new TerminatorScan(lineLen, resumePos, false);
                    }
                    return new TerminatorScan(lineLen, i + 1, true);
                }
            }
            return null;
        }

        private void accumulateRemainingBytes(ByteBuffer buf, int start) {
            int len = buf.limit() - start;
            byte[] chunk = new byte[len];
            buf.get(start, chunk);
            accumulator.addChunk(chunk, 0, len);
            buf.position(buf.limit());
        }

        private void deliverCompleteLine(ByteBuffer buf, int start, int lineLen) {
            if (accumulator.isTooLong()) {
                sseParser.processLineTooLong();
            } else {
                byte[] chunk = new byte[lineLen];
                buf.get(start, chunk, 0, lineLen);
                accumulator.addChunk(chunk, 0, lineLen);
                if (accumulator.isTooLong()) {
                    sseParser.processLineTooLong();
                } else {
                    lineSubscriber.onNext(accumulator.toLine());
                }
            }
            accumulator.reset();
        }

        @Override
        public void onError(Throwable throwable) {
            lineSubscriber.onError(throwable);
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            // Flush any pending state as a final line (stream ended without a trailing LF)
            if (accumulator.isTooLong()) {
                sseParser.processLineTooLong();
            } else if (accumulator.size() > 0) {
                lineSubscriber.onNext(accumulator.toLine());
            }
            accumulator.reset();
            lineSubscriber.onComplete();
            result.complete(null);
        }
    }
}

