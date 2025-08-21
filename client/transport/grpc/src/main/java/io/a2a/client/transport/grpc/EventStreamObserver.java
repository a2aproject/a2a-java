package io.a2a.client.transport.grpc;


import io.a2a.grpc.StreamResponse;
import io.a2a.spec.StreamingEventKind;
import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;
import java.util.logging.Logger;

import static io.a2a.grpc.utils.ProtoUtils.FromProto;

public class EventStreamObserver implements StreamObserver<StreamResponse> {

    private static final Logger log = Logger.getLogger(EventStreamObserver.class.getName());
    private final Consumer<StreamingEventKind> eventHandler;
    private final Consumer<Throwable> errorHandler;

    public EventStreamObserver(Consumer<StreamingEventKind> eventHandler, Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public void onNext(StreamResponse response) {
        StreamingEventKind event;
        switch (response.getPayloadCase()) {
            case MSG:
                event = FromProto.message(response.getMsg());
                break;
            case TASK:
                event = FromProto.task(response.getTask());
                break;
            case STATUS_UPDATE:
                event = FromProto.taskStatusUpdateEvent(response.getStatusUpdate());
                break;
            case ARTIFACT_UPDATE:
                event = FromProto.taskArtifactUpdateEvent(response.getArtifactUpdate());
                break;
            default:
                log.warning("Invalid stream response " + response.getPayloadCase());
                errorHandler.accept(new IllegalStateException("Invalid stream response from server: " + response.getPayloadCase()));
                return;
        }
        eventHandler.accept(event);
    }

    @Override
    public void onError(Throwable t) {
        if (errorHandler != null) {
            // Map gRPC errors to proper A2A exceptions
            if (t instanceof io.grpc.StatusRuntimeException) {
                errorHandler.accept(GrpcErrorMapper.mapGrpcError((io.grpc.StatusRuntimeException) t));
            } else {
                errorHandler.accept(t);
            }
        }
    }

    @Override
    public void onCompleted() {
        // done
    }
}
