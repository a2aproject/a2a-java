package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;
import org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.transport.grpc.handler.CallContextFactory;
import org.a2aproject.sdk.compat03.transport.grpc.handler.GrpcHandler;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.util.async.Internal;

@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor.class)
@Authenticated
public class QuarkusGrpcHandler extends GrpcHandler {

    private final AgentCard agentCard;
    private final Instance<CallContextFactory> callContextFactoryInstance;
    private final Executor executor;

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard,
                              Convert03To10RequestHandler requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance,
                              @Internal Executor executor) {
        this.agentCard = agentCard;
        this.callContextFactoryInstance = callContextFactoryInstance;
        this.executor = executor;
        setRequestHandler(requestHandler);
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
