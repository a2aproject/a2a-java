package io.a2a.client.transport.jsonrpc.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatusUpdateEvent;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static io.a2a.util.Utils.OBJECT_MAPPER;

public class SSEEventListener {
    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<Throwable> errorHandler;
    private volatile boolean completed = false;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
                            Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, Future<Void> completableFuture) {
        try {
            handleMessage(OBJECT_MAPPER.readTree(message),completableFuture);
        } catch (JsonProcessingException e) {
            log.warning("Failed to parse JSON message: " + message);
        }
    }

    public void onError(Throwable throwable, Future<Void> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        future.cancel(true); // close SSE channel
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

    private void handleMessage(JsonNode jsonNode, Future<Void> future) {
        try {
            if (jsonNode.has("error")) {
                JSONRPCError error = OBJECT_MAPPER.treeToValue(jsonNode.get("error"), JSONRPCError.class);
                if (errorHandler != null) {
                    errorHandler.accept(error);
                }
            } else if (jsonNode.has("result")) {
                // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
                JsonNode result = jsonNode.path("result");
                StreamingEventKind event = OBJECT_MAPPER.treeToValue(result, StreamingEventKind.class);
                eventHandler.accept(event);
                if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                    future.cancel(true); // close SSE channel
                }
            } else {
                throw new IllegalArgumentException("Unknown message type");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
