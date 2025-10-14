package io.a2a.client.transport.jsonrpc.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.http.sse.DataEvent;
import io.a2a.client.http.sse.Event;
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

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
                            Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(Event event, Future<HttpResponse> completableFuture) {
        log.fine("Streaming message received: " + event);

        if (event instanceof DataEvent) {
            try {
                handleMessage(OBJECT_MAPPER.readTree(((DataEvent) event).getData()), completableFuture);
            } catch (JsonProcessingException e) {
                log.warning("Failed to parse JSON message: " + ((DataEvent) event).getData());
            }
        }
    }

    public void onError(Throwable throwable, Future<HttpResponse> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        if (future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    private void handleMessage(JsonNode jsonNode, Future<HttpResponse> future) {
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
