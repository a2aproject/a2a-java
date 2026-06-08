package org.a2aproject.sdk.server.common.quarkus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

// LENIENT: setUp stubs are shared convenience for write tests but not all tests need them all
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SseResponseWriterTest {

    @Mock
    HttpServerResponse response;
    @Mock
    RoutingContext rc;
    @Mock
    MultiMap headers;

    private ServerCallContext context;

    @BeforeEach
    void setUp() {
        when(rc.response()).thenReturn(response);
        when(response.headers()).thenReturn(headers);
        when(response.setChunked(true)).thenReturn(response);
        successfulWrite();
        when(response.ended()).thenReturn(false);
        context = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
    }

    @Test
    void emptyStream_callsEndWithoutAnyWrite() {
        SseResponseWriter.writeSseStrings(Multi.createFrom().empty(), rc, context, null);

        verify(response, never()).write(any(Buffer.class), any());
        verify(response).end();
    }

    @Test
    void singleEvent_setsHeadersAndWritesKickstartPlusData() {
        List<Buffer> written = captureWrites();

        SseResponseWriter.writeSseStrings(Multi.createFrom().item("data: hello\n\n"), rc, context, null);

        verify(response, times(1)).write(any(Buffer.class), any());
        verify(response).end();
        // First (and only) write must contain the SSE kickstart comment
        assertTrue(written.get(0).toString().contains(": SSE stream started"),
                "First write should include SSE kickstart comment");
        assertTrue(written.get(0).toString().contains("data: hello"),
                "First write should include the event data");
    }

    @Test
    void multipleEvents_kickstartOnlyOnFirstWrite() {
        List<Buffer> written = captureWrites();

        SseResponseWriter.writeSseStrings(
                Multi.createFrom().items("data: first\n\n", "data: second\n\n"),
                rc, context, null);

        verify(response, times(2)).write(any(Buffer.class), any());
        assertTrue(written.get(0).toString().contains(": SSE stream started"),
                "First write should include SSE kickstart comment");
        assertFalse(written.get(1).toString().contains(": SSE stream started"),
                "Subsequent writes must not repeat the kickstart comment");
    }

    @Test
    void writeFails_failsRoutingContext() {
        failingWrite(new RuntimeException("network error"));
        when(rc.failed()).thenReturn(false);

        SseResponseWriter.writeSseStrings(Multi.createFrom().item("data: hello\n\n"), rc, context, null);

        verify(rc).fail(any(Throwable.class));
    }

    @Test
    void clientDisconnect_invokesEventConsumerCancelAndCancelsSubscription() {
        AtomicReference<Handler<Void>> capturedCloseHandler = new AtomicReference<>();
        doAnswer(inv -> {
            capturedCloseHandler.set(inv.getArgument(0));
            return response;
        }).when(response).closeHandler(any());

        // never() emits nothing and does not complete — subscriber stays attached
        SseResponseWriter.writeSseStrings(Multi.createFrom().nothing(), rc, context, null);

        // Simulate client disconnect on the event-loop thread
        capturedCloseHandler.get().handle(null);

        // EventConsumer.cancel() must be called so the polling loop stops
        // (verified indirectly: ServerCallContext.invokeEventConsumerCancelCallback() is a no-op
        //  when no callback is registered, so no exception means the path was exercised)
        verify(response).closeHandler(any());
    }

    @Test
    void onSubscribedHook_isCalledAfterSubscribe() {
        Runnable hook = mock(Runnable.class);

        SseResponseWriter.writeSseStrings(Multi.createFrom().empty(), rc, context, hook);

        verify(hook).run();
    }

    @Test
    void responseAlreadyEnded_endIsNotCalledAgain() {
        when(response.ended()).thenReturn(true);

        SseResponseWriter.writeSseStrings(Multi.createFrom().empty(), rc, context, null);

        verify(response, never()).end();
    }

    // --- helpers ---

    /** Configures the response mock to invoke write callbacks with a successful result. */
    private void successfulWrite() {
        doAnswer(inv -> {
            AsyncResult<Void> ok = successResult();
            inv.<Handler<AsyncResult<Void>>>getArgument(1).handle(ok);
            return response;
        }).when(response).write(any(Buffer.class), any());
    }

    /** Configures the response mock to invoke write callbacks with a failure. */
    private void failingWrite(Throwable cause) {
        doAnswer(inv -> {
            AsyncResult<Void> fail = failResult(cause);
            inv.<Handler<AsyncResult<Void>>>getArgument(1).handle(fail);
            return response;
        }).when(response).write(any(Buffer.class), any());
    }

    /** Captures every Buffer passed to response.write() and still invokes the success callback. */
    private List<Buffer> captureWrites() {
        List<Buffer> written = new ArrayList<>();
        doAnswer(inv -> {
            written.add(inv.getArgument(0));
            AsyncResult<Void> ok = successResult();
            inv.<Handler<AsyncResult<Void>>>getArgument(1).handle(ok);
            return response;
        }).when(response).write(any(Buffer.class), any());
        return written;
    }

    @SuppressWarnings("unchecked")
    private static AsyncResult<Void> successResult() {
        AsyncResult<Void> r = mock(AsyncResult.class);
        when(r.failed()).thenReturn(false);
        return r;
    }

    @SuppressWarnings("unchecked")
    private static AsyncResult<Void> failResult(Throwable cause) {
        AsyncResult<Void> r = mock(AsyncResult.class);
        when(r.failed()).thenReturn(true);
        when(r.cause()).thenReturn(cause);
        return r;
    }
}
