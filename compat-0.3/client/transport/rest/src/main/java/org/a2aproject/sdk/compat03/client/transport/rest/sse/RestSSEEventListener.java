package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import static org.a2aproject.sdk.compat03.grpc.StreamResponse.PayloadCase.ARTIFACT_UPDATE;
import static org.a2aproject.sdk.compat03.grpc.StreamResponse.PayloadCase.MSG;
import static org.a2aproject.sdk.compat03.grpc.StreamResponse.PayloadCase.STATUS_UPDATE;
import static org.a2aproject.sdk.compat03.grpc.StreamResponse.PayloadCase.TASK;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.client.transport.rest.RestErrorMapper;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.jspecify.annotations.Nullable;

public class RestSSEEventListener {

    private static final Logger log = Logger.getLogger(RestSSEEventListener.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<Throwable> errorHandler;

    public RestSSEEventListener(Consumer<StreamingEventKind> eventHandler,
            Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, @Nullable Future<Void> completableFuture) {
        try {
            log.fine("Streaming message received: " + message);
            org.a2aproject.sdk.compat03.grpc.StreamResponse.Builder builder = org.a2aproject.sdk.compat03.grpc.StreamResponse.newBuilder();
            JsonFormat.parser().merge(message, builder);
            handleMessage(builder.build());
        } catch (InvalidProtocolBufferException e) {
            errorHandler.accept(RestErrorMapper.mapRestError(message, 500));
        }
    }

    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        if (future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    private void handleMessage(StreamResponse response) {
        StreamingEventKind event;
        switch (response.getPayloadCase()) {
            case MSG ->
                event = ProtoUtils.FromProto.message(response.getMsg());
            case TASK ->
                event = ProtoUtils.FromProto.task(response.getTask());
            case STATUS_UPDATE ->
                event = ProtoUtils.FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
            case ARTIFACT_UPDATE ->
                event = ProtoUtils.FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
            default -> {
                log.warning("Invalid stream response " + response.getPayloadCase());
                errorHandler.accept(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                return;
            }
        }
        eventHandler.accept(event);
    }

}
