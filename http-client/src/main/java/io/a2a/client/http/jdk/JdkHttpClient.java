package io.a2a.client.http.jdk;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.http.jdk.sse.SSEHandler;
import io.a2a.client.http.sse.Event;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

import io.a2a.common.A2AErrorMessages;

class JdkHttpClient implements HttpClient {

    private final java.net.http.HttpClient httpClient;
    private final String baseUrl;

    JdkHttpClient(String baseUrl) {
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();

        URL targetUrl = buildUrl(baseUrl);
        this.baseUrl = targetUrl.getProtocol() + "://" + targetUrl.getAuthority();
    }

    String getBaseUrl() {
        return baseUrl;
    }

    private static final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        protected URLConnection openConnection(URL u) {
            return null;
        }
    };

    private static URL buildUrl(String uri) {
        try {
            return new URL(null, uri, URL_HANDLER);
        } catch (MalformedURLException var2) {
            throw new IllegalArgumentException("URI [" + uri + "] is not valid");
        }
    }

    @Override
    public GetRequestBuilder get(String path) {
        return new JdkGetRequestBuilder(path);
    }

    @Override
    public PostRequestBuilder post(String path) {
        return new JdkPostRequestBuilder(path);
    }

    @Override
    public DeleteRequestBuilder delete(String path) {
        return new JdkDeleteBuilder(path);
    }

    private abstract class JdkRequestBuilder<T extends RequestBuilder<T>> implements RequestBuilder<T> {
        private final String path;
        protected final Map<String, String> headers = new HashMap<>();

        public JdkRequestBuilder(String path) {
            this.path = path;
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

        protected HttpRequest.Builder createRequestBuilder() {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path));
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
            return builder;
        }
    }

    private class JdkGetRequestBuilder extends JdkRequestBuilder<GetRequestBuilder> implements GetRequestBuilder {

        public JdkGetRequestBuilder(String path) {
            super(path);
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            HttpRequest request = super.createRequestBuilder().GET().build();
            return httpClient
                    .sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(RESPONSE_MAPPER);
        }
    }

    private class JdkDeleteBuilder extends JdkRequestBuilder<DeleteRequestBuilder> implements DeleteRequestBuilder {

        public JdkDeleteBuilder(String path) {
            super(path);
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            HttpRequest request = super.createRequestBuilder().DELETE().build();
            return httpClient
                    .sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenCompose(RESPONSE_MAPPER);
        }
    }

    private class JdkPostRequestBuilder extends JdkRequestBuilder<PostRequestBuilder> implements PostRequestBuilder {
        String body = "";

        public JdkPostRequestBuilder(String path) {
            super(path);
        }

        @Override
        public PostRequestBuilder body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            final HttpRequest request = super.createRequestBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            final BodyHandler<?> bodyHandler;

            final String contentTypeHeader = this.headers.get("Accept");
            if ("text/event-stream".equalsIgnoreCase(contentTypeHeader)) {
                bodyHandler = BodyHandlers.ofPublisher();
            } else {
                bodyHandler = BodyHandlers.ofString(StandardCharsets.UTF_8);
            }

            return httpClient.sendAsync(request, bodyHandler).thenCompose(RESPONSE_MAPPER);
        }
    }

    private final static Function<java.net.http.HttpResponse<?>, CompletionStage<HttpResponse>> RESPONSE_MAPPER = response -> {
        if (response.statusCode() == HTTP_UNAUTHORIZED) {
            return CompletableFuture.failedStage(new IOException(A2AErrorMessages.AUTHENTICATION_FAILED));
        } else if (response.statusCode() == HTTP_FORBIDDEN) {
            return CompletableFuture.failedStage(new IOException(A2AErrorMessages.AUTHORIZATION_FAILED));
        }

        return CompletableFuture.completedFuture(new JdkHttpResponse(response));
    };

    private record JdkHttpResponse(java.net.http.HttpResponse<?> response) implements HttpResponse {

        @Override
            public int statusCode() {
                return response.statusCode();
            }

            static boolean success(java.net.http.HttpResponse<?> response) {
                return response.statusCode() >= HTTP_OK && response.statusCode() < HTTP_MULT_CHOICE;
            }

            @Override
            public String body() {
                if (response.body() instanceof String) {
                    return (String) response.body();
                }

                throw new IllegalStateException();
            }

            @Override
            public void bodyAsSse(Consumer<Event> eventConsumer, Consumer<Throwable> errorConsumer) {
                if (success()) {
                    Optional<String> contentTypeOpt = response.headers().firstValue("Content-Type");

                    if (contentTypeOpt.isPresent() && contentTypeOpt.get().equalsIgnoreCase("text/event-stream")) {
                        Flow.Publisher<List<ByteBuffer>> publisher = (Flow.Publisher<List<ByteBuffer>>) response.body();

                        SSEHandler sseHandler = new SSEHandler();
                        sseHandler.subscribe(new Flow.Subscriber<>() {
                            private Flow.Subscription subscription;

                            @Override
                            public void onSubscribe(Flow.Subscription subscription) {
                                this.subscription = subscription;
                                subscription.request(1);
                            }

                            @Override
                            public void onNext(Event item) {
                                eventConsumer.accept(item);
                                subscription.request(1);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                errorConsumer.accept(throwable);
                                subscription.cancel();
                            }

                            @Override
                            public void onComplete() {
                                subscription.cancel();
                            }
                        });

                        publisher.subscribe(java.net.http.HttpResponse.BodySubscribers.fromLineSubscriber(sseHandler));
                    } else {
                        errorConsumer.accept(new IOException("Response is not an event-stream response: Content-Type[" + contentTypeOpt.orElse("unknown") + "]"));
                    }
                } else {
                    errorConsumer.accept(new IOException("Request failed: status[" + response.statusCode() + "]"));
                }
            }
        }

    private static boolean isSuccessStatus(int statusCode) {
        return statusCode >= HTTP_OK && statusCode <  HTTP_MULT_CHOICE;
    }
}
