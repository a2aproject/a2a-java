package io.a2a.server.apps.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.a2a.client.http.sse.Event;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.spec.Task;
import io.a2a.util.Utils;
import java.util.Map;

@Dependent
@Alternative
public class TestHttpClient implements HttpClient {
    final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
    volatile CountDownLatch latch;

    @Override
    public GetRequestBuilder get(String path) {
        return null;
    }

    @Override
    public PostRequestBuilder post(String path) {
        return new TestPostRequestBuilder();
    }

    @Override
    public DeleteRequestBuilder delete(String path) {
        return null;
    }

    class TestPostRequestBuilder implements PostRequestBuilder {

        private volatile String body;
        @Override
        public PostRequestBuilder body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public CompletableFuture<HttpResponse> send() {
            CompletableFuture<HttpResponse> future = new CompletableFuture<>();

            try {
                tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE));

                future.complete(
                    new HttpResponse() {
                        @Override
                        public int statusCode() {
                            return 200;
                        }

                        @Override
                        public boolean success() {
                            return true;
                        }

                        @Override
                        public String body() {
                            return "";
                        }

                        @Override
                        public void bodyAsSse(Consumer<Event> eventConsumer, Consumer<Throwable> errorConsumer) {

                        }
                    });
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            } finally {
                latch.countDown();
            }

            return future;
        }

        @Override
        public PostRequestBuilder addHeader(String name, String value) {
            return this;
        }

        @Override
        public PostRequestBuilder addHeaders(Map<String, String> headers) {
            return this;
        }
    }
}