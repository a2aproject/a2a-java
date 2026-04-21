package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.transport.grpc.handler.CallContextFactory_v0_3;
import org.a2aproject.sdk.compat03.transport.grpc.handler.GrpcHandler_v0_3;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.util.async.Internal;

@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor_v0_3.class)
@Authenticated
public class QuarkusGrpcHandler_v0_3 extends GrpcHandler_v0_3 {

    private final AgentCard_v0_3 agentCard;
    private final Instance<CallContextFactory_v0_3> callContextFactoryInstance;
    private final Executor executor;

    @Inject
    public QuarkusGrpcHandler_v0_3(@PublicAgentCard AgentCard_v0_3 agentCard,
                                   Convert_v0_3_To10RequestHandler requestHandler,
                                   Instance<CallContextFactory_v0_3> callContextFactoryInstance,
                                   @Internal Executor executor) {
        this.agentCard = agentCard;
        this.callContextFactoryInstance = callContextFactoryInstance;
        this.executor = executor;
        setRequestHandler(requestHandler);
    }

    @Override
    protected AgentCard_v0_3 getAgentCard() {
        return agentCard;
    }

    @Override
    protected CallContextFactory_v0_3 getCallContextFactory() {
        return callContextFactoryInstance.isUnsatisfied() ? null : callContextFactoryInstance.get();
    }

    @Override
    protected Executor getExecutor() {
        return executor;
    }
}
