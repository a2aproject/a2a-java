package io.a2a.server.grpc.quarkus;

import jakarta.inject.Inject;

import io.a2a.server.PublicAgentCard;
import io.a2a.grpc.handler.GrpcHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class QuarkusGrpcHandler extends GrpcHandler {

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        super(agentCard, requestHandler);
    }
}