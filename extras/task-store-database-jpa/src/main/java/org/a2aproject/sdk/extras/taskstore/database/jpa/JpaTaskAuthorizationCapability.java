package org.a2aproject.sdk.extras.taskstore.database.jpa;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskOperation;

/**
 * Authoritative complete read policy for the requested operation. Wrappers must only
 * forward this capability if all of their restrictions are represented by its scope.
 * Lookup and scope failures propagate; unsupported providers use the bounded fallback.
 */
@FunctionalInterface
public interface JpaTaskAuthorizationCapability {
    JpaTaskReadScope readScope(ServerCallContext context, TaskOperation operation);
}
