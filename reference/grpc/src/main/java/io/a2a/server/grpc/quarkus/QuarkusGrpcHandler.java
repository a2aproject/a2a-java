package io.a2a.server.grpc.quarkus;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.AgentCard;
import io.a2a.transport.grpc.handler.CallContextFactory;
import io.a2a.transport.grpc.handler.GrpcHandler;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;
import org.jspecify.annotations.Nullable;

@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor.class)
@Authenticated
public class QuarkusGrpcHandler extends GrpcHandler {

    private final AgentCard agentCard;
    private final AgentCard extendedAgentCard;
    private final RequestHandler requestHandler;
    private final Instance<CallContextFactory> callContextFactoryInstance;
    private final Executor executor;

    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */

    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard,
                              @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                              RequestHandler requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance,
                              @Internal Executor executor) {
        this.agentCard = agentCard;
        if (extendedAgentCard != null && extendedAgentCard.isResolvable()) {
            this.extendedAgentCard = extendedAgentCard.get();
        } else {
            this.extendedAgentCard = null;
        }
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
    protected AgentCard getExtendedAgentCard() {
        return extendedAgentCard;
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
