---
title: "Multi-Agent Deployments"
description: "Best practices and reference architectures for scaling A2A Server Agents by hosting multiple agents in a single Quarkus application."
---

When scaling an A2A production deployment, a common requirement is exposing multiple, distinct Server Agents (each with its own `AgentCard` and specific behavior) from the same underlying application platform.

While you could deploy a separate Quarkus application and Kubernetes pod for every single agent (a 1:1 mapping) and route traffic via an Ingress, this can introduce significant operational overhead and resource consumption when the number of agents grows into the hundreds.

A more efficient reference architecture is to host **multiple agents within a single Quarkus application**.

## Reference Architecture: Single App, Multiple Agents

The default `a2a-java` reference server implementation (`reference-jsonrpc`) is designed to expose a single agent at the root context path (`/`) using CDI injection for a single `JSONRPCHandler` and `AgentCard`.

To support multiple agents, you should bypass the default reference routes and manually register Vert.x or JAX-RS routes for each agent. This allows you to namespace each agent under its own URL path (e.g., `/agent-1`, `/agent-2`).

### 1. Manual Instantiation

Instead of relying solely on `@ApplicationScoped` CDI injection which enforces a singleton pattern, you can manually instantiate the `JSONRPCHandler` and `RequestHandler` (like `DefaultRequestHandler`) for each agent you want to host.

Each agent will have its own:
*   `AgentCard`
*   `AgentExecutor` (your business logic)
*   `JSONRPCHandler`

### 2. Custom Routing Configuration

You can use a Vert.x `Router` observer to dynamically register routes for all your agents at application startup.

```java
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class MultiAgentRouter {

    @Inject
    Router router;

    // A service that discovers or provides the configuration for all agents
    @Inject
    AgentRegistry agentRegistry; 

    void setupRoutes(@Observes StartupEvent ev) {
        Map<String, JSONRPCHandler> agents = agentRegistry.getAllAgents();

        for (Map.Entry<String, JSONRPCHandler> entry : agents.entrySet()) {
            String agentId = entry.getKey(); // e.g., "binance", "github"
            JSONRPCHandler handler = entry.getValue();

            // 1. Expose the specific Agent Card for this agent
            router.get("/" + agentId + "/.well-known/agent-card.json")
                  .produces("application/json")
                  .handler(ctx -> {
                      ctx.response().end(JsonUtil.toJson(handler.getAgentCard()));
                  });

            // 2. Expose the JSON-RPC endpoint for this agent
            router.post("/" + agentId)
                  .consumes("application/json")
                  .handler(BodyHandler.create())
                  .blockingHandler(ctx -> {
                      // Process the request using this specific agent's handler
                      // (Similar to the logic in A2AServerRoutes.java)
                      // ...
                  }, false);
        }
    }
}
```

### 3. Benefits

*   **Resource Efficiency**: You only pay the JVM and Quarkus startup overhead once, regardless of how many agents you host.
*   **Shared Resources**: Database connection pools, HTTP clients, and Thread pools can be shared across all agents.
*   **Dynamic Provisioning**: By reading agent configurations from a database at startup, you can dynamically spin up new agents without redeploying the application.

### Important Considerations

When deploying this architecture:
*   Ensure that your reverse proxy or Ingress controller correctly routes requests to the appropriately namespaced URLs (`/agent-1/.well-known/agent-card.json`).
*   Be mindful of shared state. Ensure your `AgentExecutor` implementations are thread-safe and do not leak data between different agent contexts.
