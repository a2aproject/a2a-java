package org.a2aproject.sdk.compat03.transport.grpc.handler;

import jakarta.enterprise.inject.Vetoed;

import java.util.concurrent.Executor;

import org.a2aproject.sdk.compat03.spec.AgentCard;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * Abstract gRPC handler for v0.3 protocol.
 * TODO: Port full implementation with translation layer from v0.3 types to current SDK types
 */
@Vetoed
public abstract class GrpcHandler extends org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc.A2AServiceImplBase {

    // TODO: Translation layer - translate from v0.3 types to current SDK types before calling requestHandler
    // protected abstract RequestHandler getRequestHandler();

    protected abstract AgentCard getAgentCard();

    protected abstract CallContextFactory getCallContextFactory();

    protected abstract Executor getExecutor();

    // TODO: Implement all gRPC service methods with translation layer
    // For now, all methods return UNIMPLEMENTED

    @Override
    public void sendMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                           StreamObserver<org.a2aproject.sdk.compat03.grpc.SendMessageResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void getTask(org.a2aproject.sdk.compat03.grpc.GetTaskRequest request,
                       StreamObserver<org.a2aproject.sdk.compat03.grpc.Task> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void cancelTask(org.a2aproject.sdk.compat03.grpc.CancelTaskRequest request,
                          StreamObserver<org.a2aproject.sdk.compat03.grpc.Task> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void createTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest request,
                                               StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void getTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest request,
                                            StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void listTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest request,
                                             StreamObserver<org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void sendStreamingMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                                     StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void taskSubscription(org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest request,
                                 StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void getAgentCard(org.a2aproject.sdk.compat03.grpc.GetAgentCardRequest request,
                           StreamObserver<org.a2aproject.sdk.compat03.grpc.AgentCard> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }

    @Override
    public void deleteTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest request,
                                               StreamObserver<com.google.protobuf.Empty> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED
                .withDescription("Translation layer not yet implemented")
                .asRuntimeException());
    }
}
