package org.a2aproject.sdk.compat03.client.transport.grpc;


import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3.FromProto;

public class EventStreamObserver_v0_3 implements StreamObserver<StreamResponse> {

    private static final Logger log = Logger.getLogger(EventStreamObserver_v0_3.class.getName());
    private final Consumer<StreamingEventKind_v0_3> eventHandler;
    private final Consumer<Throwable> errorHandler;

    public EventStreamObserver_v0_3(Consumer<StreamingEventKind_v0_3> eventHandler, Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public void onNext(StreamResponse response) {
        StreamingEventKind_v0_3 event;
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
                errorHandler.accept(GrpcErrorMapper_v0_3.mapGrpcError((io.grpc.StatusRuntimeException) t));
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
