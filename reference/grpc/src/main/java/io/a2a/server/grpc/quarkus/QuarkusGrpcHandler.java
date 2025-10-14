package io.a2a.server.grpc.quarkus;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.concurrent.Executor;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.AgentCard;
import io.a2a.transport.grpc.handler.CallContextFactory;
import io.a2a.transport.grpc.handler.GrpcHandler;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;

@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor.class)
@Authenticated
public class QuarkusGrpcHandler extends GrpcHandler {

    private final AgentCard agentCard;
    private final RequestHandler requestHandler;
    private final Instance<CallContextFactory> callContextFactoryInstance;
    private final Executor executor;

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard,
                              RequestHandler requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance,
                              @Internal Executor executor) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
        this.callContextFactoryInstance = callContextFactoryInstance;
        this.executor = executor;
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

    @Override
    protected Executor getExecutor() {
        return executor;
    }
}
