package org.a2aproject.sdk.server.apps.quarkus.registry;

import java.util.Map;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;

/**
 * Registry for supporting multiple agents in a single Quarkus application.
 * If a CDI bean implements this interface, the server will register routes for each
 * agent in the registry under {@code /<agent-id>/} and {@code /<agent-id>/.well-known/agent-card.json}.
 */
public interface MultiAgentRegistry {
    /**
     * @return a map of agent ID (path segment) to their JSONRPCHandler
     */
    Map<String, JSONRPCHandler> getAgents();
}
