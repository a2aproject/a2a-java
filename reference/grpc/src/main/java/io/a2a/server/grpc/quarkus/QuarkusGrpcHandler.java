package io.a2a.server.grpc.quarkus;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.grpc.handler.GrpcHandler;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.CallContextFactory;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class QuarkusGrpcHandler extends GrpcHandler {

    private final AgentCard agentCard;
    private final RequestHandler requestHandler;
    private final Instance<CallContextFactory> callContextFactoryInstance;

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard,
                              RequestHandler requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
        this.callContextFactoryInstance = callContextFactoryInstance;
    }

    @Override
    protected RequestHandler getRequestHandler() {
        return requestHandler;
    }

    @Override
    protected AgentCard getAgentCard() {
        return agentCard;
    }

    @Override
    protected CallContextFactory getCallContextFactory() {
        return callContextFactoryInstance.isUnsatisfied() ? null : callContextFactoryInstance.get();
    }
}