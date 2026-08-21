---
title: Multi-Tenancy
description: Serve multiple tenants from a single A2A server — per-tenant AgentExecutor and AgentCard routing with CDI qualifiers.
layout: page
---

# Multi-Tenancy

Multi-tenancy lets a single A2A server provide different agent behavior per tenant. Each tenant can have its own `AgentExecutor` (business logic) and extended `AgentCard` (capabilities, skills, metadata). Requests without a tenant — or with an unknown tenant — fall back to the default beans automatically.

## Setup

Add the multitenancy extras module:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-extras-multitenancy</artifactId>
</dependency>
```

> **Tip:** Use the [extras BOM](boms) to manage the version.

No code changes are needed in the server itself — the module activates automatically via CDI when present on the classpath.

## Declaring Per-Tenant Beans

Use the `@Tenant` qualifier to declare tenant-specific producers:

```java
@ApplicationScoped
public class MultiTenantConfig {

    // Default executor — used for unknown tenants and requests with no tenant
    @Produces
    public AgentExecutor defaultExecutor() {
        return new DefaultAgentExecutor();
    }

    // Tenant-specific executor — used when params.tenant == "acme"
    @Produces
    @Tenant("acme")
    public AgentExecutor acmeExecutor() {
        return new AcmeAgentExecutor();
    }

    // Default extended agent card (no @Tenant qualifier)
    @Produces
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return AgentCard.builder()
                .name("Default Agent")
                .description("Default agent for all tenants")
                // ...
                .build();
    }

    // Tenant-specific extended agent card
    @Produces
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return AgentCard.builder()
                .name("Acme Agent")
                .description("Specialized agent for Acme Corp")
                // ...
                .build();
    }
}
```

## Fallback Behavior

When a request arrives with a `tenant` value:

1. The router looks for a bean qualified with `@Tenant("value")`
2. If found, that tenant-specific bean is used
3. If not found, the **unqualified default** bean is used

A `null`, blank, or missing tenant always resolves to the default bean. Unknown tenants also fall back to the default — they do not produce an error.

This means single-tenant deployments (no `@Tenant` beans) continue to work unchanged when the module is on the classpath.

## Tenant Source

The tenant is read from the **request payload** — the `tenant` field in JSON-RPC params (e.g. `MessageSendParams.tenant()`, `CancelTaskParams.tenant()`) and protobuf request messages. For the REST transport, the tenant can also be extracted from the URL path (e.g. `/\\{tenant}/extendedAgentCard`); the payload value takes precedence when both are present.

A tenant must be a **simple identifier** — only `a-zA-Z0-9_-.` characters are allowed. Path elements like `/` and `?` are rejected.

Both the execute and cancel flows propagate the tenant to the `RequestContext`, so `AgentExecutor` implementations can access it via `context.getTenant()`.

## Per-Tenant Agent Cards

### Extended Agent Card

The `getExtendedAgentCard` method is tenant-aware. When the multitenancy module is present:

- A request with `tenant: "acme"` returns the `@Tenant("acme") @ExtendedAgentCard` card
- A request with no tenant or an unknown tenant returns the default `@ExtendedAgentCard` card

This works across all transports (JSON-RPC, gRPC, REST).

### Public Agent Card

The public agent card is also tenant-aware via the URL path `/.well-known/\\{tenant}/agent-card.json`. To provide a tenant-specific public card, declare a producer with only the `@Tenant` qualifier (do **not** add `@PublicAgentCard` — that would cause CDI ambiguity):

```java
// Tenant-specific public agent card
@Produces
@Tenant("acme")
public AgentCard acmePublicCard() {
    return AgentCard.builder()
            .name("Acme Agent")
            .description("Acme-specific public card")
            // ...
            .build();
}
```

- `GET /.well-known/agent-card.json` always returns the default `@PublicAgentCard` card
- `GET /.well-known/acme/agent-card.json` returns the `@Tenant("acme")` card, or falls back to the default if none is configured

## Without the Module

When the `extras/multitenancy` module is **not** on the classpath, no routers are registered. The server behaves exactly as a single-tenant deployment:

- The default `AgentExecutor` handles all requests
- The default `@ExtendedAgentCard` card is returned for `getExtendedAgentCard`
- The `tenant` field in request payloads is silently ignored

## Limitations

- **TaskStore and QueueManager are shared** across tenants — tasks are keyed by UUID, not partitioned by tenant
- **Per-tenant TaskAuthorizationProvider** is not yet supported — the single `TaskAuthorizationProvider` applies to all tenants
- **Public agent card** — Tenant-specific public cards use `@Tenant` without `@PublicAgentCard` due to CDI qualifier matching constraints (see [Per-Tenant Agent Cards](#per-tenant-agent-cards))
