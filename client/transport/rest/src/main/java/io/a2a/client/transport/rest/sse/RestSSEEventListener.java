package io.a2a.client.transport.rest.sse;

import static io.a2a.grpc.StreamResponse.PayloadCase.ARTIFACT_UPDATE;
import static io.a2a.grpc.StreamResponse.PayloadCase.MSG;
import static io.a2a.grpc.StreamResponse.PayloadCase.STATUS_UPDATE;
import static io.a2a.grpc.StreamResponse.PayloadCase.TASK;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.client.transport.rest.RestErrorMapper;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.spec.StreamingEventKind;

public class RestSSEEventListener {

    private static final Logger log = Logger.getLogger(RestSSEEventListener.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<Throwable> errorHandler;

    public RestSSEEventListener(Consumer<StreamingEventKind> eventHandler,
            Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    public void onMessage(String message, Future<Void> completableFuture) {
        try {
            System.out.println("Streaming message received: " + message);
            io.a2a.grpc.StreamResponse.Builder builder = io.a2a.grpc.StreamResponse.newBuilder();
            JsonFormat.parser().merge(message, builder);
            handleMessage(builder.build(), completableFuture);
        } catch (InvalidProtocolBufferException e) {
            errorHandler.accept(RestErrorMapper.mapRestError(message, 500));
        }
    }

    public void onError(Throwable throwable, Future<Void> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        future.cancel(true); // close SSE channel
    }

    private void handleMessage(StreamResponse response, Future<Void> future) {
        StreamingEventKind event;
        switch (response.getPayloadCase()) {
            case MSG:
                event = ProtoUtils.FromProto.message(response.getMsg());
                break;
            case TASK:
                event = ProtoUtils.FromProto.task(response.getTask());
                break;
            case STATUS_UPDATE:
                event = ProtoUtils.FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
                break;
            case ARTIFACT_UPDATE:
                event = ProtoUtils.FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
                break;
            default:
                log.warning("Invalid stream response " + response.getPayloadCase());
                errorHandler.accept(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                return;
        }
        eventHandler.accept(event);
    }

}
