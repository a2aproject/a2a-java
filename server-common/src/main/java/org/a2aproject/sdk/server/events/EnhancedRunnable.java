package org.a2aproject.sdk.server.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

public abstract class EnhancedRunnable implements Runnable {
    private volatile @Nullable Throwable error;
    private final List<DoneCallback> doneCallbacks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public @Nullable Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void addDoneCallback(DoneCallback doneCallback) {
        if (started.get()) {
            throw new IllegalStateException(
                "Cannot add callback after runnable has started execution. " +
                "Callbacks must be registered before CompletableFuture.runAsync() is called.");
        }
        doneCallbacks.add(doneCallback);
    }

    /**
     * Marks this runnable as started, preventing further callback additions.
     * This should be called immediately before submitting to CompletableFuture.runAsync().
     */
    public void markStarted() {
        started.set(true);
    }

    public void invokeDoneCallbacks() {
        for (DoneCallback doneCallback : doneCallbacks) {
            doneCallback.done(this);
        }
    }

    public interface DoneCallback {
        void done(EnhancedRunnable agentRunnable);
    }
}
