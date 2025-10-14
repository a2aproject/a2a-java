package io.a2a.client.http.vertx;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.http.sse.Event;
import io.a2a.client.http.vertx.sse.SSEHandler;
import io.a2a.common.A2AErrorMessages;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

public class VertxHttpClient implements HttpClient {

    private final io.vertx.core.http.HttpClient client;

    private final Vertx vertx;

    VertxHttpClient(String baseUrl, Vertx vertx, HttpClientOptions options) {
        this.vertx = vertx;
        this.client = initClient(baseUrl, options);
    }

    private io.vertx.core.http.HttpClient initClient(String baseUrl, HttpClientOptions options) {
        URL targetUrl = buildUrl(baseUrl);

        return this.vertx.createHttpClient(options
                .setDefaultHost(targetUrl.getHost())
                .setDefaultPort(targetUrl.getPort() != -1 ? targetUrl.getPort() : targetUrl.getDefaultPort())
                .setSsl(isSecureProtocol(targetUrl.getProtocol())));
    }

    @Override
    public GetRequestBuilder get(String path) {
        return new VertxGetRequestBuilder(path);
    }

    @Override
    public PostRequestBuilder post(String path) {
        return new VertxPostRequestBuilder(path);
    }

    @Override
    public DeleteRequestBuilder delete(String path) {
        return new VertxDeleteRequestBuilder(path);
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

    private static boolean isSecureProtocol(String protocol) {
        return protocol.charAt(protocol.length() - 1) == 's' && protocol.length() > 2;
    }

    private abstract class VertxRequestBuilder<T extends RequestBuilder<T>> implements RequestBuilder<T> {
        protected final Future<HttpClientRequest> request;
        protected final Map<String, String> headers = new HashMap<>();

        public VertxRequestBuilder(String path, HttpMethod method) {
            this.request = client.request(method, path);
        }

        @Override
        public T addHeader(String name, String value) {
            headers.put(name, value);
            return self();
        }

        @Override
        public T addHeaders(Map<String, String> headers) {
            if (headers != null && ! headers.isEmpty()) {
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

        protected Future<HttpClientResponse> sendRequest() {
            return sendRequest(Optional.empty());
        }

        protected Future<HttpClientResponse> sendRequest(Optional<String> body) {
            return request
                    .compose(new Function<HttpClientRequest, Future<HttpClientResponse>>() {
                        @Override
                        public Future<HttpClientResponse> apply(HttpClientRequest request) {
                            // Prepare the request
                            request.headers().addAll(headers);

                            if (body.isPresent()) {
                                return request.send(body.get());
                            } else {
                                return request.send();
                            }
                        }
                    });
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            return sendRequest()
                    .compose(RESPONSE_MAPPER)
                    .toCompletionStage()
                    .toCompletableFuture();
        }
    }

    private class VertxGetRequestBuilder extends VertxRequestBuilder<GetRequestBuilder> implements GetRequestBuilder {

        public VertxGetRequestBuilder(String path) {
            super(path, HttpMethod.GET);
        }
    }

    private class VertxDeleteRequestBuilder extends VertxRequestBuilder<DeleteRequestBuilder> implements DeleteRequestBuilder {

        public VertxDeleteRequestBuilder(String path) {
            super(path, HttpMethod.DELETE);
        }
    }

    private class VertxPostRequestBuilder extends VertxRequestBuilder<PostRequestBuilder> implements PostRequestBuilder {
        String body = "";

        public VertxPostRequestBuilder(String path) {
            super(path, HttpMethod.POST);
        }

        @Override
        public PostRequestBuilder body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            return sendRequest(Optional.of(this.body))
                    .compose(RESPONSE_MAPPER)
                    .toCompletionStage()
                    .toCompletableFuture();
        }
    }

    private final Function<HttpClientResponse, Future<HttpResponse>> RESPONSE_MAPPER = response -> {
        if (response.statusCode() == HTTP_UNAUTHORIZED) {
            return Future.failedFuture(new IOException(A2AErrorMessages.AUTHENTICATION_FAILED));
        } else if (response.statusCode() == HTTP_FORBIDDEN) {
            return Future.failedFuture(new IOException(A2AErrorMessages.AUTHORIZATION_FAILED));
        }

        return Future.succeededFuture(new VertxHttpResponse(response));
    };

    private record VertxHttpResponse(HttpClientResponse response)implements HttpResponse {

        @Override
        public int statusCode() {
            return response.statusCode();
        }

        @Override
        public CompletableFuture<String> body() {
                return response.body().map(Buffer::toString).toCompletionStage().toCompletableFuture();
        }

        @Override
        public void bodyAsSse(Consumer<Event> eventConsumer, Consumer<Throwable> errorConsumer) {
            String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE.toString());

            if (contentType != null && HttpHeaderValues.TEXT_EVENT_STREAM.contentEqualsIgnoreCase(contentType)) {
                final SSEHandler handler = new SSEHandler(eventConsumer);

                response.handler(handler).exceptionHandler(errorConsumer::accept);
            } else {
                throw new IllegalStateException("Response is not an event-stream response.");
            }
        }
    }
}
