package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.client.transport.rest.RestErrorMapper_v0_3;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.jspecify.annotations.Nullable;

public class RestSSEEventListener_v0_3 {

    private static final Logger LOGGER = Logger.getLogger(RestSSEEventListener_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;
    private final AtomicBoolean terminalSignaled = new AtomicBoolean(false);
    /**
     * Set when the stream ends before the transport has a future to cancel. The future returned by
     * {@code postAsyncSSE} only exists after that call returns, so an early terminal event has
     * nothing to cancel yet; {@link #onStreamFuturePublished} replays it then.
     */
    private final AtomicBoolean closeRequested = new AtomicBoolean(false);
    /** The future returned by {@code postAsyncSSE}, published by the transport after that call returns. */
    private final AtomicReference<Future<Void>> streamFuture = new AtomicReference<>();

    public RestSSEEventListener_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler,
                                     Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        try {
            LOGGER.fine("Streaming message received: " + message);
            org.a2aproject.sdk.compat03.grpc.StreamResponse.Builder builder = org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder();
            JsonFormat.parser().merge(message, builder);
            handleMessage(builder.build(), completableFuture);
        } catch (InvalidProtocolBufferException e) {
            signalTerminal(RestErrorMapper_v0_3.mapRestError(message, 500));
        }
    }

    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        signalTerminal(throwable);
        requestClose(future);
    }

    /**
     * Hands this listener the future returned by {@code postAsyncSSE}.
     *
     * <p>That future does not exist while the call is still in flight, so an end-of-stream event
     * delivered during the call can only record the intent to close; this method performs the
     * cancellation once the future shows up. Without it the client would keep a finished stream
     * open forever, because REST never replays the request.
     *
     * @param future the streaming future returned by {@code postAsyncSSE}
     */
    public void onStreamFuturePublished(Future<Void> future) {
        // Publish first, then read the flag: a concurrent requestClose either sees the future
        // here or is seen below, so the close request is never lost.
        streamFuture.set(future);
        if (closeRequested.get() && future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    private void requestClose(@Nullable Future<Void> future) {
        // Set the flag before reading the future, mirroring onStreamFuturePublished.
        closeRequested.set(true);
        Future<Void> published = streamFuture.get();
        Future<Void> target = published != null ? published : future;
        if (target != null) {
            target.cancel(true); // close SSE channel
        }
    }

    /**
     * Signals that the server ended the stream normally. The terminal callback is delivered with
     * {@code null}, the same way the native REST listener and the 0.3 JSON-RPC listener report a
     * successful completion, so a caller can tell "stream finished" apart from "stream failed".
     */
    public void onComplete() {
        LOGGER.fine("RestSSEEventListener_v0_3.onComplete() called - signaling successful stream completion");
        signalTerminal(null);
    }

    /**
     * Delivers exactly one terminal callback for the stream. The first caller wins, so a completion
     * that arrives after an error (or the other way round) does not deliver a second outcome.
     *
     * @param error the failure to report, or {@code null} to signal normal completion
     */
    private void signalTerminal(@Nullable Throwable error) {
        if (!terminalSignaled.compareAndSet(false, true)) {
            LOGGER.fine("Terminal callback already delivered, ignoring subsequent signal");
            return;
        }
        if (errorHandler != null) {
            errorHandler.accept(error);
        } else if (error != null) {
            LOGGER.warning("errorHandler is null, cannot report terminal error");
        }
    }

    private void handleMessage(StreamResponse response, @Nullable Future<Void> future) {
        StreamingEventKind_v0_3 event;
        switch (response.getPayloadCase()) {
            case MSG ->
                event = ProtoUtils_v0_3.FromProto.message(response.getMsg());
            case TASK ->
                event = ProtoUtils_v0_3.FromProto.task(response.getTask());
            case STATUS_UPDATE ->
                event = ProtoUtils_v0_3.FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
            case ARTIFACT_UPDATE ->
                event = ProtoUtils_v0_3.FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
            default -> {
                LOGGER.warning("Invalid stream response " + response.getPayloadCase());
                signalTerminal(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                return;
            }
        }
        handleEvent(event, future);
    }

    /**
     * Delivers the event and closes the connection once the event carries the end of the task.
     * <p>
     * This mirrors {@code AbstractSSEEventListener.handleEvent}, which the native REST and JSON-RPC
     * listeners inherit: a stream that reaches a final state is cancelled client side instead of
     * waiting for the server to tear the connection down. That wait never ends when the server keeps
     * the response open, which is what happens on a late resubscription to an already finished task.
     * <p>
     * The terminal callback is delivered even when the future has not been published yet; only the
     * cancellation is deferred to {@link #onStreamFuturePublished}. Skipping both used to lose the
     * completion signal for a final event that arrives while {@code postAsyncSSE} is still running.
     *
     * @param event the parsed streaming event
     * @param future optional future for controlling the SSE connection, {@code null} while
     *               {@code postAsyncSSE} has not returned yet
     */
    private void handleEvent(StreamingEventKind_v0_3 event, @Nullable Future<Void> future) {
        eventHandler.accept(event);

        if (shouldAutoClose(event)) {
            LOGGER.fine("Auto-closing SSE connection for final event: " + event.getClass().getSimpleName());
            signalTerminal(null);
            requestClose(future);
        }
    }

    /**
     * Determines whether the stream is finished, using the same predicate as
     * {@code AbstractSSEEventListener.shouldAutoClose}: a final status update, or a task snapshot
     * that is already in a final state.
     *
     * @param event the streaming event to check
     * @return {@code true} when the connection should be closed, {@code false} otherwise
     */
    private boolean shouldAutoClose(StreamingEventKind_v0_3 event) {
        if (event instanceof TaskStatusUpdateEvent_v0_3 tue && tue.isFinal()) {
            return true;
        }
        if (event instanceof Task_v0_3 task) {
            return task.status().state().isFinal();
        }
        return false;
    }

}
