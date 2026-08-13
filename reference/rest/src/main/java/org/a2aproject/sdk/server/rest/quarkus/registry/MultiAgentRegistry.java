package org.a2aproject.sdk.server.rest.quarkus.registry;

import java.util.Map;
import org.a2aproject.sdk.transport.rest.handler.RestHandler;

/**
 * Registry for supporting multiple agents in a single Quarkus REST application.
 * If a CDI bean implements this interface, the server will register routes for each
 * agent in the registry under {@code /<agent-id>/} and {@code /<agent-id>/.well-known/agent-card.json}.
 */
public interface MultiAgentRegistry {
    /**
     * @return a map of agent ID (path segment) to their RestHandler
     */
    Map<String, RestHandler> getAgents();
}
