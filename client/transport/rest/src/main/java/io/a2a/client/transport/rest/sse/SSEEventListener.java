package io.a2a.client.transport.rest.sse;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.client.transport.spi.sse.AbstractSSEEventListener;
import io.a2a.client.transport.rest.RestErrorMapper;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.StreamingEventKind;
import org.jspecify.annotations.Nullable;

/**
 * REST transport implementation of SSE event listener.
 * Handles parsing of JSON-formatted protobuf messages from REST SSE streams.
 */
public class SSEEventListener extends AbstractSSEEventListener {

    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());

    public SSEEventListener(Consumer<StreamingEventKind> eventHandler,
            @Nullable Consumer<Throwable> errorHandler) {
        super(eventHandler, errorHandler);
    }

    @Override
    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        try {
            log.fine("Streaming message received: " + message);
            io.a2a.grpc.StreamResponse.Builder builder = io.a2a.grpc.StreamResponse.newBuilder();
            JsonFormat.parser().merge(message, builder);
            parseAndHandleMessage(builder.build(), completableFuture);
        } catch (InvalidProtocolBufferException e) {
            if (getErrorHandler() != null) {
                getErrorHandler().accept(RestErrorMapper.mapRestError(message, 500));
            }
        }
    }

    /**
     * Parses a StreamResponse protobuf message and delegates to the base class for event handling.
     *
     * @param response The parsed StreamResponse
     * @param future Optional future for controlling the SSE connection
     */
    private void parseAndHandleMessage(StreamResponse response, @Nullable Future<Void> future) {
        StreamingEventKind event;
        switch (response.getPayloadCase()) {
            case MESSAGE ->
                event = ProtoUtils.FromProto.message(response.getMessage());
            case TASK ->
                event = ProtoUtils.FromProto.task(response.getTask());
            case STATUS_UPDATE ->
                event = ProtoUtils.FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
            case ARTIFACT_UPDATE ->
                event = ProtoUtils.FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
            default -> {
                log.warning("Invalid stream response " + response.getPayloadCase());
                if (getErrorHandler() != null) {
                    getErrorHandler().accept(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                }
                return;
            }
        }
        
        // Delegate to base class for common event handling and auto-close logic
        handleEvent(event, future);
    }

}
