---
title: Multi-Tenancy
description: Serve multiple tenants from a single A2A server — per-tenant AgentExecutor and AgentCard routing with CDI qualifiers.
layout: page
---

# Multi-Tenancy

Multi-tenancy lets a single A2A server provide different agent behavior per tenant. Each tenant can have its own `AgentExecutor` (business logic) and `AgentCard` (capabilities, skills, metadata). Requests without a recognized tenant automatically fall back to the default beans.

The `@Tenant` CDI qualifier is in the core SDK (`org.a2aproject.sdk.server.multitenancy`). The `a2a-java-extras-multitenancy` extras module registers two CDI routers — `CdiAgentExecutorRouter` and `CdiAgentCardRouter` — that dispatch each request to the matching `@Tenant`-qualified bean.

For setup instructions, configuration reference, and code examples, see the **[Multi-Tenancy extras page](../extra/multi-tenancy)**.
