package org.a2aproject.sdk.client.transport.jsonrpc.sse;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.transport.spi.sse.AbstractSSEEventListener;
import org.a2aproject.sdk.grpc.StreamResponse;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC transport implementation of SSE event listener.
 * Handles parsing of JSON-RPC formatted messages from SSE streams.
 */
public class SSEEventListener extends AbstractSSEEventListener {

    private static final Logger LOGGER = Logger.getLogger(SSEEventListener.class.getName());
    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
            @Nullable Consumer<Throwable> errorHandler) {
        super(eventHandler, errorHandler);
    }

    @Override
    public void onMessage(ServerSentEvent event, @Nullable Future<Void> completableFuture) {
        parseAndHandleMessage(event.data(), completableFuture);
    }

    public void onComplete() {
        LOGGER.fine("SSEEventListener.onComplete() called - signaling successful stream completion");
        signalTerminal(null);
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
            signalTerminal(error);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
