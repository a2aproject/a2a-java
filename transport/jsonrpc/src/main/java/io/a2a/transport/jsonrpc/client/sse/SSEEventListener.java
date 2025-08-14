package io.a2a.transport.jsonrpc.client.sse;

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
    private final Consumer<JSONRPCError> errorHandler;
    private final Runnable failureHandler;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler, Consumer<JSONRPCError> errorHandler, Runnable failureHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
        this.failureHandler = failureHandler;
    }

    public void onMessage(String message, Future<Void> completableFuture) {
        try {
            handleMessage(OBJECT_MAPPER.readTree(message),completableFuture);
        } catch (JsonProcessingException e) {
            log.warning("Failed to parse JSON message: " + message);
        }
    }

    public void onError(Throwable throwable, Future<Void> future) {
        failureHandler.run();
        future.cancel(true); // close SSE channel
    }

    private void handleMessage(JsonNode jsonNode, Future<Void> future) {
        try {
            if (jsonNode.has("error")) {
                JSONRPCError error = OBJECT_MAPPER.treeToValue(jsonNode.get("error"), JSONRPCError.class);
                errorHandler.accept(error);
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
