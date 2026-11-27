package org.a2aproject.sdk.client.http;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.a2aproject.sdk.client.http.A2AHttpClientFactory.ClientSelection;
import org.jspecify.annotations.Nullable;

/** Tracks a transport's active HTTP streams and releases the client when owned. */
public final class HttpStreamLifecycle implements AutoCloseable {
    private final ClientSelection selection;
    private final Map<CompletableFuture<Void>, Runnable> activeStreams = new HashMap<>();
    private boolean closed;

    /** @param selection client and lifecycle ownership */
    public HttpStreamLifecycle(ClientSelection selection) {
        this.selection = selection;
    }

    /**
     * Tracks a stream or cancels it if shutdown has already started.
     *
     * @param stream HTTP stream future, or null if the client returned none
     * @param onCancel indicates cancellation to the stream listener
     * @return the stream future, or null if none was supplied
     */
    public @Nullable CompletableFuture<Void> track(@Nullable CompletableFuture<Void> stream, Runnable onCancel) {
        if (stream == null) {
            return null;
        }
        synchronized (activeStreams) {
            if (!closed) {
                activeStreams.put(stream, onCancel);
                stream.whenComplete((result, error) -> {
                    synchronized (activeStreams) {
                        activeStreams.remove(stream);
                    }
                });
                return stream;
            }
        }
        cancel(stream, onCancel);
        return stream;
    }

    @Override
    public void close() {
        Map<CompletableFuture<Void>, Runnable> streams;
        synchronized (activeStreams) {
            if (closed) {
                return;
            }
            closed = true;
            streams = new HashMap<>(activeStreams);
            activeStreams.clear();
        }
        RuntimeException failure = null;
        for (var entry : streams.entrySet()) {
            try {
                cancel(entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                failure = accumulate(failure, e);
            }
        }
        if (selection.owned() && selection.client() instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                RuntimeException closeFailure = e instanceof RuntimeException runtime
                        ? runtime : new IllegalStateException("Failed to close HTTP client", e);
                failure = accumulate(failure, closeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static void cancel(CompletableFuture<Void> stream, Runnable onCancel) {
        try {
            onCancel.run();
        } finally {
            stream.cancel(true);
        }
    }

    private static RuntimeException accumulate(@Nullable RuntimeException failure, RuntimeException next) {
        if (failure == null) {
            return next;
        }
        if (failure != next) {
            failure.addSuppressed(next);
        }
        return failure;
    }
}
