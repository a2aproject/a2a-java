package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Tests for the terminal outcome contract of {@link RestSSEEventListener_v0_3}: a single stream has
 * to deliver exactly one terminal callback, no matter how the connection ends, and it has to stop
 * reading once the event that ends the task has been delivered.
 */
public class RestSSEEventListener_v0_3_Test {

    private static final String INVALID_PAYLOAD = "{ this is not a protobuf StreamResponse";

    @Test
    public void testOnErrorThenOnCompleteDeliversSingleTerminalCallback() {
        AtomicInteger terminalCount = new AtomicInteger(0);
        AtomicReference<Throwable> lastArg = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> {
                    terminalCount.incrementAndGet();
                    lastArg.set(error);
                });

        RuntimeException boom = new RuntimeException("first error");
        listener.onError(boom, null);
        listener.onComplete();

        assertEquals(1, terminalCount.get());
        assertSame(boom, lastArg.get());
    }

    @Test
    public void testOnCompleteThenOnErrorDeliversSingleTerminalCallback() {
        AtomicInteger terminalCount = new AtomicInteger(0);
        AtomicReference<Throwable> lastArg = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> {
                    terminalCount.incrementAndGet();
                    lastArg.set(error);
                });

        listener.onComplete();
        listener.onError(new RuntimeException("late error"), null);

        assertEquals(1, terminalCount.get());
        assertNull(lastArg.get());
    }

    @Test
    public void testRepeatedOnCompleteDeliversSingleTerminalCallback() {
        AtomicInteger terminalCount = new AtomicInteger(0);
        AtomicReference<Throwable> lastArg = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> {
                    terminalCount.incrementAndGet();
                    lastArg.set(error);
                });

        listener.onComplete();
        listener.onComplete();
        listener.onComplete();

        assertEquals(1, terminalCount.get());
        assertNull(lastArg.get());
    }

    @Test
    public void testConcurrentTerminalSignalsDeliverExactlyOneCallback() throws Exception {
        AtomicInteger terminalCount = new AtomicInteger(0);
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> terminalCount.incrementAndGet());

        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final boolean complete = (i % 2 == 0);
            pool.submit(() -> {
                try {
                    start.await();
                    if (complete) {
                        listener.onComplete();
                    } else {
                        listener.onError(new RuntimeException("race"), null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, terminalCount.get());
    }

    @Test
    public void testParsingFailureThenOnCompleteDeliversSingleTerminalCallback() {
        AtomicInteger terminalCount = new AtomicInteger(0);
        AtomicReference<Throwable> lastArg = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> {
                    terminalCount.incrementAndGet();
                    lastArg.set(error);
                });

        listener.onMessage(INVALID_PAYLOAD, null);
        listener.onComplete();

        assertEquals(1, terminalCount.get());
        assertNotNull(lastArg.get(), "The parsing failure has to stay the single terminal outcome");
    }

    @Test
    public void testOnCompleteDeliversNullToErrorHandler() {
        AtomicInteger terminalCount = new AtomicInteger(0);
        AtomicReference<Throwable> lastArg = new AtomicReference<>(new Throwable("not invoked"));
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                receivedEvent::set,
                error -> {
                    terminalCount.incrementAndGet();
                    lastArg.set(error);
                });

        listener.onComplete();

        assertEquals(1, terminalCount.get());
        assertNull(lastArg.get(), "A clean completion has to be reported as a null error");
        assertNull(receivedEvent.get(), "Completion must not fabricate an event");
    }

    @Test
    public void testOnCompleteWithoutErrorHandlerDoesNotThrow() {
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(event -> {}, null);

        listener.onComplete();
        listener.onComplete();
    }

    /**
     * A final status update ends the task, so the listener has to stop reading instead of waiting
     * for the server to close the response. Without the auto-close the connection stays open until
     * the server drops it, which never happens when the agent considers the stream reusable.
     */
    @Test
    public void testFinalStatusUpdateEventClosesTheConnection() {
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        AtomicInteger terminalCalls = new AtomicInteger();
        AtomicReference<Throwable> terminalError = new AtomicReference<>(new Throwable("not invoked"));
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                receivedEvent::set,
                error -> {
                    terminalCalls.incrementAndGet();
                    terminalError.set(error);
                });
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage(statusUpdatePayload(true), future);

        assertTrue(receivedEvent.get() instanceof TaskStatusUpdateEvent_v0_3, "The final event still has to be delivered");
        assertTrue(future.cancelHandlerCalled, "A final status update has to close the SSE connection");
        assertEquals(1, terminalCalls.get());
        assertNull(terminalError.get(), "Reaching the end of the task is a normal completion");
    }

    /**
     * A resubscription to a task that already finished only ever receives the task snapshot itself.
     * That snapshot is a terminal event just like a final status update.
     */
    @Test
    public void testTerminalTaskSnapshotClosesTheConnection() {
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        AtomicInteger terminalCalls = new AtomicInteger();
        AtomicReference<Throwable> terminalError = new AtomicReference<>(new Throwable("not invoked"));
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                receivedEvent::set,
                error -> {
                    terminalCalls.incrementAndGet();
                    terminalError.set(error);
                });
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage(taskPayload(TaskState_v0_3.COMPLETED), future);

        assertTrue(receivedEvent.get() instanceof Task_v0_3, "The task snapshot still has to be delivered");
        assertTrue(future.cancelHandlerCalled, "A task that is already in a final state has to close the SSE connection");
        assertEquals(1, terminalCalls.get());
        assertNull(terminalError.get(), "Reaching the end of the task is a normal completion");
    }

    @Test
    public void testNonFinalEventsKeepTheConnectionOpen() {
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        AtomicInteger terminalCalls = new AtomicInteger();
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                receivedEvent::set,
                error -> terminalCalls.incrementAndGet());
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage(statusUpdatePayload(false), future);
        listener.onMessage(taskPayload(TaskState_v0_3.WORKING), future);

        assertTrue(receivedEvent.get() instanceof Task_v0_3, "Both events have to be delivered");
        assertFalse(future.cancelHandlerCalled, "A stream that is still running must not be cancelled");
        assertEquals(0, terminalCalls.get(), "A stream that is still running has no terminal outcome yet");
    }

    /**
     * The auto-close must feed the same single terminal slot as every other outcome, so a completion
     * that arrives afterwards cannot add a second callback.
     */
    @Test
    public void testFinalEventThenOnCompleteDeliversSingleTerminalCallback() {
        AtomicInteger terminalCalls = new AtomicInteger();
        AtomicReference<Throwable> terminalError = new AtomicReference<>(new Throwable("not invoked"));
        RestSSEEventListener_v0_3 listener = new RestSSEEventListener_v0_3(
                event -> {},
                error -> {
                    terminalCalls.incrementAndGet();
                    terminalError.set(error);
                });
        CancelCapturingFuture future = new CancelCapturingFuture();

        listener.onMessage(statusUpdatePayload(true), future);
        listener.onComplete();

        assertTrue(future.cancelHandlerCalled);
        assertEquals(1, terminalCalls.get());
        assertNull(terminalError.get());
    }

    private static String statusUpdatePayload(boolean isFinal) {
        TaskStatusUpdateEvent_v0_3 event = new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("task-1")
                .contextId("context-1")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .isFinal(isFinal)
                .build();
        return payload(StreamResponse.newBuilder()
                .setStatusUpdate(ProtoUtils_v0_3.ToProto.taskStatusUpdateEvent(event))
                .build());
    }

    private static String taskPayload(TaskState_v0_3 state) {
        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-1")
                .contextId("context-1")
                .status(new TaskStatus_v0_3(state))
                .build();
        return payload(StreamResponse.newBuilder()
                .setTask(ProtoUtils_v0_3.ToProto.task(task))
                .build());
    }

    private static String payload(StreamResponse response) {
        try {
            return JsonFormat.printer().print(response);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Test payload has to be a printable StreamResponse", e);
        }
    }

    private static class CancelCapturingFuture implements Future<Void> {
        private boolean cancelHandlerCalled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelHandlerCalled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelHandlerCalled;
        }

        @Override
        public boolean isDone() {
            return cancelHandlerCalled;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
