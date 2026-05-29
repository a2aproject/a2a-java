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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

import org.a2aproject.sdk.common.A2AErrorMessages;

/**
 * Default HTTP client implementation using JDK 11+ {@link HttpClient}.
 *
 * <p>This is the fallback implementation used when no higher-priority
 * {@link A2AHttpClientProvider} is available. It provides full support for:
 * <ul>
 *   <li>HTTP/2 for regular requests, HTTP/1.1 for SSE streaming (configurable per request type)</li>
 *   <li>Synchronous GET, POST, and DELETE requests</li>
 *   <li>Asynchronous Server-Sent Events (SSE) streaming</li>
 *   <li>Automatic redirect following</li>
 * </ul>
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
    private final HttpClient.Version requestVersion;
    private final HttpClient.Version sseVersion;

    /**
     * Creates a new JDK-based HTTP client.
     *
     * <p>Configures the client with:
     * <ul>
     *   <li>HTTP/2 for regular requests (with HTTP/1.1 fallback)</li>
     *   <li>HTTP/1.1 for SSE streaming connections</li>
     *   <li>Normal redirect following</li>
     * </ul>
     */
    public JdkA2AHttpClient() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /**
     * Creates a new JDK-based HTTP client using a caller-provided JDK {@link HttpClient}.
     *
     * <p>Uses HTTP/2 for regular requests and HTTP/1.1 for SSE streaming connections.
     *
     * @param httpClient the JDK HTTP client to delegate requests to
     * @throws IllegalArgumentException if {@code httpClient} is {@code null}
     */
    public JdkA2AHttpClient(HttpClient httpClient) {
        this(httpClient, HttpClient.Version.HTTP_2, HttpClient.Version.HTTP_1_1);
    }

    /**
     * Creates a new JDK-based HTTP client with configurable HTTP versions.
     *
     * @param httpClient the JDK HTTP client to delegate requests to
     * @param requestVersion the HTTP version to use for regular (non-SSE) requests
     * @param sseVersion the HTTP version to use for SSE streaming connections
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public JdkA2AHttpClient(HttpClient httpClient, HttpClient.Version requestVersion, HttpClient.Version sseVersion) {
        this.httpClient = checkNotNullParam("httpClient", httpClient);
        this.requestVersion = checkNotNullParam("requestVersion", requestVersion);
        this.sseVersion = checkNotNullParam("sseVersion", sseVersion);
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

        static boolean isCancellation(Throwable t) {
            while (t != null) {
                if (t instanceof CancellationException) {
                    return true;
                }
                // HTTP/1.1: Http1Exchange.cancel() throws IOException synchronously with this
                // hardcoded English message (not locale-sensitive — no resource bundle is used)
                if (t instanceof IOException && "Request cancelled".equals(t.getMessage())) {
                    return true;
                }
                t = t.getCause();
            }
            return false;
        }

        protected HttpRequest.Builder createRequestBuilder() throws IOException {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .version(requestVersion);
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
            return builder;
        }

        protected CompletableFuture<Void> asyncRequest(
                HttpRequest request,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable
        ) {
            ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer);
            AtomicBoolean useSseParser = new AtomicBoolean(false);
            AtomicBoolean errorNotified = new AtomicBoolean(false);
            AtomicBoolean completeNotified = new AtomicBoolean(false);
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
                    if (isCancellation(throwable)) {
                        // Treat cancellation as clean completion so completeRunnable fires and
                        // callers can distinguish a deliberate cancel from a real error.
                        // onComplete() guards against double-invocation via completeNotified, so
                        // calling it here is safe even if the publisher also delivers onComplete().
                        onComplete();
                        return;
                    }
                    if (errorNotified.compareAndSet(false, true)) {
                        errorConsumer.accept(throwable);
                    }
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }

                @Override
                public void onComplete() {
                    if (!errorNotified.get() && completeNotified.compareAndSet(false, true)) {
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
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            };

            // Create a custom body handler that checks status and Content-Type before processing body
            BodyHandler<Void> bodyHandler = responseInfo -> {
                // Check for authentication/authorization errors only
                if (responseInfo.statusCode() == HTTP_UNAUTHORIZED || responseInfo.statusCode() == HTTP_FORBIDDEN) {
                    final String errorMessage;
                    if (responseInfo.statusCode() == HTTP_UNAUTHORIZED) {
                        errorMessage = A2AErrorMessages.AUTHENTICATION_FAILED;
                    } else {
                        errorMessage = A2AErrorMessages.AUTHORIZATION_FAILED;
                    }
                    // Return a body subscriber that immediately signals error
                    return BodySubscribers.fromSubscriber(new Flow.Subscriber<List<ByteBuffer>>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {
                            subscriber.onError(new IOException(errorMessage));
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
                return BodyHandlers.fromLineSubscriber(subscriber).apply(responseInfo);
            };

            // Send the response async, and let the subscriber handle the lines.
            // handle() catches failures before the body handler is reached (e.g. connection
            // errors, DNS failures) and routes them to errorConsumer, then always completes
            // normally — consistent with the Vert.x and Android implementations.
            return httpClient.sendAsync(request, bodyHandler)
                    .<Void>handle((response, throwable) -> {
                        if (throwable != null && !isCancellation(throwable)
                                && errorNotified.compareAndSet(false, true)) {
                            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                            errorConsumer.accept(cause);
                        }
                        return null;
                    });
        }
    }

    private class JdkGetBuilder extends JdkBuilder<GetBuilder> implements A2AHttpClient.GetBuilder {

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().GET();
            if (SSE) {
                builder.header(ACCEPT, EVENT_STREAM)
                        .version(sseVersion);
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
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED);
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED);
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
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED);
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED);
            }

            return new JdkHttpResponse(response);
        }

    }

    private class JdkPostBuilder extends JdkBuilder<PostBuilder> implements A2AHttpClient.PostBuilder {
        String body = "";

        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return self();
        }

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (SSE) {
                builder.header(ACCEPT, EVENT_STREAM)
                        .version(sseVersion);
            }
            return builder;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == HTTP_UNAUTHORIZED) {
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED);
            } else if (response.statusCode() == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED);
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
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
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
    }
}

