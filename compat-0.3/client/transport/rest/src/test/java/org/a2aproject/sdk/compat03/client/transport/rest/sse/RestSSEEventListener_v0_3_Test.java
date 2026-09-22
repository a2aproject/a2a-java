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

    @Test
    void finalStatusUpdateCancels() {
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, error -> {});
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage("""
                {
                  "status_update": {
                    "task_id": "task-1",
                    "context_id": "context-1",
                    "status": {"state": "TASK_STATE_COMPLETED"},
                    "final": true
                  }
                }""", future);

        assertTrue(future.cancelled);
    }

    @Test
    void finalTaskCancels() {
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, error -> {});
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage("""
                {
                  "task": {
                    "id": "task-1",
                    "contextId": "context-1",
                    "status": {"state": "TASK_STATE_COMPLETED"}
                  }
                }""", future);

        assertTrue(future.cancelled);
    }

    @Test
    void malformedEventWithoutErrorHandlerDoesNotThrow() {
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, null);

        listener.onMessage("{not-json", null);
    }

    @Test
    void invalidPayloadWithoutErrorHandlerDoesNotThrow() {
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {}, null);

        listener.onMessage("{}", null);
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
