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
import io.a2a.spec.Task;
import io.a2a.util.Utils;
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
            tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE));
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