package org.a2aproject.sdk.server.grpc.quarkus.registry;

import java.util.Map;

/**
 * Registry for supporting multiple agents behind a single Quarkus gRPC service.
 * If a CDI bean implements this interface, incoming calls are dispatched to the agent
 * identified by the {@code X-A2A-Agent-Id} metadata header. Calls that don't carry the
 * header, or name an agent not present in the registry, fall back to the default
 * single-agent {@link org.a2aproject.sdk.spec.AgentCard} / {@link org.a2aproject.sdk.server.requesthandlers.RequestHandler}
 * beans, if configured.
 */
public interface MultiAgentRegistry {
    /**
     * @return a map of agent ID (as sent in the {@code X-A2A-Agent-Id} header) to their {@link GrpcAgent}
     */
    Map<String, GrpcAgent> getAgents();
}
