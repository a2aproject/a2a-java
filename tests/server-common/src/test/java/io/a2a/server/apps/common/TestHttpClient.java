package io.a2a.server.apps.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.json.JsonProcessingException;
import io.a2a.spec.Task;
import io.a2a.json.JsonUtil;
import java.util.Map;

@Dependent
@Alternative
public class TestHttpClient implements A2AHttpClient {
    final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
    volatile CountDownLatch latch;

    @Override
    public GetBuilder createGet() {
        return null;
    }

    @Override
    public PostBuilder createPost() {
        return new TestPostBuilder();
    }

    @Override
    public DeleteBuilder createDelete() {
        return null;
    }

    class TestPostBuilder implements A2AHttpClient.PostBuilder {
        private volatile String body;
        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return this;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            try {
                tasks.add(JsonUtil.fromJson(body, Task.class));
            } catch (JsonProcessingException e) {
                throw new IOException("Failed to parse task JSON", e);
            }
            try {
                return new A2AHttpResponse() {
                    @Override
                    public int status() {
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
                };
            } finally {
                latch.countDown();
            }
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
            return null;
        }

        @Override
        public PostBuilder url(String s) {
            return this;
        }

        @Override
        public PostBuilder addHeader(String name, String value) {
            return this;
        }

        @Override
        public PostBuilder addHeaders(Map<String, String> headers) {
            return this;
        }
    }
}