package org.a2aproject.sdk.compat03.client.transport.rest.sse;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.client.transport.rest.RestErrorMapper_v0_3;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.jspecify.annotations.Nullable;

public class RestSSEEventListener_v0_3 {

    private static final Logger log = Logger.getLogger(RestSSEEventListener_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;

    public RestSSEEventListener_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler,
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
            errorHandler.accept(RestErrorMapper_v0_3.mapRestError(message, 500));
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
        StreamingEventKind_v0_3 event;
        switch (response.getPayloadCase()) {
            case MSG ->
                event = ProtoUtils_v0_3.FromProto.message(response.getMsg());
            case TASK ->
                event = ProtoUtils_v0_3.FromProto.task(response.getTask());
            case STATUS_UPDATE ->
                event = ProtoUtils_v0_3.FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
            case ARTIFACT_UPDATE ->
                event = ProtoUtils_v0_3.FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
            default -> {
                log.warning("Invalid stream response " + response.getPayloadCase());
                errorHandler.accept(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                return;
            }
        }
        eventHandler.accept(event);
    }

}
