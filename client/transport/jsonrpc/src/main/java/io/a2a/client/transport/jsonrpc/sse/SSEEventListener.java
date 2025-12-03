package io.a2a.client.transport.jsonrpc.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import org.jspecify.annotations.Nullable;

public class SSEEventListener {

    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final @Nullable
    Consumer<Throwable> errorHandler;
    private volatile boolean completed = false;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
            @Nullable Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        handleMessage(message, completableFuture);
    }

    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        if (future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    public void onComplete() {
        // Idempotent: only signal completion once, even if called multiple times
        if (completed) {
            log.fine("SSEEventListener.onComplete() called again - ignoring (already completed)");
            return;
        }
        completed = true;

        // Signal normal stream completion (null error means successful completion)
        log.fine("SSEEventListener.onComplete() called - signaling successful stream completion");
        if (errorHandler != null) {
            log.fine("Calling errorHandler.accept(null) to signal successful completion");
            errorHandler.accept(null);
        } else {
            log.warning("errorHandler is null, cannot signal completion");
        }
    }

    private void handleMessage(String message, @Nullable Future<Void> future) {
        try {
            StreamResponse response = JSONRPCUtils.parseResponseEvent(message);

            StreamingEventKind event = ProtoUtils.FromProto.streamingEventKind(response);
            eventHandler.accept(event);
            if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                if (future != null) {
                    future.cancel(true); // close SSE channel
                }
            }
        } catch (JSONRPCError error) {
            if (errorHandler != null) {
                errorHandler.accept(error);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
