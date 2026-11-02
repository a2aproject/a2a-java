package org.a2aproject.sdk.extras.taskstore.database.jpa;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;

/**
 * Optional owner-only policy. Explicitly select this alternative or produce an instance;
 * adding the module alone does not enable it. Programmatic users pass a repository from
 * {@link JpaTaskOwnershipRepository#create}. Register JpaTaskOwner in the a2a-java unit
 * and migrate its binary subject column (PostgreSQL BYTEA, MySQL VARBINARY(1024)).
 * Usernames must uniquely identify subjects across the deployment's identity domains.
 */
@Dependent
@Alternative
public final class JpaTaskAuthorizationProvider
        implements TaskAuthorizationProvider, JpaTaskAuthorizationCapability {
    private final JpaTaskOwnershipRepository repository;

    @Inject
    public JpaTaskAuthorizationProvider(JpaTaskOwnershipRepository repository) {
        this.repository = repository;
    }

    @Override
    public <T> Optional<T> getCapability(Class<T> type) {
        return type == JpaTaskAuthorizationCapability.class
                ? Optional.of(type.cast(this))
                : Optional.empty();
    }

    @Override
    public JpaTaskReadScope readScope(ServerCallContext context, TaskOperation operation) {
        return context.getUser() != null && context.getUser().isAuthenticated()
                ? new JpaTaskReadScope.OwnedBy(context.getUser().getUsername())
                : new JpaTaskReadScope.None();
    }

    @Override
    public boolean checkRead(ServerCallContext context, String taskId, TaskOperation operation) {
        JpaTaskReadScope scope = readScope(context, operation);
        if (scope instanceof JpaTaskReadScope.None) {
            return false;
        }
        if (taskId.isEmpty() && operation == TaskOperation.LIST_TASKS) {
            return true;
        }
        return scope instanceof JpaTaskReadScope.OwnedBy owner
                && Arrays.equals(
                        JpaSubjectKey.encode(owner.subjectKey()), repository.owner(taskId));
    }

    @Override
    public boolean checkWrite(ServerCallContext context, String taskId, TaskOperation operation) {
        return readScope(context, operation) instanceof JpaTaskReadScope.OwnedBy owner
                && Arrays.equals(
                        JpaSubjectKey.encode(owner.subjectKey()), repository.owner(taskId));
    }

    @Override
    public boolean checkCreate(ServerCallContext context, TaskOperation operation) {
        return readScope(context, operation) instanceof JpaTaskReadScope.OwnedBy;
    }

    @Override
    public boolean isTaskRecorded(String taskId) {
        return repository.owner(taskId) != null;
    }

    @Override
    public void recordOwnership(ServerCallContext context, String taskId, TaskOperation operation) {
        if (readScope(context, operation) instanceof JpaTaskReadScope.OwnedBy owner) {
            repository.record(taskId, JpaSubjectKey.encode(owner.subjectKey()));
        }
    }
}
