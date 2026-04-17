package org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.a2aproject.sdk.compat03.json.JsonProcessingException;
import org.a2aproject.sdk.compat03.json.JsonUtil;
import org.a2aproject.sdk.compat03.spec.JSONRPCError;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

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
            handleMessage(JsonParser.parseString(message).getAsJsonObject(), completableFuture);
        } catch (JsonSyntaxException e) {
            log.warning("Failed to parse JSON message: " + message);
        } catch (JsonProcessingException e) {
            log.warning("Failed to process JSON message: " + message);
        } catch (IllegalArgumentException e) {
            log.warning("Invalid message format: " + message);
            if (errorHandler != null) {
                errorHandler.accept(e);
            }
            completableFuture.cancel(true); // close SSE channel
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

    private void handleMessage(JsonObject jsonObject, Future<Void> future) throws JsonProcessingException {
        if (jsonObject.has("error")) {
            JSONRPCError error = JsonUtil.fromJson(jsonObject.get("error").toString(), JSONRPCError.class);
            if (errorHandler != null) {
                errorHandler.accept(error);
            }
        } else if (jsonObject.has("result")) {
            // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
            String resultJson = jsonObject.get("result").toString();
            StreamingEventKind event = JsonUtil.fromJson(resultJson, StreamingEventKind.class);
            eventHandler.accept(event);
            if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent) event).isFinal()) {
                future.cancel(true); // close SSE channel
            }
        } else {
            throw new IllegalArgumentException("Unknown message type");
        }
    }

}
