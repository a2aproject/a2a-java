package org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.ResponseCorrelation_v0_3;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class SSEEventListener_v0_3 {
    private static final Logger LOGGER = Logger.getLogger(SSEEventListener_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;
    private final JsonElement expectedId;
    private final AtomicBoolean terminalSignaled = new AtomicBoolean(false);
    /**
     * Set as soon as this listener decides the stream is over, which can happen before the future
     * returned by {@code postAsyncSSE} exists; {@link #onStreamFuturePublished} replays it then.
     */
    private final AtomicBoolean closeRequested = new AtomicBoolean(false);
    /** The future returned by {@code postAsyncSSE}, published by the transport after that call returns. */
    private final AtomicReference<Future<Void>> streamFuture = new AtomicReference<>();

    public SSEEventListener_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler,
                                 Consumer<Throwable> errorHandler) {
        this(eventHandler, errorHandler, null);
    }

    public SSEEventListener_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler,
                                 Consumer<Throwable> errorHandler, JsonElement expectedId) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.expectedId = expectedId;
    }

    /**
     * @param completableFuture the streaming future, or {@code null} while {@code postAsyncSSE} has
     *                          not returned yet
     */
    public void onMessage(String message, Future<Void> completableFuture) {
        if (terminalSignaled.get()) {
            return;
        }
        try {
            handleMessage(JsonParser.parseString(message).getAsJsonObject(), completableFuture);
        } catch (JsonSyntaxException e) {
            LOGGER.warning("Failed to parse JSON message: " + message);
        } catch (JsonProcessingException_v0_3 e) {
            LOGGER.warning("Failed to process JSON message: " + message);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid message format: " + message);
            signalTerminal(e);
            requestClose(completableFuture);
        }
    }

    /**
     * @param future the streaming future, or {@code null} while {@code postAsyncSSE} has not
     *               returned yet
     */
    public void onError(Throwable throwable, Future<Void> future) {
        signalTerminal(throwable);
        requestClose(future);
    }

    /**
     * Hands this listener the future returned by {@code postAsyncSSE}.
     *
     * <p>That future does not exist while the call is still in flight, so a terminal event that
     * arrives early cannot close the channel from {@link #onMessage} or {@link #onError}. The
     * close request recorded in that case is honoured here.
     *
     * @param future the streaming future, or {@code null} if the client did not return one
     */
    public void onStreamFuturePublished(Future<Void> future) {
        // Publish first, then read the flag: a concurrent requestClose either sees the future
        // here or is seen below, so the close request is never lost.
        streamFuture.set(future);
        if (closeRequested.get() && future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    private void requestClose(Future<Void> future) {
        // Set the flag before reading the future, mirroring onStreamFuturePublished.
        closeRequested.set(true);
        Future<Void> published = streamFuture.get();
        Future<Void> target = published != null ? published : future;
        if (target != null) {
            target.cancel(true); // close SSE channel
        }
    }

    private void signalTerminal(Throwable error) {
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

    public void onComplete() {
        LOGGER.fine("SSEEventListener.onComplete() called - signaling successful stream completion");
        signalTerminal(null);
    }

    private void handleMessage(JsonObject jsonObject, Future<Void> future) throws JsonProcessingException_v0_3 {
        if (expectedId != null) {
            ResponseCorrelation_v0_3.validate(jsonObject, expectedId);
        }
        if (jsonObject.has("error")) {
            JSONRPCError_v0_3 error = JsonUtil_v0_3.fromJson(jsonObject.get("error").toString(), JSONRPCError_v0_3.class);
            signalTerminal(error);
            requestClose(future);
        } else if (jsonObject.has("result")) {
            // result can be a Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent
            String resultJson = jsonObject.get("result").toString();
            StreamingEventKind_v0_3 event = JsonUtil_v0_3.fromJson(resultJson, StreamingEventKind_v0_3.class);
            eventHandler.accept(event);
            if (shouldAutoClose(event)) {
                signalTerminal(null);
                requestClose(future);
            }
        } else {
            throw new IllegalArgumentException("Unknown message type");
        }
    }

    /**
     * Determines whether the stream is finished, using the same predicate as
     * {@code AbstractSSEEventListener.shouldAutoClose} in the native transport: a final status
     * update, or a task snapshot that is already in a final state.
     *
     * @param event the streaming event to check
     * @return {@code true} when the connection should be closed, {@code false} otherwise
     */
    private boolean shouldAutoClose(StreamingEventKind_v0_3 event) {
        if (event instanceof TaskStatusUpdateEvent_v0_3 tsue && tsue.isFinal()) {
            return true;
        }
        if (event instanceof Task_v0_3 task) {
            return task.status().state().isFinal();
        }
        return false;
    }

}
