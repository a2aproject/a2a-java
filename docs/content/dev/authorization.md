---
title: Task Authorization
description: Per-user access control for A2A tasks — ownership, read/write checks, and user identity across transports.
layout: page
---

# Task Authorization

> **Security note:** By default, all task operations are **denied** when no `TaskAuthorizationProvider` is configured (fail-closed). This ensures multi-user deployments cannot accidentally expose tasks. For single-user deployments or testing where authorization is not needed, set `a2a.authorization.required=false` in your configuration, or call `.authorizationRequired(false)` on the builder. Production deployments should always configure a provider with a fail-closed ownership policy (deny access when ownership is unknown).

## Implementing TaskAuthorizationProvider

Implement `TaskAuthorizationProvider` to control per-user access:

```java
@ApplicationScoped
public class MyTaskAuthorizationProvider implements TaskAuthorizationProvider {

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context.getUser(), taskId);
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation op) {
        return isOwner(context.getUser(), taskId);
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
            ownershipStore.put(taskId, context.getUser().getUsername());
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
