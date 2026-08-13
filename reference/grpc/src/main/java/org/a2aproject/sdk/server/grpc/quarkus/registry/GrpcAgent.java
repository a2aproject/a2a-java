package org.a2aproject.sdk.server.grpc.quarkus.registry;

import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Bundles the pieces needed to serve a single agent over gRPC: its request handler and
 * agent card(s). Used by {@link MultiAgentRegistry} to describe each registered agent.
 */
public record GrpcAgent(AgentCard agentCard, @Nullable AgentCard extendedAgentCard, RequestHandler requestHandler) {
    public GrpcAgent {
        Assert.checkNotNullParam("agentCard", agentCard);
        Assert.checkNotNullParam("requestHandler", requestHandler);
    }
}
