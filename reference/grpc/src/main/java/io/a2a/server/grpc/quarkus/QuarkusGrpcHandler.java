package io.a2a.server.grpc.quarkus;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.Task;
import io.a2a.grpc.TaskState;
import io.a2a.grpc.TaskStatus;
import io.a2a.grpc.TaskStatusUpdateEvent;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.GrpcHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class QuarkusGrpcHandler extends GrpcHandler {

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        super(agentCard, requestHandler);
    }
}