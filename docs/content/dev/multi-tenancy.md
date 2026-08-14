---
title: Multi-Tenancy
description: Route requests to different tenants using URL paths — an SDK extension to the A2A protocol.
layout: page
---

# Multi-Tenancy

> **SDK extension:** Path-based tenant routing is not part of the A2A specification. The spec only defines `tenant` as an optional field in the JSON-RPC `params` body. The URL path routing described on this page is an addition provided by the Java SDK reference server implementation.

Multi-tenancy lets a single server instance serve multiple logical tenants. The tenant value flows through the request context and is available to your `AgentExecutor` via `RequestContext.getTenant()`.

## How Tenant Resolution Works

### JSON-RPC Transport

The tenant is resolved from two sources, in priority order:

| Source | Example | Resolved tenant |
|--------|---------|-----------------|
| `params.tenant` body field | `"params": { "tenant": "acme", ... }` | `"acme"` |
| URL path (SDK extension, fallback) | `POST /acme` | `"acme"` |

**Resolution rules:**

- If `params.tenant` is set and non-blank → use body tenant (spec-compliant primary source)
- If `params.tenant` is absent or blank and the URL path contains a tenant → use path tenant (SDK extension fallback)
- If both are set and **differ** → the request is rejected with a JSON-RPC `InvalidParamsError` (code `-32602`)
- If both are absent or blank → tenant is empty string (no tenant)

**URL path patterns:**

| Request | Resolved tenant |
|---------|-----------------|
| `POST /` | `""` (no tenant) |
| `POST /acme` | `"acme"` |
| `POST /acme/` | `"acme"` (trailing slash stripped) |
| `POST /org/team` | `"org/team"` |

### REST Transport

For the REST transport, the tenant is always taken from the URL path. There is no separate `tenant` body field for REST. All REST endpoints support an optional tenant prefix:

| Request | Resolved tenant |
|---------|-----------------|
| `POST /message:send` | `""` (no tenant) |
| `POST /acme/message:send` | `"acme"` |
| `GET /acme/tasks` | `"acme"` |
| `GET /acme/tasks/{taskId}` | `"acme"` |
| `POST /acme/tasks/{taskId}:cancel` | `"acme"` |
| `POST /acme/tasks/{taskId}:subscribe` | `"acme"` |

## Accessing the Tenant in Your Executor

The resolved tenant is available on the `RequestContext` passed to your `AgentExecutor`:

```java
@Override
public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
    String tenant = context.getTenant(); // "" if no tenant was provided
    // Use tenant to scope task storage, access control, etc.
}
```

## JSON-RPC Examples

### Body tenant (spec-compliant)

```
POST /
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "sendMessage",
  "params": {
    "tenant": "acme",
    "message": { ... }
  }
}
```

Executor receives `tenant = "acme"`.

### Path tenant (SDK extension fallback)

```
POST /acme
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "sendMessage",
  "params": {
    "message": { ... }
  }
}
```

No `tenant` field in the body → executor receives `tenant = "acme"` from the path.

### Matching tenants (both sources, same value)

```
POST /acme
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "sendMessage",
  "params": {
    "tenant": "acme",
    "message": { ... }
  }
}
```

Both sources agree → executor receives `tenant = "acme"`.

### Mismatched tenants (error)

```
POST /acme
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "req-1",
  "method": "sendMessage",
  "params": {
    "tenant": "other",
    "message": { ... }
  }
}
```

URL tenant `"acme"` ≠ body tenant `"other"` → response:

```json
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "error": {
    "code": -32602,
    "message": "Tenant mismatch: URL path tenant 'acme' does not match params tenant 'other'"
  }
}
```

## REST Examples

```
POST /acme/message:send
Content-Type: application/json

{ ... }
```

Executor receives `tenant = "acme"`.

```
POST /message:send
Content-Type: application/json

{ ... }
```

Executor receives `tenant = ""` (no tenant).
