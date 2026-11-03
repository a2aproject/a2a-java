package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Tests for the terminal outcome contract of {@link RestSSEEventListener_v0_3}: a single stream has
 * to deliver exactly one terminal callback, no matter how the connection ends.
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
}
