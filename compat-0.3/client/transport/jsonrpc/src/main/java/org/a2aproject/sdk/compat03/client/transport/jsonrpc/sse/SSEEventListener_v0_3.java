package org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SSEEventListener_v0_3 {
    private static final Logger LOGGER = Logger.getLogger(SSEEventListener_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;
    private volatile boolean completed = false;

    public SSEEventListener_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler,
                                 Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        try {
            handleMessage(JsonParser.parseString(message).getAsJsonObject(), completableFuture);
        } catch (JsonSyntaxException e) {
            fail(e, completableFuture);
        } catch (JsonProcessingException_v0_3 e) {
            fail(e, completableFuture);
        } catch (IllegalArgumentException e) {
            fail(e, completableFuture);
        } catch (IllegalStateException e) {
            fail(e, completableFuture);
        }
    }

    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        completed = true;
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
            LOGGER.fine("SSEEventListener.onComplete() called again - ignoring (already completed)");
            return;
        }
        completed = true;

        // Signal normal stream completion (null error means successful completion)
        LOGGER.fine("SSEEventListener.onComplete() called - signaling successful stream completion");
        if (errorHandler != null) {
            LOGGER.fine("Calling errorHandler.accept(null) to signal successful completion");
            errorHandler.accept(null);
        } else {
            LOGGER.warning("errorHandler is null, cannot signal completion");
        }
    }

    private void handleMessage(JsonObject jsonObject, @Nullable Future<Void> future) throws JsonProcessingException_v0_3 {
        if (jsonObject.has("error")) {
            completed = true;
            JSONRPCError_v0_3 error = JsonUtil_v0_3.fromJson(jsonObject.get("error").toString(), JSONRPCError_v0_3.class);
            if (errorHandler != null) {
                errorHandler.accept(error);
            }
            if (future != null) {
                future.cancel(true); // close SSE channel
            }
        } else if (jsonObject.has("result")) {
            // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
            String resultJson = jsonObject.get("result").toString();
            StreamingEventKind_v0_3 event = JsonUtil_v0_3.fromJson(resultJson, StreamingEventKind_v0_3.class);
            eventHandler.accept(event);
            if ((event instanceof TaskStatusUpdateEvent_v0_3 tsue && tsue.isFinal())
                    || (event instanceof Task_v0_3 task && task.status().state().isFinal())) {
                if (future != null) {
                    future.cancel(true); // close SSE channel
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown message type");
        }
    }

    private void fail(Throwable throwable, @Nullable Future<Void> future) {
        completed = true;
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        if (future != null) {
            future.cancel(true);
        }
    }

}
