---
title: Task Authorization
description: Per-user access control for A2A tasks — ownership, read/write checks, and user identity across transports.
layout: page
---

# Task Authorization

> **Security note:** By default, all task operations are **denied** when no `TaskAuthorizationProvider` is configured (fail-closed). This ensures multi-user deployments cannot accidentally expose tasks. For single-user deployments or testing where authorization is not needed, set `a2a.authorization.required=false` in your configuration, or call `.authorizationRequired(false)` on the `DefaultRequestHandler` builder. Production deployments should always configure a provider with a fail-closed ownership policy (deny access when ownership is unknown).

## Implementing TaskAuthorizationProvider

Implement `TaskAuthorizationProvider` to control per-user access:

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;

@ApplicationScoped
public class MyTaskAuthorizationProvider implements TaskAuthorizationProvider {
    private final ConcurrentMap<String, String> ownershipStore = new ConcurrentHashMap<>();

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context, taskId);
    }

    private boolean isOwner(ServerCallContext context, String taskId) {
        String owner = ownershipStore.get(taskId);
        return owner != null
                && context.getUser() != null
                && owner.equals(context.getUser().getUsername());
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context, taskId);
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation op) {
        return context.getUser() != null && context.getUser().isAuthenticated();
    }

    @Override
    public boolean isTaskRecorded(String taskId) {
        return ownershipStore.containsKey(taskId);
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation op) {
        if (context.getUser() != null) {
            ownershipStore.putIfAbsent(taskId, context.getUser().getUsername());
        }
    }
}
```

The SDK discovers the bean via CDI automatically — no additional wiring needed. Authorization is also enforced when using `DefaultRequestHandler.builder()` directly — pass the provider via `.authorizationProvider(provider)`.

## User Identity in ServerCallContext

Authorization decisions rely on `context.getUser()` returning the authenticated user. How the user is populated depends on the transport:

- **JSON-RPC and REST**: The Quarkus route handler extracts the user from the Vert.x routing context (`rc.user()`) and sets it on `ServerCallContext` directly.
- **gRPC**: The reference server includes a `QuarkusCallContextFactory` CDI bean that injects the Quarkus `SecurityIdentity` and maps it to the `ServerCallContext` `User`. This happens automatically when using the reference gRPC module. If you provide your own `CallContextFactory`, you are responsible for populating the user.

## Authorization Checks

| Operation | Authorization check |
|-----------|---------------------|
| `getTask`, `subscribeToTask`, `getTaskPushNotificationConfig`, `listTaskPushNotificationConfigs` | `checkRead` |
| `cancelTask`, `createTaskPushNotificationConfig`, `deleteTaskPushNotificationConfig` | `checkWrite` |
| `messageSend` / `messageSendStream` (existing task) | `checkWrite` |
| `messageSend` / `messageSendStream` (new task) | `checkCreate`, then `recordOwnership` |
| `listTasks` | `checkRead` per task |

## Default authorized listing fallback

Stores that do not override `TaskStore.listAuthorized` use a bounded fallback over `list`. Each call scans all matching candidates to count authorized tasks, then separately traverses the requested page and looks ahead for more authorized tasks. This can scan the full candidate set twice, even for a small requested page.

The count scan fetches batches of 100 candidates. Counting and page traversal/lookahead each have separate limits of 300 queries or 20,000 returned candidate tasks, including denied tasks. Exceeding either limit throws `TaskStoreException` rather than returning a partial total; JPA configuration properties do not change these generic limits. Stores with larger candidate sets or more efficient query support should override `listAuthorized`, applying the supplied provider's authorization before pagination and counting authorized matches across all pages independently of the cursor.

The exact `totalSize` assumes stable task data and authorization decisions. The default fallback does not share a snapshot between counting and page selection, so concurrent task additions, deletions, or updates can make the count, returned page, and continuation token disagree. Overrides follow their store's consistency and isolation guarantees.
