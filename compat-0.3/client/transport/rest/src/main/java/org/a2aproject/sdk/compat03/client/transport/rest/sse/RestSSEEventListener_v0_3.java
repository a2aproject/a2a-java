package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.client.transport.rest.RestErrorMapper_v0_3;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.jspecify.annotations.Nullable;

public class RestSSEEventListener_v0_3 {

    private static final Logger LOGGER = Logger.getLogger(RestSSEEventListener_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;
    private final AtomicBoolean terminalSignaled = new AtomicBoolean(false);

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
            handleMessage(builder.build());
        } catch (InvalidProtocolBufferException e) {
            signalTerminal(RestErrorMapper_v0_3.mapRestError(message, 500));
        }
    }

    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        signalTerminal(throwable);
        if (future != null) {
            future.cancel(true); // close SSE channel
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

    private void handleMessage(StreamResponse response) {
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
        eventHandler.accept(event);
    }

}
