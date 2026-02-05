package io.a2a.client.transport.jsonrpc.sse;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.a2a.client.transport.spi.sse.AbstractSSEEventListener;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.spec.A2AError;
import io.a2a.spec.StreamingEventKind;
import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC transport implementation of SSE event listener.
 * Handles parsing of JSON-RPC formatted messages from SSE streams.
 */
public class SSEEventListener extends AbstractSSEEventListener {

    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());
    private volatile boolean completed = false;

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
            @Nullable Consumer<Throwable> errorHandler) {
        super(eventHandler, errorHandler);
    }

    @Override
    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        parseAndHandleMessage(message, completableFuture);
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
        if (getErrorHandler() != null) {
            log.fine("Calling errorHandler.accept(null) to signal successful completion");
            getErrorHandler().accept(null);
        } else {
            log.warning("errorHandler is null, cannot signal completion");
        }
    }

    /**
     * Parses a JSON-RPC message and delegates to the base class for event handling.
     *
     * @param message The raw JSON-RPC message string
     * @param future Optional future for controlling the SSE connection
     */
    private void parseAndHandleMessage(String message, @Nullable Future<Void> future) {
        try {
            StreamResponse response = JSONRPCUtils.parseResponseEvent(message);
            StreamingEventKind event = ProtoUtils.FromProto.streamingEventKind(response);
            
            // Delegate to base class for common event handling and auto-close logic
            handleEvent(event, future);
        } catch (A2AError error) {
            if (getErrorHandler() != null) {
                getErrorHandler().accept(error);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
