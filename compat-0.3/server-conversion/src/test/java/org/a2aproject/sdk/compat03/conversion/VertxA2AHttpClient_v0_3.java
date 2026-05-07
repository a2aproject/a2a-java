package org.a2aproject.sdk.compat03.conversion;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.VertxA2AHttpClient;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.A2AHttpResponse_v0_3;
import io.vertx.core.Vertx;

/**
 * Adapts {@link VertxA2AHttpClient} to the {@link A2AHttpClient_v0_3} interface,
 * bridging the v0.3 SSE callback ({@code Consumer<String>}) to the v1.0 SSE callback
 * ({@code Consumer<ServerSentEvent>}) by extracting the event data payload.
 */
public class VertxA2AHttpClient_v0_3 implements A2AHttpClient_v0_3 {

    private final VertxA2AHttpClient delegate;

    public VertxA2AHttpClient_v0_3(Vertx vertx) {
        this.delegate = new VertxA2AHttpClient(vertx);
    }

    @Override
    public GetBuilder createGet() {
        return new GetBuilderAdapter(delegate.createGet());
    }

    @Override
    public PostBuilder createPost() {
        return new PostBuilderAdapter(delegate.createPost());
    }

    @Override
    public DeleteBuilder createDelete() {
        return new DeleteBuilderAdapter(delegate.createDelete());
    }

    private static A2AHttpResponse_v0_3 adapt(org.a2aproject.sdk.client.http.A2AHttpResponse r) {
        return new A2AHttpResponse_v0_3() {
            @Override public int status() { return r.status(); }
            @Override public boolean success() { return r.success(); }
            @Override public String body() { return r.body(); }
        };
    }

    private static class GetBuilderAdapter implements GetBuilder {
        private final A2AHttpClient.GetBuilder delegate;

        GetBuilderAdapter(A2AHttpClient.GetBuilder delegate) {
            this.delegate = delegate;
        }

        @Override public GetBuilder url(String s) { delegate.url(s); return this; }
        @Override public GetBuilder addHeader(String n, String v) { delegate.addHeader(n, v); return this; }
        @Override public GetBuilder addHeaders(Map<String, String> h) { delegate.addHeaders(h); return this; }

        @Override
        public A2AHttpResponse_v0_3 get() throws IOException, InterruptedException {
            return adapt(delegate.get());
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(Consumer<String> mc, Consumer<Throwable> ec, Runnable cr)
                throws IOException, InterruptedException {
            return delegate.getAsyncSSE(event -> mc.accept(event.data()), ec, cr);
        }
    }

    private static class PostBuilderAdapter implements PostBuilder {
        private final A2AHttpClient.PostBuilder delegate;

        PostBuilderAdapter(A2AHttpClient.PostBuilder delegate) {
            this.delegate = delegate;
        }

        @Override public PostBuilder url(String s) { delegate.url(s); return this; }
        @Override public PostBuilder addHeader(String n, String v) { delegate.addHeader(n, v); return this; }
        @Override public PostBuilder addHeaders(Map<String, String> h) { delegate.addHeaders(h); return this; }
        @Override public PostBuilder body(String body) { delegate.body(body); return this; }

        @Override
        public A2AHttpResponse_v0_3 post() throws IOException, InterruptedException {
            return adapt(delegate.post());
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(Consumer<String> mc, Consumer<Throwable> ec, Runnable cr)
                throws IOException, InterruptedException {
            return delegate.postAsyncSSE(event -> mc.accept(event.data()), ec, cr);
        }
    }

    private static class DeleteBuilderAdapter implements DeleteBuilder {
        private final A2AHttpClient.DeleteBuilder delegate;

        DeleteBuilderAdapter(A2AHttpClient.DeleteBuilder delegate) {
            this.delegate = delegate;
        }

        @Override public DeleteBuilder url(String s) { delegate.url(s); return this; }
        @Override public DeleteBuilder addHeader(String n, String v) { delegate.addHeader(n, v); return this; }
        @Override public DeleteBuilder addHeaders(Map<String, String> h) { delegate.addHeaders(h); return this; }

        @Override
        public A2AHttpResponse_v0_3 delete() throws IOException, InterruptedException {
            return adapt(delegate.delete());
        }
    }
}
