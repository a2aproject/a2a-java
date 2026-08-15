---
title: "Multi-Agent Deployments"
description: "Host multiple A2A Server Agents, each with its own AgentCard, in a single Quarkus application."
layout: page
---

# Multi-Agent Deployments

A common requirement when scaling an A2A deployment is exposing multiple, distinct Server Agents — each with its own `AgentCard` and behaviour — from one application.

Deploying a separate Quarkus application and Kubernetes pod per agent works, but the JVM and container overhead adds up quickly once the agent count grows into the hundreds. The alternative is to host many agents inside a **single application**, each namespaced under its own path.

The SDK supports this directly through the `MultiAgentRegistry` interface. Implement it as a CDI bean and the reference server registers routes for every agent in the map — no manual Vert.x wiring required.

## How It Works

Each reference transport defines its own `MultiAgentRegistry` in its `registry` sub-package. If a bean implementing it is resolvable, the server switches to multi-agent mode; if not, it behaves exactly as a normal single-agent server.

| Transport | Registry maps to | Agent selected by |
|---|---|---|
| JSON-RPC | `JSONRPCHandler` | URL path segment — `/<agentId>` |
| HTTP+JSON/REST | `RestHandler` | URL path segment — `/<agentId>` |
| gRPC | `GrpcAgent` | `X-A2A-Agent-Id` request metadata |

For the two HTTP transports, each registered agent gets both its RPC endpoint and its own Agent Card endpoint:

```
POST /agent-1
GET  /agent-1/.well-known/agent-card.json
POST /agent-2
GET  /agent-2/.well-known/agent-card.json
```

gRPC has no per-path routing, so the agent is chosen from the `X-A2A-Agent-Id` metadata header instead.

## JSON-RPC

Implement `MultiAgentRegistry` and return one `JSONRPCHandler` per agent. Only the `AgentCard` and the `AgentExecutor` differ between agents — everything else is shared infrastructure you inject once.

```java
@ApplicationScoped
public class MyAgentRegistry implements MultiAgentRegistry {

    @Inject
    TaskStore taskStore;

    @Inject
    QueueManager queueManager;

    @Inject
    PushNotificationConfigStore pushConfigStore;

    @Inject
    MainEventBusProcessor mainEventBusProcessor;

    @Inject
    @Internal
    Executor executor;

    @Inject
    @EventConsumerExecutor
    Executor eventConsumerExecutor;

    private Map<String, JSONRPCHandler> agents;

    @PostConstruct
    void init() {
        agents = Map.of(
                "weather", handlerFor(weatherCard(), new WeatherAgentExecutor()),
                "billing", handlerFor(billingCard(), new BillingAgentExecutor()));
    }

    private JSONRPCHandler handlerFor(AgentCard card, AgentExecutor agentExecutor) {
        RequestHandler requestHandler = new DefaultRequestHandler(
                agentExecutor, taskStore, queueManager, pushConfigStore,
                mainEventBusProcessor, executor, eventConsumerExecutor);
        return new JSONRPCHandler(card, requestHandler, executor);
    }

    @Override
    public Map<String, JSONRPCHandler> getAgents() {
        return agents;
    }
}
```

Each agent's card is served at its own path, so clients discover them independently:

```bash
curl http://localhost:8080/weather/.well-known/agent-card.json
curl http://localhost:8080/billing/.well-known/agent-card.json
```

## HTTP+JSON/REST

Identical in shape, returning `RestHandler` instances. `RestHandler` also takes an `AgentCardCacheMetadata`, which derives the `ETag` and cache headers from the card — build one per agent so each card is cached against its own entity tag.

```java
@ApplicationScoped
public class MyAgentRegistry implements MultiAgentRegistry {

    @Inject
    A2AConfigProvider config;

    // ... same shared injections as above

    private RestHandler handlerFor(AgentCard card, AgentExecutor agentExecutor) {
        RequestHandler requestHandler = new DefaultRequestHandler(
                agentExecutor, taskStore, queueManager, pushConfigStore,
                mainEventBusProcessor, executor, eventConsumerExecutor);
        return new RestHandler(card, new AgentCardCacheMetadata(card, config), requestHandler, executor);
    }

    @Override
    public Map<String, RestHandler> getAgents() {
        return agents;
    }
}
```

## gRPC

gRPC routes by metadata rather than path, so the registry maps to `GrpcAgent` — a record bundling the public card, an optional extended card, and the request handler.

```java
@ApplicationScoped
public class MyAgentRegistry implements MultiAgentRegistry {

    // ... same shared injections as above

    @Override
    public Map<String, GrpcAgent> getAgents() {
        return Map.of(
                "weather", new GrpcAgent(weatherCard(), null, weatherRequestHandler()),
                "billing", new GrpcAgent(billingCard(), null, billingRequestHandler()));
    }
}
```

Clients select the agent by attaching the `X-A2A-Agent-Id` header to the call metadata. Calls that omit the header, or name an agent not in the registry, fall back to the default single-agent beans when those are configured.

## Loading Agents From a Database

Because the registry is an ordinary CDI bean returning a map, agent definitions do not have to be hard-coded. Reading them from a database at startup lets you provision new agents without redeploying:

```java
@ApplicationScoped
public class DatabaseAgentRegistry implements MultiAgentRegistry {

    @Inject
    AgentCardRepository repository;

    private Map<String, JSONRPCHandler> agents;

    @PostConstruct
    void init() {
        agents = repository.findAllEnabled().stream()
                .collect(Collectors.toMap(
                        AgentDefinition::agentId,
                        definition -> handlerFor(definition.card(), executorFor(definition))));
    }

    @Override
    public Map<String, JSONRPCHandler> getAgents() {
        return agents;
    }
}
```

Routes are registered when the router starts, so the map must be fully populated by then. Adding agents after startup requires a restart or your own dynamic route registration.

## Considerations

- **Capabilities are per-agent.** Each registered agent carries its own `AgentCard`, and the handler validates `streaming`, `pushNotifications`, `extendedAgentCard` and the protocol version against that agent's card. Agents sharing an application do not share capabilities.
- **Agent IDs and tenants are distinct.** The agent ID is the first path segment and is stripped before tenant extraction, so `POST /weather/tenant-a` routes to the `weather` agent with tenant `tenant-a`.
- **Shared state.** `AgentExecutor` implementations run concurrently across agents. Keep them thread-safe and avoid leaking state between agent contexts.
- **Shared infrastructure.** Connection pools, HTTP clients and thread pools are shared by every agent in the application. Size them for the aggregate load, not for one agent.
- **Routing.** Ensure your reverse proxy or Ingress preserves the agent path segment so `/<agentId>/.well-known/agent-card.json` reaches the application intact.
