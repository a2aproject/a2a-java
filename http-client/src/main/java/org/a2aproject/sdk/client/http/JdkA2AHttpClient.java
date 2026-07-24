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

    /**
     * Creates a new JDK-based HTTP client.
     *
     * <p>Configures the client with:
     * <ul>
     *   <li>HTTP/2 preferred (with HTTP/1.1 fallback)</li>
     *   <li>Normal redirect following</li>
     * </ul>
     */
    public JdkA2AHttpClient() {
        this(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /**
     * Creates a new JDK-based HTTP client using a caller-provided JDK {@link HttpClient}.
     *
     * @param httpClient the JDK HTTP client to delegate requests to
     * @throws IllegalArgumentException if {@code httpClient} is {@code null}
     */
    public JdkA2AHttpClient(HttpClient httpClient) {
        this.httpClient = checkNotNullParam("httpClient", httpClient);
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
                Runnable completeRunnable
        ) {
            ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer);
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
                return BodyHandlers.fromLineSubscriber(subscriber).apply(responseInfo);
            };

            // Send the response async, and let the subscriber handle the lines.
            // handle() catches failures before the body handler is reached (e.g. connection
            // errors, DNS failures) and routes them to errorConsumer, then always completes
            // normally — consistent with the Vert.x and Android implementations.
            return httpClient.sendAsync(request, bodyHandler)
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
                builder.header(ACCEPT, EVENT_STREAM);
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

        @Override
        public A2AHttpHeaders headers() {
            return A2AHttpHeaders.of(response.headers().map());
        }
    }
}

