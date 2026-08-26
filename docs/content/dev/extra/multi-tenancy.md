---
title: Multi-Tenancy
description: CDI-based multitenancy support for A2A Java SDK servers — per-tenant AgentExecutor and AgentCard routing with the @Tenant qualifier.
layout: page
---

# Multi-Tenancy

Lets a single A2A server serve multiple tenants with different agent behavior — each tenant gets its own `AgentExecutor` and `AgentCard`, while requests without a recognized tenant automatically fall back to the default beans.

## Module

| Artifact ID | Description |
|-------------|-------------|
| `a2a-java-extras-multitenancy` | `@Tenant` qualifier, `CdiAgentExecutorRouter`, `CdiAgentCardRouter` |

### Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-extras-multitenancy</artifactId>
</dependency>
```

> **Tip:** Use the [extras BOM](../boms) to manage the version without specifying it explicitly.

The module activates automatically via CDI when present on the classpath — no additional server configuration is required.

## Architecture

<pre class="mermaid">
flowchart TD
    A["Incoming Request\ntenant field"] --> B["CdiAgentExecutorRouter"]
    B -->|"@Tenant match found"| C["@Tenant AgentExecutor"]
    B -->|"null / blank / unknown"| D["Default AgentExecutor"]
    C --> E["RequestContext.getTenant()"]
    D --> E

    F["getExtendedAgentCard\npublic card URL"] --> G["CdiAgentCardRouter"]
    G -->|"@Tenant match found"| H["@Tenant AgentCard"]
    G -->|"null / blank / unknown"| I["Default AgentCard"]
</pre>

Two CDI routers are registered automatically:

- **`CdiAgentExecutorRouter`** — looks up an `AgentExecutor` bean qualified with `@Tenant(value)` for each request. Falls back to the unqualified default executor when no match is found.
- **`CdiAgentCardRouter`** — resolves the appropriate `AgentCard` for `getExtendedAgentCard` and tenant-specific public card endpoints.

## Declaring Per-Tenant Beans

Use the `@Tenant` qualifier on CDI producer methods:

### AgentExecutor

```java
@ApplicationScoped
public class MyExecutors {

    @Produces
    public AgentExecutor defaultExecutor() {
        return new DefaultAgentExecutor();
    }

    @Produces
    @Tenant("acme")
    public AgentExecutor acmeExecutor() {
        return new AcmeAgentExecutor();
    }

    @Produces
    @Tenant("beta")
    public AgentExecutor betaExecutor() {
        return new BetaAgentExecutor();
    }
}
```

### AgentCard

```java
@ApplicationScoped
public class MyAgentCards {

    // Default public card — used by /.well-known/agent-card.json
    @Produces
    @PublicAgentCard
    public AgentCard publicCard() {
        return AgentCard.builder().name("My Agent")...build();
    }

    // Default extended card — used when no tenant match
    @Produces
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return AgentCard.builder().name("My Agent (extended)")...build();
    }

    // Tenant-specific extended card
    @Produces
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return AgentCard.builder().name("Acme Agent")...build();
    }

    // Tenant-specific public card — no @PublicAgentCard qualifier (see note below)
    @Produces
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return AgentCard.builder().name("Acme Agent")...build();
    }
}
```

> **Note:** Tenant-specific public cards must **not** carry `@PublicAgentCard` — that qualifier is reserved for the single default public card. Adding it to a `@Tenant` bean causes CDI ambiguity.

## Routing Rules

### AgentExecutor routing

| Request tenant | Result |
|----------------|--------|
| `null` or blank | Default (unqualified) executor |
| Known tenant (e.g. `"acme"`) | `@Tenant("acme")` executor |
| Unknown tenant | Default (unqualified) executor |

### AgentCard routing

| Request / URL | Card returned |
|---------------|---------------|
| `getExtendedAgentCard` with no tenant | Default `@ExtendedAgentCard` |
| `getExtendedAgentCard` with `tenant: "acme"` | `@Tenant("acme") @ExtendedAgentCard`, or default |
| `GET /.well-known/agent-card.json` | Default `@PublicAgentCard` |
| `GET /.well-known/acme/agent-card.json` | `@Tenant("acme")` card (no `@PublicAgentCard`), or default |

## Accessing the Tenant in AgentExecutor

The resolved tenant is available in `RequestContext`:

```java
@Override
public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
    String tenant = context.getTenant(); // null for the default tenant
    // ...
}
```

## Tenant Source

The tenant is read from the `tenant` field in the request payload (e.g. `MessageSendParams.tenant()`, `CancelTaskParams.tenant()`). For the REST transport the tenant can also come from the URL path (e.g. `/\{tenant}/extendedAgentCard`); the payload value takes precedence when both are present.

Tenant identifiers are restricted to `a-zA-Z0-9_-.` characters — path segments containing `/` or `?` are rejected.

## Limitations

- **TaskStore and QueueManager are shared** across all tenants — tasks are keyed by UUID, not partitioned per tenant.
- **Per-tenant TaskAuthorizationProvider** is not yet supported — a single provider applies to all tenants.

## Without the Module

When `a2a-java-extras-multitenancy` is **not** on the classpath, the server behaves as a single-tenant deployment: the default `AgentExecutor` handles all requests, the default cards are returned, and the `tenant` field in request payloads is silently ignored. Existing single-tenant code requires no changes when the module is added.

## See Also

- [Multi-Tenancy concept page](../../multi-tenancy) — conceptual overview and setup guide
- [Extras BOM](../boms) — version management for all extras modules
