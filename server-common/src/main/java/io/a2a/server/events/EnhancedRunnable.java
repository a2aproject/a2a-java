package io.a2a.server.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;

public abstract class EnhancedRunnable implements Runnable {
    private volatile @Nullable Throwable error;
    private final List<DoneCallback> doneCallbacks = new CopyOnWriteArrayList<>();

    public @Nullable Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void addDoneCallback(DoneCallback doneCallback) {
        doneCallbacks.add(doneCallback);
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
