package org.a2aproject.sdk.extras.opentelemetry.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.util.Assert;

/**
 * Delegating {@link A2AHttpClient} that injects W3C trace-context headers
 * ({@code traceparent}, {@code tracestate}) into every outbound HTTP request.
 *
 * <p>The propagation uses the {@link io.opentelemetry.context.propagation.TextMapPropagator}
 * registered with the supplied {@link OpenTelemetry} instance, reading the current
 * {@link Context} at call time. This is the same mechanism the client-side
 * {@code OpenTelemetryClientPropagatorTransport} uses, applied here to
 * server-initiated outbound calls such as push-notification delivery.
 *
 * @see OpenTelemetryHttpClientProducer
 */
class OpenTelemetryA2AHttpClient implements A2AHttpClient {

    private final A2AHttpClient delegate;
    private final OpenTelemetry openTelemetry;

    OpenTelemetryA2AHttpClient(A2AHttpClient delegate, OpenTelemetry openTelemetry) {
        this.delegate = Assert.checkNotNullParam("delegate", delegate);
        this.openTelemetry = Assert.checkNotNullParam("openTelemetry", openTelemetry);
    }

    @Override
    public GetBuilder createGet() {
        return new PropagatingGetBuilder(delegate.createGet(), openTelemetry);
    }

    @Override
    public PostBuilder createPost() {
        return new PropagatingPostBuilder(delegate.createPost(), openTelemetry);
    }

    @Override
    public DeleteBuilder createDelete() {
        return new PropagatingDeleteBuilder(delegate.createDelete(), openTelemetry);
    }

    @SuppressWarnings("rawtypes")
    private static final TextMapSetter<Builder> BUILDER_SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.addHeader(key, value);
        }
    };

    /**
     * Abstract base for propagating builder wrappers. Consolidates the common
     * {@link Builder} delegation ({@code url}, {@code addHeaders}, {@code addHeader})
     * and the context injection logic into one place.
     *
     * @param <S> the concrete propagating builder type (for method chaining)
     * @param <D> the delegate builder type
     */
    private abstract static class AbstractPropagatingBuilder<S extends Builder<S>, D extends Builder<D>> {

        final D delegate;
        private final OpenTelemetry openTelemetry;

        AbstractPropagatingBuilder(D delegate, OpenTelemetry openTelemetry) {
            this.delegate = delegate;
            this.openTelemetry = openTelemetry;
        }

        @SuppressWarnings("unchecked")
        protected S self() {
            return (S) this;
        }

        public S url(String s) {
            delegate.url(s);
            return self();
        }

        public S addHeaders(Map<String, String> headers) {
            delegate.addHeaders(headers);
            return self();
        }

        public S addHeader(String name, String value) {
            delegate.addHeader(name, value);
            return self();
        }

        @SuppressWarnings("rawtypes")
        protected void injectContext() {
            openTelemetry.getPropagators().getTextMapPropagator()
                    .inject(Context.current(), (Builder) delegate, BUILDER_SETTER);
        }
    }

    private static final class PropagatingGetBuilder
            extends AbstractPropagatingBuilder<GetBuilder, GetBuilder>
            implements GetBuilder {

        PropagatingGetBuilder(GetBuilder delegate, OpenTelemetry openTelemetry) {
            super(delegate, openTelemetry);
        }

        @Override
        public A2AHttpResponse get() throws IOException, InterruptedException {
            injectContext();
            return delegate.get();
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            injectContext();
            return delegate.getAsyncSSE(messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private static final class PropagatingPostBuilder
            extends AbstractPropagatingBuilder<PostBuilder, PostBuilder>
            implements PostBuilder {

        PropagatingPostBuilder(PostBuilder delegate, OpenTelemetry openTelemetry) {
            super(delegate, openTelemetry);
        }

        @Override
        public PostBuilder body(String body) {
            delegate.body(body);
            return this;
        }

        @Override
        public PostBuilder followRedirects(boolean follow) {
            delegate.followRedirects(follow);
            return this;
        }

        @Override
        public A2AHttpResponse post() throws IOException, InterruptedException {
            injectContext();
            return delegate.post();
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException {
            injectContext();
            return delegate.postAsyncSSE(messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private static final class PropagatingDeleteBuilder
            extends AbstractPropagatingBuilder<DeleteBuilder, DeleteBuilder>
            implements DeleteBuilder {

        PropagatingDeleteBuilder(DeleteBuilder delegate, OpenTelemetry openTelemetry) {
            super(delegate, openTelemetry);
        }

        @Override
        public A2AHttpResponse delete() throws IOException, InterruptedException {
            injectContext();
            return delegate.delete();
        }
    }
}
