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
        if (op == TaskOperation.LIST_TASKS && taskId.isEmpty()) {
            return context.getUser() != null && context.getUser().isAuthenticated();
        }
        return isOwner(context, taskId);
    }

    private boolean isOwner(ServerCallContext context, String taskId) {
        String owner = ownershipStore.get(taskId);
        return owner != null
                && context.getUser() != null
                && context.getUser().isAuthenticated()
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
        if (context.getUser() != null && context.getUser().isAuthenticated()) {
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
| `listTasks` | Unconditional `checkRead(context, "", LIST_TASKS)` gate, authorized count/page selection, final returned-task checks |

## Persisted JPA ownership

The optional JPA module provides `JpaTaskAuthorizationProvider`. Adding the module does not select it. Explicitly enable the CDI alternative (in Quarkus, `quarkus.arc.selected-alternatives=org.a2aproject.sdk.extras.taskstore.database.jpa.JpaTaskAuthorizationProvider`), register `JpaTaskOwner` with `JpaTask` in the same `a2a-java` unit, and migrate the ownership table and complete index. See [Storage & Persistence](extra/storage.md) for PostgreSQL `BYTEA` and MySQL `VARBINARY(1024)` setup.

The provider requires authenticated subjects, denies unknown ownership, and records a persistent first owner without transfer. Strict UTF-8 subject identities have a 1,024-byte limit; malformed surrogates and oversized values are rejected. Case, accents, Unicode-normalization forms and trailing spaces remain distinct. There is no normalization, trimming, truncation or lossy hashing. Usernames must be unique across the deployment; multiple issuers or tenant-local names require namespaced keys or a custom policy. Backfill only from a trusted source preserving original identities.

Outside CDI, use `JpaTaskOwnershipRepository.create(entityManagerFactory, transactionManager)`, then `new JpaTaskAuthorizationProvider(repository)`, and pass it to the handler builder. The JTA factory and manager must govern the task database. Insertion and duplicate confirmation each run in a separate new transaction with a fresh persistence context; insertion rolls back before confirmation. Never instantiate the transaction helper directly, as this bypasses CDI interception.

## Optional backend capabilities

Core authorization stays independent of storage technology:

```java
default <T> Optional<T> getCapability(Class<T> capabilityType) {
    return Optional.empty();
}
```

Existing providers inherit empty discovery. Stores define backend-specific capability interfaces and request them from the **same configured provider** used by the handler. Empty means bounded fallback; present means supported policy, including explicit all/none scopes. Exceptions fail the request and never mean unsupported or allow-all.

JPA requests `JpaTaskAuthorizationCapability.readScope(context, operation)`. Its closed scopes are `JpaTaskReadScope.OwnedBy(subject)`, `All`, and `None`. A capability represents the provider's **complete per-task policy for that operation**. Wrappers adding restrictions must not forward a broader delegate capability. Custom allow-all providers may expose `All` only when individual checks agree; it includes unowned tasks.

Both database `COUNT` and paginated `SELECT` apply the same scope and ordinary filters. The cursor never narrows the exact authorized total. Only returned page data is deserialized and transformed. The handler still applies its unconditional list gate and final task checks; these cannot repair an overbroad count or replace exact query equality.

Unsupported providers retain iterative checks. JPA exact counts can fail at the configured candidate ceiling, without returning partial totals. See [Configuration](configuration.md) for JPA fallback settings. Capability queries do not use these Java scan limits.

## Default authorized listing fallback

Stores that do not override `TaskStore.listAuthorized` use a bounded fallback over `list`. Each call scans all matching candidates to count authorized tasks, then separately traverses the requested page and looks ahead for more authorized tasks. This can scan the full candidate set twice, even for a small requested page.

The fallback fetches batches of up to 100 candidates and separately limits count queries (300), normal page/lookahead queries (300), and distinct candidate tasks per phase (20,000, including denied tasks). Up to 100 extra queries may recover an opaque continuation token within the final fetched batch. Exceeding any limit throws `TaskStoreException` rather than returning a partial total; JPA configuration properties do not change these generic limits. Stores with larger candidate sets or more efficient query support should override `listAuthorized`, applying the supplied provider's authorization before pagination and counting authorized matches across all pages independently of the cursor.

The exact `totalSize` assumes stable task data and authorization decisions. The default fallback does not share a snapshot between counting and page selection, so concurrent task additions, deletions, or updates can make the count, returned page, and continuation token disagree. Overrides follow their store's consistency and isolation guarantees; JPA count and page queries use the configured database isolation without an implied stronger snapshot guarantee.

Without a provider, access is denied unless explicitly disabled. `a2a.authorization.required=false` only opts out when no provider is present; configured providers remain authoritative.
