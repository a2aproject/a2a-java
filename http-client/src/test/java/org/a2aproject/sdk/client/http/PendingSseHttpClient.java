package org.a2aproject.sdk.client.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Test HTTP client that leaves SSE requests pending until the test completes or cancels them. */
public class PendingSseHttpClient extends JdkA2AHttpClient implements AutoCloseable {
    public static final class CountingFuture extends CompletableFuture<Void> {
        private int cancellations;
        private Runnable completion = () -> {};
        private Consumer<Throwable> errors = error -> {};
        private boolean reportCancellationErrors;
        private Map<String, String> headers = Map.of();

        public int cancellations() {
            return cancellations;
        }

        public void signalCompletion() {
            completion.run();
        }

        public Map<String, String> headers() {
            return headers;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellations++;
            boolean firstCancellation = !isDone();
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (firstCancellation && reportCancellationErrors) {
                errors.accept(new IOException("Request cancelled"));
            }
            return cancelled;
        }
    }

    private final List<CountingFuture> requests = new ArrayList<>();
    private final List<Boolean> followRedirectCalls = new ArrayList<>();
    private volatile Runnable duringSetup = () -> {};
    private boolean reportCancellationErrors;

    private int closeCount;

    @Override
    public void close() {
        closeCount++;
    }

    public int closeCount() {
        return closeCount;
    }

    public List<CountingFuture> requests() {
        return requests;
    }

    public List<Boolean> followRedirectCalls() {
        return followRedirectCalls;
    }

    public void duringSetup(Runnable callback) {
        duringSetup = callback;
    }

    public void reportCancellationErrors(boolean enabled) {
        reportCancellationErrors = enabled;
    }

    @Override
    public PostBuilder createPost() {
        return new A2AHttpClient.PostBuilder() {
            private final Map<String, String> headers = new HashMap<>();

            @Override
            public PostBuilder url(String url) {
                return this;
            }
            @Override
            public PostBuilder addHeaders(Map<String, String> headers) {
                this.headers.putAll(headers);
                return this;
            }
            @Override
            public PostBuilder addHeader(String name, String value) {
                headers.put(name, value);
                return this;
            }
            @Override
            public PostBuilder followRedirects(boolean follow) {
                followRedirectCalls.add(follow);
                return this;
            }
            @Override
            public PostBuilder body(String body) {
                return this;
            }
            @Override
            public A2AHttpResponse post() {
                throw new UnsupportedOperationException();
            }
            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> messages,
                    Consumer<Throwable> errors, Runnable complete) {
                CountingFuture request = new CountingFuture();
                request.completion = complete;
                request.errors = errors;
                request.reportCancellationErrors = reportCancellationErrors;
                request.headers = Map.copyOf(headers);
                requests.add(request);
                duringSetup.run();
                return request;
            }
        };
    }
}
