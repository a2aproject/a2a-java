package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class RestSSEEventListener_v0_3_Test {
    @Test
    void malformedEventReportsAndCancels() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, receivedError::set);
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage("{not-json", future);

        assertNotNull(receivedError.get());
        assertTrue(future.cancelled);
    }

    @Test
    void invalidPayloadReportsAndCancels() {
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, receivedError::set);
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage("{}", future);

        assertNotNull(receivedError.get());
        assertTrue(future.cancelled);
    }

    private static final class CancelCapturingFuture implements Future<Void> {
        private boolean cancelled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
