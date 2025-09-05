package io.a2a.client.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class JdkA2AHttpClient implements A2AHttpClient {

    private final HttpClient httpClient;

    public JdkA2AHttpClient() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
        private String url;
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
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable
        ) {
            Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(String item) {
                    // SSE messages sometimes start with "data:". Strip that off
                    if (item != null && item.startsWith("data:")) {
                        item = item.substring(5).trim();
                        if (!item.isEmpty()) {
                            messageConsumer.accept(item);
                        }
                    }
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    errorConsumer.accept(throwable);
                    subscription.cancel();
                }

                @Override
                public void onComplete() {
                    completeRunnable.run();
                    subscription.cancel();
                }
            };

            BodyHandler<Void> bodyHandler = BodyHandlers.fromLineSubscriber(subscriber);

            // Send the response async, and let the subscriber handle the lines.
            return httpClient.sendAsync(request, bodyHandler)
                    .thenAccept(response -> {
                        if (!JdkHttpResponse.success(response)) {
                            subscriber.onError(new IOException("Request failed " + response.statusCode()));
                        }
                    });
        }
    }

    private class JdkGetBuilder extends JdkBuilder<GetBuilder> implements A2AHttpClient.GetBuilder {

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().GET();
            if (SSE) {
                builder.header("Accept", "text/event-stream");
            }
            return builder;
        }

        @Override
        public A2AHttpResponse get() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<String> messageConsumer,
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
                builder.header("Accept", "text/event-stream");
            }
            return builder;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            HttpRequest request = createRequestBuilder(false)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkHttpResponse(response);
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<String> messageConsumer,
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
        public boolean success() {// Send the request and get the response
            return success(response);
        }

        static boolean success(HttpResponse<?> response) {
            return response.statusCode() >= 200 && response.statusCode() < 300;
        }

        @Override
        public String body() {
            return response.body();
        }
    }
}
